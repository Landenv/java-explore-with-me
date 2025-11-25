package ru.practicum.service.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.request.ParticipationRequestMapper;
import ru.practicum.model.Event;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.model.RequestStatus;
import ru.practicum.model.User;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestService {
    private final ParticipationRequestRepository participationRequestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ParticipationRequestMapper participationRequestMapper;

    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (participationRequestRepository.findByEventIdAndRequesterId(eventId, userId).isPresent()) {
            throw new ConflictException("Request already exists");
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Initiator cannot request participation in own event");
        }

        if (!event.getState().equals(ru.practicum.model.EventState.PUBLISHED)) {
            throw new ConflictException("Event is not published");
        }

        if (event.getParticipantLimit() > 0 &&
                participationRequestRepository.countConfirmedRequestsByEventId(eventId) >= event.getParticipantLimit()) {
            throw new ConflictException("Participant limit reached");
        }

        RequestStatus status;
        if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
            status = RequestStatus.CONFIRMED;
        } else {
            status = RequestStatus.PENDING;
        }

        ParticipationRequest participationRequest = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requester(user)
                .status(status)
                .build();

        ParticipationRequest savedParticipationRequest = participationRequestRepository.save(participationRequest);
        return participationRequestMapper.toParticipationRequestDto(savedParticipationRequest);
    }

    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        return participationRequestRepository.findByRequesterId(userId).stream()
                .map(participationRequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest participationRequest = participationRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));

        if (!participationRequest.getRequester().getId().equals(userId)) {
            throw new NotFoundException("User can only cancel own requests");
        }

        participationRequest.setStatus(RequestStatus.CANCELED);
        ParticipationRequest updatedParticipationRequest = participationRequestRepository.save(participationRequest);
        return participationRequestMapper.toParticipationRequestDto(updatedParticipationRequest);
    }

    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Only event initiator can view participants");
        }

        return participationRequestRepository.findByEventId(eventId).stream()
                .map(participationRequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Only event initiator can change request status");
        }

        // Исправление: преобразуем массив в список
        List<Long> requestIds = Arrays.asList(updateRequest.getRequestIds());
        List<ParticipationRequest> requests = participationRequestRepository.findByIds(requestIds);

        if (requests.stream().anyMatch(req -> req.getStatus() != RequestStatus.PENDING)) {
            throw new ConflictException("Request must have status PENDING");
        }

        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

        long confirmedCount = participationRequestRepository.countConfirmedRequestsByEventId(eventId);
        int participantLimit = event.getParticipantLimit();

        for (ParticipationRequest request : requests) {
            if ("CONFIRMED".equals(updateRequest.getStatus())) {
                if (participantLimit > 0 && confirmedCount >= participantLimit) {
                    throw new ConflictException("The participant limit has been reached");
                }
                request.setStatus(RequestStatus.CONFIRMED);
                confirmedCount++;
                confirmedRequests.add(participationRequestMapper.toParticipationRequestDto(request));
            } else if ("REJECTED".equals(updateRequest.getStatus())) {
                request.setStatus(RequestStatus.REJECTED);
                rejectedRequests.add(participationRequestMapper.toParticipationRequestDto(request));
            }
        }

        participationRequestRepository.saveAll(requests);

        if ("CONFIRMED".equals(updateRequest.getStatus()) && participantLimit > 0 && confirmedCount >= participantLimit) {
            List<ParticipationRequest> pendingRequests = participationRequestRepository.findByEventIdAndStatus(eventId, RequestStatus.PENDING);
            for (ParticipationRequest pendingRequest : pendingRequests) {
                pendingRequest.setStatus(RequestStatus.REJECTED);
                rejectedRequests.add(participationRequestMapper.toParticipationRequestDto(pendingRequest));
            }
            participationRequestRepository.saveAll(pendingRequests);
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedRequests.toArray(new ParticipationRequestDto[0]))
                .rejectedRequests(rejectedRequests.toArray(new ParticipationRequestDto[0]))
                .build();
    }
}