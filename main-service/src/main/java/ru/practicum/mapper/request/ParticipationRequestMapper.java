package ru.practicum.mapper.request;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.model.ParticipationRequest;

@Mapper(componentModel = "spring")
public interface ParticipationRequestMapper {

    String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @Mapping(target = "created", expression = "java(participationRequest.getCreated().format(java.time.format.DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)))")
    @Mapping(target = "event", source = "participationRequest.event.id")
    @Mapping(target = "requester", source = "participationRequest.requester.id")
    @Mapping(target = "status", source = "participationRequest.status")
    ParticipationRequestDto toParticipationRequestDto(ParticipationRequest participationRequest);
}