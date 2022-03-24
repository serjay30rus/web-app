package ru.kutepov.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.kutepov.responses.StatisticResponse;
import ru.kutepov.service.StatisticsService;

@RestController
@RequiredArgsConstructor
public class StatisticsController {
    @Autowired
    private StatisticsService statisticsService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticResponse> getStatistics() {

        return ResponseEntity.ok(statisticsService.getStatistics());
    }
}
