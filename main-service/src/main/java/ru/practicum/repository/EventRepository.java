package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Event;
import ru.practicum.model.EventState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByInitiatorId(Long userId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

    @Query("SELECT event FROM Event event WHERE " +
            "(:users IS NULL OR event.initiator.id IN :users) AND " +
            "(:states IS NULL OR event.state IN :states) AND " +
            "(:categories IS NULL OR event.category.id IN :categories) AND " +
            "(:rangeStart IS NULL OR event.eventDate >= :rangeStart) AND " +
            "(:rangeEnd IS NULL OR event.eventDate <= :rangeEnd)")
    List<Event> findEventsByAdmin(@Param("users") List<Long> users,
                                  @Param("states") List<EventState> states,
                                  @Param("categories") List<Long> categories,
                                  @Param("rangeStart") LocalDateTime rangeStart,
                                  @Param("rangeEnd") LocalDateTime rangeEnd,
                                  Pageable pageable);

    @Query("""
            SELECT e FROM Event e
            WHERE e.state = 'PUBLISHED'
               AND (:text IS NULL OR (
                 LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%'))
                 OR LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%'))
               ))
              AND (:categories IS NULL OR e.category.id IN :categories)
              AND (:paid IS NULL OR e.paid = :paid)
              AND e.eventDate >= COALESCE(:rangeStart, e.eventDate)
              AND e.eventDate <= COALESCE(:rangeEnd, e.eventDate)
              AND (
                    :onlyAvailable IS NULL OR :onlyAvailable = false
                    OR e.participantLimit = 0
                    OR e.confirmedRequests < e.participantLimit
                  )
            """)
    List<Event> findEventsPublic(@Param("text") String text,
                                 @Param("categories") List<Long> categories,
                                 @Param("paid") Boolean paid,
                                 @Param("rangeStart") LocalDateTime rangeStart,
                                 @Param("rangeEnd") LocalDateTime rangeEnd,
                                 @Param("onlyAvailable") Boolean onlyAvailable,
                                 Pageable pageable);

    List<Event> findByIdIn(List<Long> eventIds);

    Boolean existsByCategoryId(Long categoryId);
}