package ru.practicum.service;

import ru.practicum.dto.event.*;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {
    EventFullDto createEvent(Long userId, NewEventDto newEventDto);
    List<EventShortDto> getUserEvents(Long userId, int from, int size);
    EventFullDto getUserEvent(Long userId, Long eventId);
    EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest);
    List<EventFullDto> getAdminEvents(List<Long> users, List<String> states, List<Long> categories,
                                      LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size);
    EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest updateRequest);
    List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                        LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                        Boolean onlyAvailable, String sort, int from, int size,
                                        String clientIp, String requestUri);
    EventFullDto getPublicEvent(Long eventId, String clientIp, String requestUri);
}