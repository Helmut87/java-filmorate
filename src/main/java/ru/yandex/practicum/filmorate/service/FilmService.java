package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FilmService {
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);
    private static final int DEFAULT_POPULAR_COUNT = 10;

    private final FilmStorage filmStorage;
    private final UserService userService;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserService userService) {
        this.filmStorage = filmStorage;
        this.userService = userService;
    }

    public List<Film> getAllFilms() {
        log.debug("Запрос на получение всех фильмов");
        return filmStorage.getAllFilms();
    }

    public Film createFilm(Film film) {
        log.info("Создание нового фильма: {}", film.getName());
        validateFilm(film);
        Film createdFilm = filmStorage.createFilm(film);
        log.info("Фильм создан с ID: {}", createdFilm.getId());
        return createdFilm;
    }

    public Film updateFilm(Film film) {
        log.info("Обновление фильма с ID: {}", film.getId());
        validateFilm(film);
        Film updatedFilm = filmStorage.updateFilm(film);
        if (updatedFilm == null) {
            throw new NotFoundException("Фильм с ID " + film.getId() + " не найден");
        }
        log.info("Фильм с ID {} успешно обновлен", film.getId());
        return updatedFilm;
    }

    public Film getFilmById(Long id) {
        log.debug("Получение фильма с ID: {}", id);
        return filmStorage.getFilmById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с ID " + id + " не найден"));
    }

    public void addLike(Long filmId, Long userId) {
        log.info("Пользователь {} ставит лайк фильму {}", userId, filmId);
        Film film = getFilmById(filmId);
        userService.getUserById(userId);

        if (film.getLikes().contains(userId)) {
            log.warn("Пользователь {} уже ставил лайк фильму {}", userId, filmId);
            throw new ValidationException("Пользователь уже ставил лайк этому фильму");
        }

        film.getLikes().add(userId);
        log.info("Фильм {} получил лайк от пользователя {}. Всего лайков: {}",
                filmId, userId, film.getLikes().size());
    }

    public void removeLike(Long filmId, Long userId) {
        log.info("Пользователь {} удаляет лайк у фильма {}", userId, filmId);
        Film film = getFilmById(filmId);
        userService.getUserById(userId);

        if (!film.getLikes().contains(userId)) {
            log.warn("Пользователь {} не ставил лайк фильму {}", userId, filmId);
            throw new NotFoundException("Лайк не найден");
        }

        film.getLikes().remove(userId);
        log.info("У фильма {} удален лайк от пользователя {}. Всего лайков: {}",
                filmId, userId, film.getLikes().size());
    }

    public List<Film> getPopularFilms(Integer count) {
        int limit = (count == null || count <= 0) ? DEFAULT_POPULAR_COUNT : count;
        log.debug("Получение {} самых популярных фильмов", limit);

        return filmStorage.getAllFilms().stream()
                .sorted(Comparator.comparingInt((Film f) -> f.getLikes().size()).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private void validateFilm(Film film) {
        if (film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            log.warn("Попытка создать фильм с датой релиза {} (минимальная допустимая: {})",
                    film.getReleaseDate(), MIN_RELEASE_DATE);
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
        log.debug("Валидация фильма {} прошла успешно", film.getName());
    }
}
