package ru.practicum.mapper;

import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.model.request.ParticipationRequest;

public class RequestMapper {

    public static ParticipationRequestDto toDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated()) // LocalDateTime передается как есть
                .event(request.getEvent().getId())
                .requester(request.getRequester().getId())
                .status(request.getStatus().name())
                .build();
    }
}