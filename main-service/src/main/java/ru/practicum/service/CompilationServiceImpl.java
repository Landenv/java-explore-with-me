package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.model.compilation.Compilation;
import ru.practicum.model.event.Event;
import ru.practicum.repository.CompilationRepository;
import ru.practicum.repository.EventRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        Set<Event> events = new HashSet<>();
        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            events = new HashSet<>(eventRepository.findByIdIn(
                    newCompilationDto.getEvents().stream().collect(Collectors.toList())));
        }

        Compilation compilation = Compilation.builder()
                .events(events)
                .pinned(newCompilationDto.getPinned())
                .title(newCompilationDto.getTitle())
                .build();

        Compilation savedCompilation = compilationRepository.save(compilation);
        return CompilationMapper.toDto(savedCompilation);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена"));
        compilationRepository.delete(compilation);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateRequest) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена"));

        if (updateRequest.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findByIdIn(
                    updateRequest.getEvents().stream().collect(Collectors.toList())));
            compilation.setEvents(events);
        }

        if (updateRequest.getPinned() != null) {
            compilation.setPinned(updateRequest.getPinned());
        }

        if (updateRequest.getTitle() != null) {
            compilation.setTitle(updateRequest.getTitle());
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);
        return CompilationMapper.toDto(updatedCompilation);
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        PageRequest pageRequest = PageRequest.of(from / size, size);

        if (pinned != null) {
            return compilationRepository.findByPinned(pinned, pageRequest).stream()
                    .map(CompilationMapper::toDto)
                    .collect(Collectors.toList());
        } else {
            return compilationRepository.findAll(pageRequest).stream()
                    .map(CompilationMapper::toDto)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public CompilationDto getCompilation(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена"));
        return CompilationMapper.toDto(compilation);
    }
}