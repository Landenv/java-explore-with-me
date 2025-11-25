package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.model.RequestStatus;

import java.util.List;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    List<ParticipationRequest> findByRequesterId(Long userId);

    List<ParticipationRequest> findByEventId(Long eventId);

    List<ParticipationRequest> findByEventIdAndStatus(Long eventId, RequestStatus status);

    Optional<ParticipationRequest> findByEventIdAndRequesterId(Long eventId, Long userId);

    @Query("SELECT COUNT(participationRequest) FROM ParticipationRequest participationRequest WHERE participationRequest.event.id = :eventId AND participationRequest.status = 'CONFIRMED'")
    Long countConfirmedRequestsByEventId(@Param("eventId") Long eventId);

    List<ParticipationRequest> findByIdIn(List<Long> requestIds);

    @Query("SELECT pr FROM ParticipationRequest pr WHERE pr.id IN :requestIds")
    List<ParticipationRequest> findByIds(@Param("requestIds") List<Long> requestIds);
}