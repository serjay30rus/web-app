package ru.kutepov.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.kutepov.extractor.RunExtractor;
import ru.kutepov.model.dto.interfaces.IndexPageId;
import ru.kutepov.model.dto.interfaces.ModelId;
import ru.kutepov.model.dto.interfaces.PageRelevanceAndData;
import ru.kutepov.model.dto.search.PageSearchDto;
import ru.kutepov.responses.SearchResponse;
import ru.kutepov.utils.LemmaFinder;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

@Service
public class SearchService {
    private final LemmaFinder lemmaFinder;
    @Autowired
    LemmaService lemmaService;
    @Autowired
    IndexService indexService;
    @Autowired
    SiteService siteService;


    public SearchService() throws IOException {
        this.lemmaFinder = new LemmaFinder();
    }

    public Object search(String findQuery, String siteUrl, int offset, int limit) throws SQLException {
        Set<String> findQueryLemmas = lemmaFinder.getLemmaSet(findQuery);

        Set<ModelId> allLemmasIds = new HashSet<>();
        Set<IndexPageId> allPageIds = new HashSet<>();

        if (siteUrl == null) {
            siteService.findAllSites().forEach(site -> {
                List<ModelId> lemmasIdsOfSite = lemmaService.findLemmasIdBySiteOrderByFrequency(findQueryLemmas, site);
                allLemmasIds.addAll(lemmasIdsOfSite);
                allPageIds.addAll(getPageIdsOfSite(lemmasIdsOfSite));
            });
        } else {
            List<ModelId> lemmasIdsOfSite = lemmaService.findLemmasIdBySiteOrderByFrequency(findQueryLemmas,
                    siteService.findSiteByName(siteUrl));
            allLemmasIds.addAll(lemmasIdsOfSite);
            allPageIds.addAll(getPageIdsOfSite(lemmasIdsOfSite));
        }

        List<PageRelevanceAndData> pageData = indexService.findPageRelevanceAndData(allPageIds, allLemmasIds,
                limit, offset);

        return new SearchResponse(allPageIds.size(), createSearchResult(pageData, findQuery));
    }

    private List<IndexPageId> getPageIdsOfSite(List<ModelId> lemmasIdsOfSite) {
        List<IndexPageId> pageIdsOfSite = new ArrayList<>();
        if (!lemmasIdsOfSite.isEmpty()) {
            pageIdsOfSite = indexService.findPagesIds(lemmasIdsOfSite.get(0).getId());
            if (lemmasIdsOfSite.size() > 2) {
                for (int lemma = 1; lemma < lemmasIdsOfSite.size() - 1; lemma++) {
                    pageIdsOfSite = indexService.getPagesIdOfNextLemmas(lemmasIdsOfSite.get(lemma).getId(),
                            pageIdsOfSite);
                }
                return pageIdsOfSite;
            }
        }
        return pageIdsOfSite;
    }

    private ArrayList<PageSearchDto> createSearchResult(List<PageRelevanceAndData> pageData, String findQuery) {
        ArrayList<PageSearchDto> searchResult = new ArrayList<>();

        pageData.forEach(pageRelevanceAndData -> {
            PageSearchDto searchDto = new PageSearchDto();
            searchDto.setSite(pageRelevanceAndData.getSite());
            searchDto.setSiteName(pageRelevanceAndData.getSiteName());
            searchDto.setUri(pageRelevanceAndData.getUri());
            searchDto.setTitle(RunExtractor.getTitle(pageRelevanceAndData.getContent()));
            searchDto.setSnippet(RunExtractor.getSnippetInHtml(pageRelevanceAndData.getContent(), findQuery));
            searchDto.setRelevance(pageRelevanceAndData.getRelevance());
            searchResult.add(searchDto);
        });

        return searchResult;
    }
}
