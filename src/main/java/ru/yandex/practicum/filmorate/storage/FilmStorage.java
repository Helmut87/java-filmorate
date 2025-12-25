package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.impl.FilmStorageImpl;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class FilmStorage implements FilmStorageImpl {
    private final Map<Long, Film> films = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    @Override
    public List<Film> getAllFilms() {
        log.debug("Получение всех фильмов, количество: {}", films.size());
        return new ArrayList<>(films.values());
    }

    @Override
    public Film createFilm(Film film) {
        long id = nextId.getAndIncrement();
        film.setId(id);
        films.put(id, film);
        log.info("Создан фильм с ID: {}, название: {}", id, film.getName());
        return film;
    }

    @Override
    public Film updateFilm(Film film) {
        Long id = film.getId();
        if (films.containsKey(id)) {
            films.put(id, film);
            log.info("Обновлен фильм с ID: {}, название: {}", id, film.getName());
            return film;
        }
        log.warn("Попытка обновить несуществующий фильм с ID: {}", id);
        return null;
    }

    @Override
    public Optional<Film> getFilmById(Long id) {
        Film film = films.get(id);
        if (film == null) {
            log.debug("Фильм с ID {} не найден", id);
        }
        return Optional.ofNullable(film);
    }

    @Override
    public void deleteFilm(Long id) {
        if (films.remove(id) != null) {
            log.info("Удален фильм с ID: {}", id);
        } else {
            log.warn("Попытка удалить несуществующий фильм с ID: {}", id);
        }
    }

    @Override
    public boolean existsById(Long id) {
        return films.containsKey(id);
    }
}
