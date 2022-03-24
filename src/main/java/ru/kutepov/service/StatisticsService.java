package ru.kutepov.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.kutepov.model.dto.statistics.DetailedStatisticsDto;
import ru.kutepov.model.dto.statistics.StatisticsDto;
import ru.kutepov.model.dto.statistics.TotalStatisticsDto;
import ru.kutepov.repository.LemmaRepository;
import ru.kutepov.repository.PageRepository;
import ru.kutepov.repository.SiteRepository;
import ru.kutepov.responses.StatisticResponse;

import java.util.ArrayList;
import java.util.List;

@Service
public class StatisticsService {
    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private LemmaRepository lemmaRepository;


    public StatisticResponse getStatistics() {
        TotalStatisticsDto totalStatisticsDto = new TotalStatisticsDto(siteRepository.count(), pageRepository.count(),
                lemmaRepository.count(), true);

        List<DetailedStatisticsDto> detailedStatisticsDtoList = new ArrayList<>();

        siteRepository.findAll().forEach(site -> {
            DetailedStatisticsDto detailedStatisticsDto = new DetailedStatisticsDto(site.getUrl(), site.getName(),
                    site.getStatus(), site.getStatusTime(), site.getLastError(),
                    pageRepository.countBySiteBySiteId(site), lemmaRepository.countBySiteBySiteId(site));
            detailedStatisticsDtoList.add(detailedStatisticsDto);
        });

        return new StatisticResponse(new StatisticsDto(totalStatisticsDto, detailedStatisticsDtoList));
    }
}
