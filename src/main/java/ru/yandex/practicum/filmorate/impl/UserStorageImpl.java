package ru.yandex.practicum.filmorate.impl;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;
import java.util.Optional;

public interface UserStorageImpl {
    List<User> getAllUsers();
    User createUser(User user);
    User updateUser(User user);
    Optional<User> getUserById(Long id);
    void deleteUser(Long id);
    boolean existsById(Long id);
}
