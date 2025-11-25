package ru.practicum.mapper.compilation;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.model.Compilation;
import ru.practicum.model.Event;
import ru.practicum.repository.EventRepository;

import java.util.List;

@Mapper(componentModel = "spring", uses = ru.practicum.mapper.event.EventMapper.class)
public interface CompilationMapper {

    default Compilation toCompilation(NewCompilationDto newCompilationDto, EventRepository eventRepository) {
        List<Event> events = newCompilationDto.getEvents() != null ?
                eventRepository.findByIdIn(newCompilationDto.getEvents()) : List.of();

        return Compilation.builder()
                .events(events)
                .pinned(newCompilationDto.getPinned())
                .title(newCompilationDto.getTitle())
                .build();
    }

    @Mapping(target = "events", source = "compilation.events")
    CompilationDto toCompilationDto(Compilation compilation);
}