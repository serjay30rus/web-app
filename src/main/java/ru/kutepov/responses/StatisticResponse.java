package ru.kutepov.responses;

import lombok.Data;
import ru.kutepov.model.dto.statistics.StatisticsDto;

@Data
public class StatisticResponse {
    private String result = "true";

    private StatisticsDto statistics;

    public StatisticResponse(StatisticsDto statisticsDto) {
        this.statistics = statisticsDto;
    }

}
