package ru.yandex.practicum.filmorate.service;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.impl.UserStorage;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class UserService {
    private final UserStorage userStorage;

    public UserService(@Qualifier("userDbStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public List<User> getAllUsers() {
        log.debug("Запрос на получение всех пользователей");
        return userStorage.getAllUsers();
    }

    public User getUserById(Long id) {
        log.debug("Запрос на получение пользователя с ID: {}", id);
        return userStorage.getUserById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + id + " не найден"));
    }

    public User createUser(@Valid User user) {
        log.info("Создание нового пользователя: {}", user.getLogin());
        validateUser(user);

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.debug("Для пользователя '{}' установлено имя из логина", user.getLogin());
        }

        User createdUser = userStorage.createUser(user);
        log.info("Пользователь создан с ID: {}", createdUser.getId());
        return createdUser;
    }

    public User updateUser(@Valid User user) {
        log.info("Обновление пользователя с ID: {}", user.getId());
        validateUser(user);

        if (user.getId() == null) {
            log.error("Попытка обновить пользователя без ID");
            throw new ValidationException("ID пользователя должен быть указан");
        }

        if (!userStorage.existsById(user.getId())) {
            log.error("Пользователь с ID {} не найден", user.getId());
            throw new NotFoundException("Пользователь с ID " + user.getId() + " не найден");
        }

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.debug("Для пользователя с ID {} установлено имя из логина", user.getId());
        }

        User updatedUser = userStorage.updateUser(user);
        log.info("Пользователь с ID {} успешно обновлен", user.getId());
        return updatedUser;
    }

    public void addFriend(Long userId, Long friendId) {
        log.info("Пользователь {} добавляет в друзья пользователя {}", userId, friendId);

        validateFriendship(userId, friendId);

        userStorage.addFriend(userId, friendId);
        log.info("Пользователь {} успешно добавил в друзья пользователя {}", userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        log.info("Пользователь {} удаляет из друзей пользователя {}", userId, friendId);

        validateFriendship(userId, friendId);

        userStorage.removeFriend(userId, friendId);
        log.info("Пользователь {} успешно удалил из друзей пользователя {}", userId, friendId);
    }

    public List<User> getFriends(Long userId) {
        log.debug("Запрос на получение списка друзей пользователя с ID: {}", userId);

        if (!userStorage.existsById(userId)) {
            log.error("Пользователь с ID {} не найден", userId);
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }

        return userStorage.getFriends(userId);
    }

    public List<User> getCommonFriends(Long userId, Long otherId) {
        log.debug("Запрос на получение общих друзей пользователей {} и {}", userId, otherId);

        validateBothUsersExist(userId, otherId);

        return userStorage.getCommonFriends(userId, otherId);
    }

    private void validateUser(User user) {
        // Валидация email
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Попытка создать пользователя с пустым email");
            throw new ValidationException("Электронная почта не может быть пустой");
        }

        if (!user.getEmail().contains("@")) {
            log.warn("Попытка создать пользователя с некорректным email: {}", user.getEmail());
            throw new ValidationException("Электронная почта должна содержать символ @");
        }

        // Валидация логина
        if (user.getLogin() == null || user.getLogin().isBlank()) {
            log.warn("Попытка создать пользователя с пустым логином");
            throw new ValidationException("Логин не может быть пустым");
        }

        if (user.getLogin().contains(" ")) {
            log.warn("Попытка создать пользователя с логином, содержащим пробелы: {}", user.getLogin());
            throw new ValidationException("Логин не может содержать пробелы");
        }

        // Валидация даты рождения
        if (user.getBirthday() == null) {
            log.warn("Попытка создать пользователя без даты рождения");
            throw new ValidationException("Дата рождения должна быть указана");
        }

        if (user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Попытка создать пользователя с датой рождения в будущем: {}", user.getBirthday());
            throw new ValidationException("Дата рождения не может быть в будущем");
        }

        log.debug("Валидация пользователя '{}' прошла успешно", user.getLogin());
    }

    private void validateFriendship(Long userId, Long friendId) {
        validateBothUsersExist(userId, friendId);

        if (userId.equals(friendId)) {
            log.error("Пользователь {} пытается добавить в друзья самого себя", userId);
            throw new ValidationException("Пользователь не может добавить в друзья самого себя");
        }
    }

    private void validateBothUsersExist(Long userId, Long otherId) {
        if (!userStorage.existsById(userId)) {
            log.error("Пользователь с ID {} не найден", userId);
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }

        if (!userStorage.existsById(otherId)) {
            log.error("Пользователь с ID {} не найден", otherId);
            throw new NotFoundException("Пользователь с ID " + otherId + " не найден");
        }
    }
}