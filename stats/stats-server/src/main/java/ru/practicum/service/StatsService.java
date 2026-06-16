package ru.practicum.service;

import ru.practicum.dto.endpointHit.EndpointHitDto;

public interface StatsService {
    void saveHit(EndpointHitDto hitDto);
}
