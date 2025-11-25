package ru.practicum.mapper.event;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.common.Location;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.model.Category;
import ru.practicum.model.Event;
import ru.practicum.model.User;
import ru.practicum.repository.CategoryRepository;

import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring", uses = {ru.practicum.mapper.user.UserMapper.class, ru.practicum.mapper.category.CategoryMapper.class})
public interface EventMapper {

    DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    default Event toEvent(NewEventDto newEventDto, User user, CategoryRepository categoryRepository) {
        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        return Event.builder()
                .annotation(newEventDto.getAnnotation())
                .category(category)
                .createdOn(java.time.LocalDateTime.now())
                .description(newEventDto.getDescription())
                .eventDate(newEventDto.getEventDate())
                .initiator(user)
                .locationLat(newEventDto.getLocation().getLat())
                .locationLon(newEventDto.getLocation().getLon())
                .paid(newEventDto.getPaid())
                .participantLimit(newEventDto.getParticipantLimit())
                .requestModeration(newEventDto.getRequestModeration())
                .state(ru.practicum.model.EventState.PENDING)
                .title(newEventDto.getTitle())
                .confirmedRequests(0L)
                .views(0L)
                .build();
    }

    @Mapping(target = "location", expression = "java(mapToLocation(event.getLocationLat(), event.getLocationLon()))")
    @Mapping(target = "confirmedRequests", source = "confirmedRequests")
    @Mapping(target = "views", source = "views")
    @Mapping(target = "state", source = "event.state")
    @Mapping(target = "eventDate", expression = "java(event.getEventDate().format(DATE_TIME_FORMATTER))")
    @Mapping(target = "createdOn", expression = "java(event.getCreatedOn().format(DATE_TIME_FORMATTER))")
    @Mapping(target = "publishedOn", expression = "java(event.getPublishedOn() != null ? event.getPublishedOn().format(DATE_TIME_FORMATTER) : null)")
    EventFullDto toEventFullDto(Event event, Long confirmedRequests, Long views);

    @Mapping(target = "confirmedRequests", source = "confirmedRequests")
    @Mapping(target = "views", source = "views")
    @Mapping(target = "eventDate", expression = "java(event.getEventDate().format(DATE_TIME_FORMATTER))")
    EventShortDto toEventShortDto(Event event, Long confirmedRequests, Long views);

    default Location mapToLocation(Float lat, Float lon) {
        return Location.builder()
                .lat(lat)
                .lon(lon)
                .build();
    }
}