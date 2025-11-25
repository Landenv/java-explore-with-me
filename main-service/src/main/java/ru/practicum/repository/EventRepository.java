package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.event.Event;
import ru.practicum.model.event.EventState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByInitiatorId(Long userId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

    @Query("""
            SELECT e FROM Event e
            WHERE (:users IS NULL OR e.initiator.id IN :users)
              AND (:states IS NULL OR e.state IN :states)
              AND (:categories IS NULL OR e.category.id IN :categories)
              AND e.eventDate >= COALESCE(:rangeStart, e.eventDate)
              AND e.eventDate <= COALESCE(:rangeEnd,   e.eventDate)
            ORDER BY e.id
            """)
    List<Event> findAdminEvents(@Param("users") List<Long> users,
                                @Param("states") List<EventState> states,
                                @Param("categories") List<Long> categories,
                                @Param("rangeStart") LocalDateTime rangeStart,
                                @Param("rangeEnd") LocalDateTime rangeEnd,
                                Pageable pageable);

    @Query("""
            SELECT e FROM Event e
            WHERE e.state = 'PUBLISHED'
               AND (:text IS NULL OR (
                 LOWER(e.annotation) LIKE :text
                 OR LOWER(e.description) LIKE :text
               ))
              AND (:categories IS NULL OR e.category.id IN :categories)
              AND (:paid IS NULL OR e.paid = :paid)
              AND e.eventDate >= COALESCE(:rangeStart, e.eventDate)
              AND e.eventDate <= COALESCE(:rangeEnd,   e.eventDate)
              AND (
                    :onlyAvailable IS NULL OR :onlyAvailable = false
                    OR COALESCE(e.participantLimit, 0) = 0
                    OR COALESCE(e.confirmedRequests, 0) < COALESCE(e.participantLimit, 0)
                  )
            ORDER BY e.id
            """)
    List<Event> findPublicEvents(@Param("text") String text,
                                 @Param("categories") List<Long> categories,
                                 @Param("paid") Boolean paid,
                                 @Param("rangeStart") LocalDateTime rangeStart,
                                 @Param("rangeEnd") LocalDateTime rangeEnd,
                                 @Param("onlyAvailable") Boolean onlyAvailable,
                                 Pageable pageable);

    List<Event> findByIdIn(List<Long> eventIds);

    List<Event> findByCategoryId(Long categoryId);

    Optional<Event> findByIdAndState(Long id, EventState state);
}