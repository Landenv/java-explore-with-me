package ru.practicum.mapper;

import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.model.compilation.Compilation;

import java.util.stream.Collectors;

public class CompilationMapper {

    public static CompilationDto toDto(Compilation compilation) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .events(compilation.getEvents() != null ?
                        compilation.getEvents().stream()
                                .map(EventMapper::toShortDto)
                                .collect(Collectors.toSet()) : null)
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .build();
    }
}