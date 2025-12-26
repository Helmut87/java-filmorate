package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.impl.UserStorageImpl;
import ru.yandex.practicum.filmorate.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class UserStorage implements UserStorageImpl {
    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    @Override
    public List<User> getAllUsers() {
        log.debug("Получение всех пользователей, количество: {}", users.size());
        return new ArrayList<>(users.values());
    }

    @Override
    public User createUser(User user) {
        long id = nextId.getAndIncrement();
        user.setId(id);
        users.put(id, user);
        log.info("Создан пользователь с ID: {}, логин: {}", id, user.getLogin());
        return user;
    }

    @Override
    public User updateUser(User user) {
        Long id = user.getId();
        if (users.containsKey(id)) {
            users.put(id, user);
            log.info("Обновлен пользователь с ID: {}, логин: {}", id, user.getLogin());
            return user;
        }
        log.warn("Попытка обновить несуществующего пользователя с ID: {}", id);
        return null;
    }

    @Override
    public Optional<User> getUserById(Long id) {
        User user = users.get(id);
        if (user == null) {
            log.debug("Пользователь с ID {} не найден", id);
        }
        return Optional.ofNullable(user);
    }

    @Override
    public void deleteUser(Long id) {
        if (users.remove(id) != null) {
            log.info("Удален пользователь с ID: {}", id);
        } else {
            log.warn("Попытка удалить несуществующего пользователя с ID: {}", id);
        }
    }

    @Override
    public boolean existsById(Long id) {
        return users.containsKey(id);
    }
}
