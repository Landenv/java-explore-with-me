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
import ru.practicum.mapper.EventUpdateMapper;
import ru.practicum.model.category.Category;
import ru.practicum.model.event.Event;
import ru.practicum.model.event.EventSort;
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
        User user = findUserById(userId);
        Category category = findCategoryById(newEventDto.getCategory());
        validateNewEvent(newEventDto);

        Event event = buildNewEvent(user, category, newEventDto);
        Event savedEvent = eventRepository.save(event);

        return EventMapper.toFullDto(savedEvent);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        findUserById(userId);
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
        List<Event> events = eventRepository.findByInitiatorId(userId, pageable);

        return events.stream()
                .map(EventMapper::toShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        Event event = findUserEventById(userId, eventId);
        EventFullDto dto = EventMapper.toFullDto(event);
        dto.setViews(statsService.getViewsForEvent(eventId));
        return dto;
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        Event event = findUserEventById(userId, eventId);

        validateUserEventUpdate(event, updateRequest);

        if (!hasChanges(event, updateRequest)) {
            return mapToFullDtoWithViews(event);
        }

        applyUserEventUpdate(event, updateRequest);
        applyUserStateAction(event, updateRequest.getStateAction());

        Event updatedEvent = eventRepository.save(event);
        return mapToFullDtoWithViews(updatedEvent);
    }

    @Override
    public List<EventFullDto> getAdminEvents(List<Long> users, List<String> states, List<Long> categories,
                                             LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
        List<EventState> eventStates = buildAdminEventStates(states);

        List<Event> events = eventRepository.findAdminEvents(users, eventStates, categories,
                rangeStart, rangeEnd, pageable);

        return mapToFullDtoWithViews(events);
    }

    @Override
    @Transactional
    public EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest updateRequest) {
        Event event = findEventById(eventId);
        applyAdminStateAction(event, updateRequest.getStateAction());
        applyAdminEventUpdate(event, updateRequest);

        Event updatedEvent = eventRepository.save(event);
        return mapToFullDtoWithViews(updatedEvent);
    }

    @Override
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, String sort, int from, int size,
                                               String clientIp, String requestUri) {
        logHit(requestUri, clientIp);

        LocalDateTime actualRangeStart = getActualRangeStart(rangeStart, rangeEnd);
        Pageable pageable = getPageableWithSort(sort, from, size);
        String searchText = prepareSearchText(text);

        List<Event> events = eventRepository.findPublicEvents(searchText, categories, paid, actualRangeStart,
                rangeEnd, onlyAvailable, pageable);

        return mapToShortDtoWithViews(events, sort);
    }

    @Override
    @Transactional
    public EventFullDto getPublicEvent(Long eventId, String clientIp, String requestUri) {
        logHit(requestUri, clientIp);
        Event event = findPublishedEventById(eventId);
        return mapToFullDtoWithViews(event);
    }

    // Приватные вспомогательные методы

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    private Category findCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория не найдена"));
    }

    private Event findUserEventById(Long userId, Long eventId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));
    }

    private Event findEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));
    }

    private Event findPublishedEventById(Long eventId) {
        return eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));
    }

    private void validateNewEvent(NewEventDto newEventDto) {
        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Дата события должна быть не менее чем через 2 часа от текущего момента");
        }
    }

    private Event buildNewEvent(User user, Category category, NewEventDto newEventDto) {
        Event event = EventMapper.toEntity(newEventDto);
        event.setInitiator(user);
        event.setCategory(category);
        event.setState(EventState.PENDING);
        event.setConfirmedRequests(0);
        return event;
    }

    private void validateUserEventUpdate(Event event, UpdateEventUserRequest updateRequest) {
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Только ожидающие или отмененные события могут быть изменены");
        }

        if (updateRequest.getEventDate() != null &&
                updateRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Дата события должна быть не менее чем через 2 часа от текущего момента");
        }
    }

    private void applyUserEventUpdate(Event event, UpdateEventUserRequest updateRequest) {
        EventUpdateMapper.updateEventFromUserRequest(event, updateRequest, categoryRepository);
    }

    private void applyUserStateAction(Event event, String stateAction) {
        if (stateAction != null) {
            EventStateAction action = EventStateAction.valueOf(stateAction);
            if (action == EventStateAction.SEND_TO_REVIEW) {
                event.setState(EventState.PENDING);
            } else if (action == EventStateAction.CANCEL_REVIEW) {
                event.setState(EventState.CANCELED);
            }
        }
    }

    private void applyAdminEventUpdate(Event event, UpdateEventAdminRequest updateRequest) {
        EventUpdateMapper.updateEventFromAdminRequest(event, updateRequest, categoryRepository);
    }

    private void applyAdminStateAction(Event event, String stateAction) {
        if (stateAction != null) {
            EventStateAction action = EventStateAction.valueOf(stateAction);
            if (action == EventStateAction.PUBLISH_EVENT) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Только ожидающие события могут быть опубликованы");
                }
                if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                    throw new ConflictException("Дата события должна быть не менее чем через 1 час от момента публикации");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (action == EventStateAction.REJECT_EVENT) {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Опубликованные события не могут быть отклонены");
                }
                event.setState(EventState.CANCELED);
            }
        }
    }

    private List<EventState> buildAdminEventStates(List<String> states) {
        if (states == null) {
            return null;
        }
        return states.stream()
                .map(EventState::valueOf)
                .collect(Collectors.toList());
    }

    private EventFullDto mapToFullDtoWithViews(Event event) {
        EventFullDto dto = EventMapper.toFullDto(event);
        dto.setViews(statsService.getViewsForEvent(event.getId()));
        return dto;
    }

    private List<EventFullDto> mapToFullDtoWithViews(List<Event> events) {
        return events.stream()
                .map(this::mapToFullDtoWithViews)
                .collect(Collectors.toList());
    }

    private List<EventShortDto> mapToShortDtoWithViews(List<Event> events, String sort) {
        List<EventShortDto> result = events.stream()
                .map(event -> {
                    EventShortDto dto = EventMapper.toShortDto(event);
                    dto.setViews(statsService.getViewsForEvent(event.getId()));
                    return dto;
                })
                .collect(Collectors.toList());

        if (sort != null && sort.equalsIgnoreCase(EventSort.VIEWS.name())) {
            result.sort((e1, e2) -> Long.compare(e2.getViews(), e1.getViews()));
        }

        return result;
    }

    private void logHit(String uri, String ip) {
        statsService.saveHit(uri, ip);
    }

    private LocalDateTime getActualRangeStart(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        return (rangeStart == null && rangeEnd == null) ? LocalDateTime.now() : rangeStart;
    }

    private String prepareSearchText(String text) {
        return text != null ? "%" + text.toLowerCase() + "%" : null;
    }

    private Pageable getPageableWithSort(String sort, int from, int size) {
        if (sort == null) {
            return PageRequest.of(from / size, size, Sort.by("id").ascending());
        }

        try {
            EventSort eventSort = EventSort.valueOf(sort.toUpperCase());

            return switch (eventSort) {
                case EVENT_DATE -> PageRequest.of(from / size, size, Sort.by("eventDate").ascending());
                case VIEWS, ID -> PageRequest.of(from / size, size, Sort.by("id").ascending());
            };
        } catch (IllegalArgumentException e) {
            return PageRequest.of(from / size, size, Sort.by("id").ascending());
        }
    }

    private boolean hasChanges(Event event, UpdateEventUserRequest request) {
        return (request.getAnnotation() != null && !request.getAnnotation().equals(event.getAnnotation())) ||
                (request.getDescription() != null && !request.getDescription().equals(event.getDescription())) ||
                (request.getEventDate() != null && !request.getEventDate().equals(event.getEventDate())) ||
                (request.getPaid() != null && !request.getPaid().equals(event.getPaid())) ||
                (request.getParticipantLimit() != null && !request.getParticipantLimit().equals(event.getParticipantLimit())) ||
                (request.getTitle() != null && !request.getTitle().equals(event.getTitle())) ||
                (request.getCategory() != null && !request.getCategory().equals(event.getCategory().getId())) ||
                (request.getLocation() != null && !request.getLocation().equals(
                        new LocationDto(event.getLocation().getLat(), event.getLocation().getLon()))) ||
                (request.getRequestModeration() != null && !request.getRequestModeration().equals(event.getRequestModeration())) ||
                request.getStateAction() != null;
    }
}