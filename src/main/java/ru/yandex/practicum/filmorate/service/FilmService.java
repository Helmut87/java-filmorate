package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.impl.FilmStorage;
import ru.yandex.practicum.filmorate.impl.UserStorage;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class FilmService {
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);
    private static final Long DEFAULT_MPA_ID = 1L;

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final UserService userService;
    private final MpaService mpaService;
    private final GenreService genreService;

    public FilmService(
            @Qualifier("filmDbStorage") FilmStorage filmStorage,
            @Qualifier("userDbStorage") UserStorage userStorage,
            MpaService mpaService,
            GenreService genreService,
            UserService userService) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.mpaService = mpaService;
        this.genreService = genreService;
        this.userService = userService;
    }

    public List<Film> getAllFilms() {
        log.debug("Запрос на получение всех фильмов");
        return filmStorage.getAllFilms();
    }

    public Film getFilmById(Long id) {
        log.debug("Запрос на получение фильма с ID: {}", id);
        Film film = filmStorage.getFilmById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с ID " + id + " не найден"));
        validateAndEnrichFilm(film);
        return film;
    }

    public Film createFilm(Film film) {
        log.info("Создание нового фильма: {}", film.getName());
        validateFilm(film);
        validateAndSetMpa(film);
        validateAndSetGenres(film);

        Film createdFilm = filmStorage.createFilm(film);
        log.info("Фильм создан с ID: {}", createdFilm.getId());
        return createdFilm;
    }

    public Film updateFilm(Film film) {
        log.info("Обновление фильма с ID: {}", film.getId());
        validateFilm(film);

        if (film.getId() == null) {
            log.error("Попытка обновить фильм без ID");
            throw new ValidationException("ID фильма должен быть указан");
        }

        if (!filmStorage.existsById(film.getId())) {
            log.error("Фильм с ID {} не найден", film.getId());
            throw new NotFoundException("Фильм с ID " + film.getId() + " не найден");
        }

        validateAndSetMpa(film);
        validateAndSetGenres(film);

        Film updatedFilm = filmStorage.updateFilm(film);
        log.info("Фильм с ID {} успешно обновлен", film.getId());
        return updatedFilm;
    }

    public void addLike(Long filmId, Long userId) {
        log.info("Добавление лайка фильму {} от пользователя {}", filmId, userId);

        if (!filmStorage.existsById(filmId)) {
            log.error("Фильм с ID {} не найден", filmId);
            throw new NotFoundException("Фильм с ID " + filmId + " не найден");
        }

        userService.getUserById(userId);

        filmStorage.addLike(filmId, userId);
        log.info("Лайк успешно добавлен");
    }

    public void removeLike(Long filmId, Long userId) {
        log.info("Удаление лайка фильму {} от пользователя {}", filmId, userId);

        if (!filmStorage.existsById(filmId)) {
            log.error("Фильм с ID {} не найден", filmId);
            throw new NotFoundException("Фильм с ID " + filmId + " не найден");
        }

        userService.getUserById(userId);

        filmStorage.removeLike(filmId, userId);
        log.info("Лайк успешно удален");
    }

    public List<Film> getPopularFilms(Integer count) {
        int limit = (count != null && count > 0) ? count : 10;
        log.info("Запрос на получение {} популярных фильмов", limit);

        List<Film> popularFilms = filmStorage.getPopularFilms(limit);

        popularFilms.forEach(this::validateAndEnrichFilm);

        return popularFilms;
    }

    private void validateFilm(Film film) {
        // Проверка названия
        if (film.getName() == null || film.getName().isBlank()) {
            log.warn("Попытка создать фильм с пустым названием");
            throw new ValidationException("Название фильма не может быть пустым");
        }

        // Проверка описания
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            log.warn("Попытка создать фильм с описанием длиннее 200 символов: {}", film.getDescription().length());
            throw new ValidationException("Максимальная длина описания — 200 символов");
        }

        // Проверка даты релиза
        if (film.getReleaseDate() == null) {
            log.warn("Попытка создать фильм без даты релиза");
            throw new ValidationException("Дата релиза должна быть указана");
        }

        if (film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            log.warn("Попытка создать фильм с датой релиза {} (минимальная допустимая: {})",
                    film.getReleaseDate(), MIN_RELEASE_DATE);
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }

        // Проверка продолжительности
        if (film.getDuration() <= 0) {
            log.warn("Попытка создать фильм с некорректной продолжительностью: {}", film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }

        log.debug("Валидация фильма '{}' прошла успешно", film.getName());
    }

    private void validateAndSetMpa(Film film) {
        if (film.getMpa() == null || film.getMpa().getId() == null) {
            // Устанавливаем рейтинг по умолчанию
            Mpa defaultMpa = mpaService.getMpaById(DEFAULT_MPA_ID);
            film.setMpa(defaultMpa);
            log.debug("Для фильма '{}' установлен рейтинг MPA по умолчанию: {}",
                    film.getName(), defaultMpa.getName());
        } else {
            // Проверяем существование MPA
            try {
                Mpa mpa = mpaService.getMpaById(film.getMpa().getId());
                // Обновляем объект Mpa полными данными (с именем)
                film.setMpa(mpa);
                log.debug("Для фильма '{}' установлен рейтинг MPA: {}",
                        film.getName(), mpa.getName());
            } catch (NotFoundException e) {
                log.error("Рейтинг MPA с ID {} не найден", film.getMpa().getId());
                throw new NotFoundException("Рейтинг MPA с ID " + film.getMpa().getId() + " не найден");
            }
        }
    }

    private void validateAndSetGenres(Film film) {
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            // Создаем новый сет для валидированных жанров
            Set<Genre> validatedGenres = new LinkedHashSet<>();

            for (Genre genre : film.getGenres()) {
                if (genre.getId() == null) {
                    log.warn("Попытка добавить жанр без ID");
                    throw new ValidationException("ID жанра должен быть указан");
                }

                try {
                    // Получаем полную информацию о жанре
                    Genre fullGenre = genreService.getGenreById(genre.getId());
                    validatedGenres.add(fullGenre);
                } catch (NotFoundException e) {
                    log.error("Жанр с ID {} не найден", genre.getId());
                    throw new NotFoundException("Жанр с ID " + genre.getId() + " не найден");
                }
            }

            film.setGenres(validatedGenres);
            log.debug("Для фильма '{}' установлены жанры: {}",
                    film.getName(), validatedGenres.stream()
                            .map(Genre::getName)
                            .toList());
        } else {

            film.setGenres(new LinkedHashSet<>());
        }
    }

    private void validateAndEnrichFilm(Film film) {
        // Дополняем информацию о MPA если она неполная
        if (film.getMpa() != null && film.getMpa().getId() != null &&
                (film.getMpa().getName() == null || film.getMpa().getName().isBlank())) {
            try {
                Mpa fullMpa = mpaService.getMpaById(film.getMpa().getId());
                film.setMpa(fullMpa);
            } catch (NotFoundException e) {
                log.warn("MPA с ID {} не найден для фильма {}", film.getMpa().getId(), film.getId());
            }
        }

        // Дополняем информацию о жанрах если они неполные
        if (film.getGenres() != null) {
            Set<Genre> enrichedGenres = new LinkedHashSet<>();
            for (Genre genre : film.getGenres()) {
                if (genre.getId() != null &&
                        (genre.getName() == null || genre.getName().isBlank())) {
                    try {
                        Genre fullGenre = genreService.getGenreById(genre.getId());
                        enrichedGenres.add(fullGenre);
                    } catch (NotFoundException e) {
                        log.warn("Жанр с ID {} не найден для фильма {}", genre.getId(), film.getId());
                        enrichedGenres.add(genre);
                    }
                } else {
                    enrichedGenres.add(genre);
                }
            }
            film.setGenres(enrichedGenres);
        }
    }
}