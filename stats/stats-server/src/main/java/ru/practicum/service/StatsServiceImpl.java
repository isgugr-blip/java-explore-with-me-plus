package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.dto.endpointHit.EndpointHitDto;
import ru.practicum.dto.EndpointStatsResponseDto;
import ru.practicum.exceptions.ValidationException;
import ru.practicum.mapper.EndpointHitMapper;
import ru.practicum.model.EndpointHit;
import ru.practicum.repository.HitRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final HitRepository hitRepository;

    @Override
    public void saveHit(EndpointHitDto dto) {
        EndpointHit hit = EndpointHitMapper.toEntity(dto);
        hitRepository.save(hit);
    }

    @Override
    public List<EndpointStatsResponseDto> getStats(String start, String end, List<String> uris, boolean unique) {

        LocalDateTime startTime = LocalDateTime.parse(start, FORMATTER);
        LocalDateTime endTime = LocalDateTime.parse(end, FORMATTER);

        if (endTime.isBefore(startTime)) {
            throw new ValidationException("End time should be after start time");
        }

        if (uris != null && !uris.isEmpty()) {
            return unique ? hitRepository.getStatsUniqueByUris(startTime, endTime, uris) : hitRepository.getStatsWithUris(startTime, endTime, uris);
        }

        return unique ? hitRepository.getStatsUnique(startTime, endTime) : hitRepository.getStats(startTime, endTime);
    }
}
