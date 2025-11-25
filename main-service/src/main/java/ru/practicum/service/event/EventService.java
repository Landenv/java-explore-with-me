package ru.practicum.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.event.*;
import ru.practicum.exception.ForbiddenException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.event.EventMapper;
import ru.practicum.model.Event;
import ru.practicum.model.EventState;
import ru.practicum.model.User;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.repository.UserRepository;
import ru.practicum.service.StatsService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRequestRepository participationRequestRepository;
    private final StatsService statsService;
    private final EventMapper eventMapper;

    private static final int MIN_HOURS_BEFORE_EVENT = 2;

    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT))) {
            throw new ForbiddenException("Event date must be at least " + MIN_HOURS_BEFORE_EVENT + " hours from now");
        }

        Event event = eventMapper.toEvent(newEventDto, user, categoryRepository);
        Event savedEvent = eventRepository.save(event);

        Long confirmedRequests = participationRequestRepository.countConfirmedRequestsByEventId(savedEvent.getId());
        Long views = statsService.getEventViews(savedEvent.getId());

        return eventMapper.toEventFullDto(savedEvent, confirmedRequests, views);
    }

    public List<EventShortDto> getUserEvents(Long userId, Pageable pageable) {
        List<Event> events = eventRepository.findByInitiatorId(userId, pageable);
        return events.stream()
                .map(event -> {
                    Long confirmedRequests = participationRequestRepository.countConfirmedRequestsByEventId(event.getId());
                    Long views = statsService.getEventViews(event.getId());
                    return eventMapper.toEventShortDto(event, confirmedRequests, views);
                })
                .collect(Collectors.toList());
    }

    public EventFullDto getUserEvent(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        Long confirmedRequests = participationRequestRepository.countConfirmedRequestsByEventId(eventId);
        Long views = statsService.getEventViews(eventId);
        return eventMapper.toEventFullDto(event, confirmedRequests, views);
    }

    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            throw new ForbiddenException("Only pending or canceled events can be changed");
        }

        if (updateRequest.getEventDate() != null) {
            if (updateRequest.getEventDate().isBefore(LocalDateTime.now())) {
                throw new ForbiddenException("Event date cannot be in the past");
            }
            if (updateRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT))) {
                throw new ForbiddenException("Event date must be at least " + MIN_HOURS_BEFORE_EVENT + " hours from now");
            }
        }

        updateEventFields(event, updateRequest);

        if (updateRequest.getStateAction() != null) {
            switch (updateRequest.getStateAction()) {
                case "SEND_TO_REVIEW":
                    event.setState(EventState.PENDING);
                    break;
                case "CANCEL_REVIEW":
                    event.setState(EventState.CANCELED);
                    break;
            }
        }

        Event updatedEvent = eventRepository.save(event);
        Long confirmedRequests = participationRequestRepository.countConfirmedRequestsByEventId(eventId);
        Long views = statsService.getEventViews(eventId);
        return eventMapper.toEventFullDto(updatedEvent, confirmedRequests, views);
    }

    public List<EventFullDto> getEventsByAdmin(List<Long> users, List<EventState> states, List<Long> categories,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd, Pageable pageable) {
        List<Event> events = eventRepository.findEventsByAdmin(users, states, categories, rangeStart, rangeEnd, pageable);
        return events.stream()
                .map(event -> {
                    Long confirmedRequests = participationRequestRepository.countConfirmedRequestsByEventId(event.getId());
                    Long views = statsService.getEventViews(event.getId());
                    return eventMapper.toEventFullDto(event, confirmedRequests, views);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (updateRequest.getEventDate() != null) {
            if (updateRequest.getEventDate().isBefore(LocalDateTime.now())) {
                throw new ForbiddenException("Event date cannot be in the past");
            }
            if (updateRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                throw new ForbiddenException("Event date must be at least 1 hour from now");
            }
        }

        if (updateRequest.getStateAction() != null) {
            if ("PUBLISH_EVENT".equals(updateRequest.getStateAction())) {
                if (event.getState() != EventState.PENDING) {
                    throw new ForbiddenException("Cannot publish the event because it's not in the right state: " + event.getState());
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if ("REJECT_EVENT".equals(updateRequest.getStateAction())) {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ForbiddenException("Cannot reject the event because it's already published");
                }
                event.setState(EventState.CANCELED);
            }
        }

        updateEventFields(event, updateRequest);
        Event updatedEvent = eventRepository.save(event);
        Long confirmedRequests = participationRequestRepository.countConfirmedRequestsByEventId(eventId);
        Long views = statsService.getEventViews(eventId);
        return eventMapper.toEventFullDto(updatedEvent, confirmedRequests, views);
    }

    public List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, Pageable pageable) {

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }

        if (onlyAvailable == null) {
            onlyAvailable = false;
        }

        List<Event> events = eventRepository.findEventsPublic(text, categories, paid, rangeStart, rangeEnd, pageable);

        if (onlyAvailable) {
            events = events.stream()
                    .filter(event -> event.getParticipantLimit() == 0 ||
                            event.getParticipantLimit() > participationRequestRepository.countConfirmedRequestsByEventId(event.getId()))
                    .collect(Collectors.toList());
        }

        return events.stream()
                .map(event -> {
                    Long confirmedRequests = participationRequestRepository.countConfirmedRequestsByEventId(event.getId());
                    Long views = statsService.getEventViews(event.getId());
                    return eventMapper.toEventShortDto(event, confirmedRequests, views);
                })
                .collect(Collectors.toList());
    }

    public EventFullDto getEventPublic(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }

        Long confirmedRequests = participationRequestRepository.countConfirmedRequestsByEventId(eventId);
        Long views = statsService.getEventViews(eventId);
        return eventMapper.toEventFullDto(event, confirmedRequests, views);
    }

    private void updateEventFields(Event event, Object updateRequest) {
        if (updateRequest instanceof UpdateEventUserRequest) {
            UpdateEventUserRequest userRequest = (UpdateEventUserRequest) updateRequest;
            updateCommonFields(event, userRequest.getAnnotation(), userRequest.getCategory(),
                    userRequest.getDescription(), userRequest.getEventDate(), userRequest.getLocation(),
                    userRequest.getPaid(), userRequest.getParticipantLimit(), userRequest.getRequestModeration(),
                    userRequest.getTitle());
        } else if (updateRequest instanceof UpdateEventAdminRequest) {
            UpdateEventAdminRequest adminRequest = (UpdateEventAdminRequest) updateRequest;
            updateCommonFields(event, adminRequest.getAnnotation(), adminRequest.getCategory(),
                    adminRequest.getDescription(), adminRequest.getEventDate(), adminRequest.getLocation(),
                    adminRequest.getPaid(), adminRequest.getParticipantLimit(), adminRequest.getRequestModeration(),
                    adminRequest.getTitle());
        }
    }

    private void updateCommonFields(Event event, String annotation, Long categoryId, String description,
                                    LocalDateTime eventDate, ru.practicum.dto.common.Location location,
                                    Boolean paid, Integer participantLimit, Boolean requestModeration, String title) {
        if (annotation != null) event.setAnnotation(annotation);
        if (categoryId != null) {
            event.setCategory(categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new NotFoundException("Category with id=" + categoryId + " was not found")));
        }
        if (description != null) event.setDescription(description);
        if (eventDate != null) event.setEventDate(eventDate);
        if (location != null) {
            event.setLocationLat(location.getLat());
            event.setLocationLon(location.getLon());
        }
        if (paid != null) event.setPaid(paid);
        if (participantLimit != null) event.setParticipantLimit(participantLimit);
        if (requestModeration != null) event.setRequestModeration(requestModeration);
        if (title != null) event.setTitle(title);
    }
}