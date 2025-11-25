package ru.practicum.mapper;

import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.model.user.User;

public class UserMapper {

    public static User toEntity(NewUserRequest dto) {
        return User.builder()
                .name(dto.getName().trim())
                .email(dto.getEmail().trim())
                .build();
    }

    public static UserDto toDto(User user) {
        if (user == null) {
            return null;
        }
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public static UserShortDto toShortDto(User user) {
        if (user == null) {
            return null;
        }
        return UserShortDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}