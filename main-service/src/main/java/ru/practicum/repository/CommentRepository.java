package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.comment.Comment;
import ru.practicum.model.comment.CommentStatus;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByEventIdAndStatus(Long eventId, CommentStatus status, Pageable pageable);

    List<Comment> findByAuthorId(Long userId, Pageable pageable);

    Optional<Comment> findByIdAndAuthorId(Long commentId, Long userId);

    List<Comment> findByEventId(Long eventId, Pageable pageable);

    List<Comment> findByStatus(CommentStatus status, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE " +
            "(:eventIds IS NULL OR c.event.id IN :eventIds) AND " +
            "(:statuses IS NULL OR c.status IN :statuses)")
    List<Comment> findAdminComments(@Param("eventIds") List<Long> eventIds,
                                    @Param("statuses") List<CommentStatus> statuses,
                                    Pageable pageable);

    boolean existsByAuthorIdAndEventId(Long userId, Long eventId);
}