package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.model.request.ParticipationRequest;
import ru.practicum.model.request.RequestStatus;

import java.util.List;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    List<ParticipationRequest> findByRequesterId(Long userId);

    List<ParticipationRequest> findByEventId(Long eventId);

    List<ParticipationRequest> findByEventInitiatorIdAndEventId(Long userId, Long eventId);

    Optional<ParticipationRequest> findByRequesterIdAndEventId(Long userId, Long eventId);

    Long countByEventIdAndStatus(Long eventId, RequestStatus status);

    List<ParticipationRequest> findByIdIn(List<Long> requestIds);
}