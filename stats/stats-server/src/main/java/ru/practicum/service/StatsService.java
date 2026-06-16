package ru.practicum.service;

import ru.practicum.dto.EndpointHitDto;

public interface StatsService {
    void saveHit(EndpointHitDto hitDto);
}
