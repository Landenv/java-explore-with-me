package ru.practicum.service;

import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;

import java.util.List;

public interface UserService {
    UserDto createUser(NewUserRequest newUserRequest);
    List<UserDto> getUsers(List<Long> ids, int from, int size);
    void deleteUser(Long userId);
}