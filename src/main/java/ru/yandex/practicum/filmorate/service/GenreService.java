package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.dao.GenreDao;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class GenreService {
    private final GenreDao genreDao;

    public List<Genre> getAllGenres() {
        log.debug("Получение списка всех жанров");
        return genreDao.getAllGenres();
    }

    public Genre getGenreById(Long id) {
        log.debug("Получение жанра с ID: {}", id);
        return genreDao.getGenreById(id)
                .orElseThrow(() -> {
                    log.error("Жанр с ID {} не найден", id);
                    return new NotFoundException("Жанр с ID " + id + " не найден");
                });
    }
}