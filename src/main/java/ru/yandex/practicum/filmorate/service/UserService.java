package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {
    private final UserStorage userStorage;

    @Autowired
    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public List<User> getAllUsers() {
        log.debug("Запрос на получение всех пользователей");
        return userStorage.getAllUsers();
    }

    public User createUser(User user) {
        log.info("Создание нового пользователя: {}", user.getLogin());
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.debug("Для пользователя {} установлено имя из логина", user.getLogin());
        }
        User createdUser = userStorage.createUser(user);
        log.info("Пользователь создан с ID: {}", createdUser.getId());
        return createdUser;
    }

    public User updateUser(User user) {
        log.info("Обновление пользователя с ID: {}", user.getId());
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        User updatedUser = userStorage.updateUser(user);
        if (updatedUser == null) {
            throw new NotFoundException("Пользователь с ID " + user.getId() + " не найден");
        }
        log.info("Пользователь с ID {} успешно обновлен", user.getId());
        return updatedUser;
    }

    public User getUserById(Long id) {
        log.debug("Получение пользователя с ID: {}", id);
        return userStorage.getUserById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + id + " не найден"));
    }

    public void addFriend(Long userId, Long friendId) {
        log.info("Пользователь {} добавляет в друзья пользователя {}", userId, friendId);
        User user = getUserById(userId);
        User friend = getUserById(friendId);

        if (userId.equals(friendId)) {
            log.warn("Пользователь {} пытается добавить самого себя в друзья", userId);
            throw new ValidationException("Нельзя добавить самого себя в друзья");
        }

        if (user.getFriends().contains(friendId)) {
            log.warn("Пользователь {} уже в друзьях у пользователя {}", friendId, userId);
            throw new ValidationException("Пользователь уже в друзьях");
        }

        user.getFriends().add(friendId);
        friend.getFriends().add(userId);
        log.info("Пользователи {} и {} теперь друзья", userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        log.info("Пользователь {} удаляет из друзей пользователя {}", userId, friendId);

        User user = getUserById(userId);
        User friend = getUserById(friendId);

        boolean userHadFriend = user.getFriends().contains(friendId);
        boolean friendHadUser = friend.getFriends().contains(userId);

        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);

        if (userHadFriend || friendHadUser) {
            log.info("Пользователи {} и {} больше не друзья", userId, friendId);
        } else {
            log.debug("Пользователи {} и {} не были друзьями", userId, friendId);
        }
    }

    public List<User> getFriends(Long userId) {
        log.debug("Получение списка друзей пользователя {}", userId);
        User user = getUserById(userId);

        return user.getFriends().stream()
                .map(this::getUserById)
                .collect(Collectors.toList());
    }

    public List<User> getCommonFriends(Long userId, Long otherId) {
        log.debug("Поиск общих друзей пользователей {} и {}", userId, otherId);
        User user = getUserById(userId);
        User otherUser = getUserById(otherId);

        Set<Long> commonFriendIds = user.getFriends().stream()
                .filter(otherUser.getFriends()::contains)
                .collect(Collectors.toSet());

        log.debug("Найдено {} общих друзей", commonFriendIds.size());
        return commonFriendIds.stream()
                .map(this::getUserById)
                .collect(Collectors.toList());
    }
}
