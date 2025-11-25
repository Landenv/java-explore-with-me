package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.event.*;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.category.Category;
import ru.practicum.model.event.Event;
import ru.practicum.model.event.EventState;
import ru.practicum.model.event.EventStateAction;
import ru.practicum.model.user.User;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final StatsService statsService;

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория не найдена"));

        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Дата события должна быть не менее чем через 2 часа от текущего момента");
        }

        Event event = EventMapper.toEntity(newEventDto);
        event.setInitiator(user);
        event.setCategory(category);
        event.setState(EventState.PENDING);
        event.setConfirmedRequests(0);

        Event savedEvent = eventRepository.save(event);
        return EventMapper.toFullDto(savedEvent);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
        List<Event> events = eventRepository.findByInitiatorId(userId, pageable);
        return events.stream()
                .map(EventMapper::toShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        EventFullDto dto = EventMapper.toFullDto(event);
        dto.setViews(statsService.getViewsForEvent(eventId));
        return dto;
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Только ожидающие или отмененные события могут быть изменены");
        }

        if (!hasChanges(event, updateRequest)) {
            EventFullDto dto = EventMapper.toFullDto(event);
            dto.setViews(statsService.getViewsForEvent(eventId));
            return dto;
        }

        if (updateRequest.getEventDate() != null &&
                updateRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Дата события должна быть не менее чем через 2 часа от текущего момента");
        }

        updateEventFields(event, updateRequest);

        if (updateRequest.getStateAction() != null) {
            EventStateAction stateAction = EventStateAction.valueOf(updateRequest.getStateAction());
            if (stateAction == EventStateAction.SEND_TO_REVIEW) {
                event.setState(EventState.PENDING);
            } else if (stateAction == EventStateAction.CANCEL_REVIEW) {
                event.setState(EventState.CANCELED);
            }
        }

        Event updatedEvent = eventRepository.save(event);
        EventFullDto dto = EventMapper.toFullDto(updatedEvent);
        dto.setViews(statsService.getViewsForEvent(eventId));
        return dto;
    }

    @Override
    public List<EventFullDto> getAdminEvents(List<Long> users, List<String> states, List<Long> categories,
                                             LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());

        List<EventState> eventStates = null;
        if (states != null) {
            eventStates = states.stream()
                    .map(EventState::valueOf)
                    .collect(Collectors.toList());
        }

        List<Event> events = eventRepository.findAdminEvents(users, eventStates, categories,
                rangeStart, rangeEnd, pageable);
        return events.stream()
                .map(event -> {
                    EventFullDto dto = EventMapper.toFullDto(event);
                    dto.setViews(statsService.getViewsForEvent(event.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest updateRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (updateRequest.getStateAction() != null) {
            EventStateAction stateAction = EventStateAction.valueOf(updateRequest.getStateAction());
            if (stateAction == EventStateAction.PUBLISH_EVENT) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Только ожидающие события могут быть опубликованы");
                }
                if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                    throw new ConflictException("Дата события должна быть не менее чем через 1 час от момента публикации");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (stateAction == EventStateAction.REJECT_EVENT) {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Опубликованные события не могут быть отклонены");
                }
                event.setState(EventState.CANCELED);
            }
        }

        updateEventFields(event, updateRequest);
        Event updatedEvent = eventRepository.save(event);
        EventFullDto dto = EventMapper.toFullDto(updatedEvent);
        dto.setViews(statsService.getViewsForEvent(eventId));
        return dto;
    }

    @Override
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, String sort, int from, int size,
                                               String clientIp, String requestUri) {
        statsService.saveHit(requestUri, clientIp);

        if (rangeStart == null && rangeEnd == null) {
            rangeStart = LocalDateTime.now();
        }

        Pageable pageable;
        if ("EVENT_DATE".equals(sort)) {
            pageable = PageRequest.of(from / size, size, Sort.by("eventDate").ascending());
        } else {
            pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
        }

        String safeText = null;
        if (text != null) {
            safeText = "%" + text.toLowerCase() + "%";
        }

        List<Event> events = eventRepository.findPublicEvents(safeText, categories, paid, rangeStart,
                rangeEnd, onlyAvailable, pageable);

        return events.stream()
                .map(event -> {
                    EventShortDto dto = EventMapper.toShortDto(event);
                    Long views = statsService.getViewsForEvent(event.getId());
                    dto.setViews(views);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto getPublicEvent(Long eventId, String clientIp, String requestUri) {
        statsService.saveHit(requestUri, clientIp);

        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие не опубликовано");
        }

        Long views = statsService.getViewsForEvent(eventId);

        EventFullDto dto = EventMapper.toFullDto(event);
        dto.setViews(views);

        return dto;
    }

    private boolean hasChanges(Event event, UpdateEventUserRequest request) {
        return (request.getAnnotation() != null && !request.getAnnotation().equals(event.getAnnotation())) ||
                (request.getDescription() != null && !request.getDescription().equals(event.getDescription())) ||
                (request.getEventDate() != null && !request.getEventDate().equals(event.getEventDate())) ||
                (request.getPaid() != null && !request.getPaid().equals(event.getPaid())) ||
                (request.getParticipantLimit() != null && !request.getParticipantLimit().equals(event.getParticipantLimit())) ||
                (request.getTitle() != null && !request.getTitle().equals(event.getTitle())) ||
                request.getStateAction() != null;
    }

    private void updateEventFields(Event event, Object updateRequest) {
        if (updateRequest instanceof UpdateEventUserRequest userRequest) {
            updateFromUserRequest(event, userRequest);
        } else if (updateRequest instanceof UpdateEventAdminRequest adminRequest) {
            updateFromAdminRequest(event, adminRequest);
        }
    }

    private void updateFromUserRequest(Event event, UpdateEventUserRequest request) {
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventDate() != null) event.setEventDate(request.getEventDate());
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getTitle() != null) event.setTitle(request.getTitle());
    }

    private void updateFromAdminRequest(Event event, UpdateEventAdminRequest request) {
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventDate() != null) event.setEventDate(request.getEventDate());
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getTitle() != null) event.setTitle(request.getTitle());
    }
}