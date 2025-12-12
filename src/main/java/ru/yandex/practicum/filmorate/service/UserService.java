package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;

@Slf4j
@Service
public class UserService {
    private final UserStorage userStorage;

    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public List<User> getAllUsers() {
        return userStorage.getAllUsers();
    }

    public User createUser(User user) {
        validateUser(user);
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.info("Для пользователя с логином {} установлено имя из логина", user.getLogin());
        }
        return userStorage.createUser(user);
    }

    public User updateUser(User user) {
        validateUser(user);
        if (user.getId() == null) {
            log.error("Попытка обновить пользователя без ID");
            throw new ValidationException("ID пользователя должен быть указан");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        User updatedUser = userStorage.updateUser(user);
        if (updatedUser == null) {
            log.error("Пользователь с ID {} не найден", user.getId());
            throw new NotFoundException("Пользователь с ID " + user.getId() + " не найден");
        }
        return updatedUser;
    }

    private void validateUser(User user) {
    }
}
