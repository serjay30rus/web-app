package ru.kutepov.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.kutepov.config.SitesConfig;
import ru.kutepov.extractor.RunExtractor;
import ru.kutepov.model.Site;
import ru.kutepov.responses.ErrorResponse;
import ru.kutepov.service.SiteService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

@RestController
@RequiredArgsConstructor
public class IndexingController {
    @Autowired
    private RunExtractor runExtractor;
    @Autowired
    private SitesConfig sitesConfig;
    @Autowired
    private SiteService siteService;

    @GetMapping("/start-indexing")
    public ResponseEntity<Object> startIndexing() throws IOException {
        if (!siteService.isIndexingStarted()) {
            return ResponseEntity.ok(runExtractor.startExtract());
        }
        return ResponseEntity.badRequest().body(new ErrorResponse("Индексация уже запущена"));
    }

    @GetMapping("/stop-indexing")
    public ResponseEntity<Object> stopIndexing() {
        if (siteService.isIndexingStarted()) {
            return ResponseEntity.ok(runExtractor.stopIndexing());
        }
        return ResponseEntity.badRequest().body(new ErrorResponse("Индексация не запущена"));
    }

    @GetMapping("/index-page")      // /index-page?url=http://www.playback.ru
    public ResponseEntity<Object> indexPage(@RequestParam(value = "url") String url) throws SQLException, IOException, InterruptedException {
        if (!siteService.isIndexingStarted()) {
            List<Site> siteArrayList = sitesConfig.getFileTypes();
            for (Site siteFromConfig : siteArrayList) {
                if (url.toLowerCase(Locale.ROOT).contains(siteFromConfig.getUrl())) {
                    return ResponseEntity.ok(runExtractor.startExtractSinglePage(url, siteFromConfig));
                }
            }
            return ResponseEntity.badRequest().body(new ErrorResponse("Данная страница находится за пределами сайтов, " +
                    "указаных в конфигурационном файле."));
        }
        return ResponseEntity.badRequest().body(new ErrorResponse("Индексация уже запущена. Остановите индексацию, " +
                "или дождитесь ее окончания"));
    }

}
