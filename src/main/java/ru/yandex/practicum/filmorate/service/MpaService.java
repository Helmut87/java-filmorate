package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.dao.MpaDao;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MpaService {
    private final MpaDao mpaDao;

    public List<Mpa> getAllMpa() {
        log.debug("Получение списка всех рейтингов MPA");
        return mpaDao.getAllMpa();
    }

    public Mpa getMpaById(Long id) {
        log.debug("Получение рейтинга MPA с ID: {}", id);
        return mpaDao.getMpaById(id)
                .orElseThrow(() -> {
                    log.error("Рейтинг MPA с ID {} не найден", id);
                    return new NotFoundException("Рейтинг MPA с ID " + id + " не найден");
                });
    }
}