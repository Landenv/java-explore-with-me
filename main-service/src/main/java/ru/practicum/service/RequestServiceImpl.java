package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.event.Event;
import ru.practicum.model.event.EventState;
import ru.practicum.model.request.ParticipationRequest;
import ru.practicum.model.request.RequestStatus;
import ru.practicum.model.user.User;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        validateIds(userId, eventId);
        User user = findUserById(userId);
        Event event = findEventById(eventId);

        validateRequestCreation(user, event);
        RequestStatus status = determineRequestStatus(event);

        ParticipationRequest request = buildRequest(user, event, status);
        ParticipationRequest savedRequest = requestRepository.save(request);

        updateConfirmedRequestsIfConfirmed(savedRequest, event);

        return RequestMapper.toDto(savedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        findUserById(userId);
        return requestRepository.findByRequesterId(userId).stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = findRequestById(requestId);
        validateRequestOwnership(request, userId);

        RequestStatus originalStatus = request.getStatus();
        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest updatedRequest = requestRepository.save(request);

        updateConfirmedRequestsIfCanceled(originalStatus, request.getEvent());

        return RequestMapper.toDto(updatedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        Event event = findEventById(eventId);
        validateEventInitiator(event, userId);

        return requestRepository.findByEventInitiatorIdAndEventId(userId, eventId).stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        Event event = findEventById(eventId);
        validateEventInitiator(event, userId);

        List<ParticipationRequest> requests = findRequestsByIds(updateRequest.getRequestIds());
        validateAllRequestsPending(requests);

        return processRequestStatusUpdate(event, requests, updateRequest.getStatus());
    }

    private void validateIds(Long userId, Long eventId) {
        if (userId == null || userId < 1) {
            throw new ValidationException("ID пользователя должен быть положительным");
        }
        if (eventId == null || eventId < 1) {
            throw new ValidationException("ID события должен быть положительным");
        }
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }

    private Event findEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
    }

    private void validateRequestCreation(User user, Event event) {
        validateDuplicateRequest(user.getId(), event.getId());
        validateNotInitiator(user.getId(), event);
        validateEventPublished(event);
        validateParticipantLimit(event);
    }

    private void validateDuplicateRequest(Long userId, Long eventId) {
        requestRepository.findByRequesterIdAndEventId(userId, eventId)
                .ifPresent(request -> {
                    throw new ConflictException("Запрос уже существует");
                });
    }

    private void validateNotInitiator(Long userId, Event event) {
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор не может подавать заявку на участие в своем собственном событии");
        }
    }

    private void validateEventPublished(Event event) {
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }
    }

    private void validateParticipantLimit(Event event) {
        if (event.getParticipantLimit() != null && event.getParticipantLimit() > 0) {
            Long confirmedRequests = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
            if (confirmedRequests >= event.getParticipantLimit()) {
                throw new ConflictException("Достигнут лимит участников");
            }
        }
    }

    private RequestStatus determineRequestStatus(Event event) {
        boolean requiresModeration = event.getRequestModeration() != null && event.getRequestModeration();
        boolean hasLimit = event.getParticipantLimit() != null && event.getParticipantLimit() > 0;

        return (requiresModeration && hasLimit) ? RequestStatus.PENDING : RequestStatus.CONFIRMED;
    }

    private ParticipationRequest buildRequest(User user, Event event, RequestStatus status) {
        return ParticipationRequest.builder()
                .requester(user)
                .event(event)
                .status(status)
                .build();
    }

    private void updateConfirmedRequestsIfConfirmed(ParticipationRequest request, Event event) {
        if (request.getStatus() == RequestStatus.CONFIRMED) {
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
            eventRepository.save(event);
        }
    }

    private ParticipationRequest findRequestById(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос не найден"));
    }

    private void validateRequestOwnership(ParticipationRequest request, Long userId) {
        if (!request.getRequester().getId().equals(userId)) {
            throw new NotFoundException("Запрос не найден для текущего пользователя");
        }
    }

    private void updateConfirmedRequestsIfCanceled(RequestStatus originalStatus, Event event) {
        // Исправление - проверяем исходный статус, а не измененный
        if (originalStatus == RequestStatus.CONFIRMED) {
            event.setConfirmedRequests(event.getConfirmedRequests() - 1);
            eventRepository.save(event);
        }
    }

    private void validateEventInitiator(Event event, Long userId) {
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Пользователь не является инициатором этого события");
        }
    }


    private List<ParticipationRequest> findRequestsByIds(List<Long> requestIds) {
        return requestRepository.findByIdIn(requestIds);
    }

    private void validateAllRequestsPending(List<ParticipationRequest> requests) {
        if (hasNonPendingRequests(requests)) {
            throw new ConflictException("Запрос должен иметь статус PENDING");
        }
    }

    private EventRequestStatusUpdateResult processRequestStatusUpdate(Event event,
                                                                      List<ParticipationRequest> requests,
                                                                      String status) {
        if ("CONFIRMED".equals(status)) {
            return confirmRequests(event, requests);
        } else if ("REJECTED".equals(status)) {
            return rejectRequests(requests);
        } else {
            throw new ValidationException("Неизвестный статус: " + status);
        }
    }

    private EventRequestStatusUpdateResult confirmRequests(Event event, List<ParticipationRequest> requests) {
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();

        if (event.getParticipantLimit() != null && event.getParticipantLimit() > 0) {
            confirmRequestsWithLimit(event, requests, result);
        } else {
            confirmAllRequests(event, requests, result);
        }

        eventRepository.save(event);
        return result;
    }

    private void confirmRequestsWithLimit(Event event, List<ParticipationRequest> requests,
                                          EventRequestStatusUpdateResult result) {
        Long confirmedCount = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        int availableSlots = event.getParticipantLimit() - confirmedCount.intValue();

        if (availableSlots <= 0) {
            throw new ConflictException("Достигнут лимит участников");
        }

        for (ParticipationRequest request : requests) {
            if (availableSlots > 0) {
                confirmRequest(request, event, result);
                availableSlots--;
            } else {
                rejectRequest(request, result);
            }
        }
    }

    private void confirmAllRequests(Event event, List<ParticipationRequest> requests,
                                    EventRequestStatusUpdateResult result) {
        for (ParticipationRequest request : requests) {
            confirmRequest(request, event, result);
        }
    }

    private void confirmRequest(ParticipationRequest request, Event event,
                                EventRequestStatusUpdateResult result) {
        request.setStatus(RequestStatus.CONFIRMED);
        requestRepository.save(request);
        result.getConfirmedRequests().add(RequestMapper.toDto(request));
        event.setConfirmedRequests(event.getConfirmedRequests() + 1);
    }

    private EventRequestStatusUpdateResult rejectRequests(List<ParticipationRequest> requests) {
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();

        for (ParticipationRequest request : requests) {
            rejectRequest(request, result);
        }

        return result;
    }

    private void rejectRequest(ParticipationRequest request, EventRequestStatusUpdateResult result) {
        request.setStatus(RequestStatus.REJECTED);
        requestRepository.save(request);
        result.getRejectedRequests().add(RequestMapper.toDto(request));
    }

    private boolean hasNonPendingRequests(List<ParticipationRequest> requests) {
        for (ParticipationRequest request : requests) {
            if (request.getStatus() != RequestStatus.PENDING) {
                return true;
            }
        }
        return false;
    }
}