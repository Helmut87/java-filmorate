package ru.yandex.practicum.filmorate.impl;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Optional;

public interface FilmStorageImpl {
    List<Film> getAllFilms();
    Film createFilm(Film film);
    Film updateFilm(Film film);
    Optional<Film> getFilmById(Long id);
    void deleteFilm(Long id);
    boolean existsById(Long id);
}
