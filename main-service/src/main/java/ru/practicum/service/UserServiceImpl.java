package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.UserMapper;
import ru.practicum.model.user.User;
import ru.practicum.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest newUserRequest) {
        if (userRepository.findByEmail(newUserRequest.getEmail()).isPresent()) {
            throw new ConflictException("Пользователь с таким email уже существует");
        }

        try {
            User user = UserMapper.toEntity(newUserRequest);
            User savedUser = userRepository.save(user);
            return UserMapper.toDto(savedUser);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Пользователь с таким email уже существует");
        }
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        PageRequest pageRequest = PageRequest.of(from / size, size);

        if (ids == null || ids.isEmpty()) {
            return userRepository.findAll(pageRequest).stream()
                    .map(UserMapper::toDto)
                    .collect(Collectors.toList());
        } else {
            return userRepository.findByIdIn(ids, pageRequest).stream()
                    .map(UserMapper::toDto)
                    .collect(Collectors.toList());
        }
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
        userRepository.deleteById(userId);
    }
}