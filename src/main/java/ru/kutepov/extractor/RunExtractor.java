package ru.kutepov.extractor;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import ru.kutepov.config.SitesConfig;
import ru.kutepov.model.*;
import ru.kutepov.repository.*;
import ru.kutepov.responses.ResultResponse;
import ru.kutepov.service.IndexService;
import ru.kutepov.service.LemmaService;
import ru.kutepov.service.PageService;
import ru.kutepov.service.SiteService;
import ru.kutepov.utils.LemmaFinder;

@Component
@Transactional
public class RunExtractor {
    private static Logger mainExceptions = LogManager.getLogger("searchFile");
    public PageRepository pageRepository;
    public LinkRepository linkRepository;
    public FieldRepository fieldRepository;
    public LemmaRepository lemmaRepository;
    public IndexRepository indexRepository;
    public LemmaService lemmaService;
    public PageService pageService;
    public IndexService indexService;
    @Autowired
    private SitesConfig sitesConfig;
    @Autowired
    private SiteService siteService;
    List<Lemma> lemmaEntityList = new ArrayList<>();


    @Autowired
    public RunExtractor(PageRepository pageRepository, LinkRepository linkRepository,
                        FieldRepository fieldRepository, LemmaRepository lemmaRepository,
                        IndexRepository indexRepository){
        this.pageRepository = pageRepository;
        this.linkRepository = linkRepository;
        this.fieldRepository = fieldRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }


    public Object startExtract() throws IOException {                                       // Полная индексация
        List<Site> sitesConfigSites = sitesConfig.getFileTypes();
        siteService.setIndexingStarted(true);
        siteService.setIndexingStopFlag(false);
        List<String> indentedUrls = null;
        for(Site siteConfig : sitesConfigSites) {
            try {
                jsoupConnection(siteConfig.getUrl());
            } catch (Exception exp) {
                mainExceptions.error(exp);
            }
            Set<String> urls = Collections.synchronizedSet(new HashSet<>());                   // потокобезопасная коллекция для ссылок на дочерние страницы
            SiteExtractor rootTask = new SiteExtractor(urls, siteConfig.getUrl());           // собираем переходы на дочерние страницы со всех страниц сайта, начиная с корневого
            new ForkJoinPool().invoke(rootTask);

            indentedUrls = urls.stream().sorted(Comparator.comparing(u -> u))        // упорядочиваем собранные ссылки, добавлем отступы и сохраняем в файл
                    .map(u -> StringUtils.repeat('\t', StringUtils.countMatches(u, "/") - 2) + u)
                    .collect(Collectors.toList());

            SiteExtractor site = new SiteExtractor(urls, siteConfig.getUrl());

            List<HashMap<String, String>> listField = site.getField();
            List<String> listContent = site.getSiteContent();
            List<Integer> listSiteStatusCode = site.getSiteStatus();
            ArrayList<String> lemmatizeListContent = new ArrayList<>();
            Set<String> uniqalLemmaKeySet = new HashSet<>();                                                  // Создаём список уникальных лемм всего сайта
            for (String l : listContent) {
                lemmatizeListContent.add(LemmaFinder.lemmatize(l).keySet().toString());
            }

            // Saving entity to DB:

            Site dbSite = siteService.saveSiteIfNotExist(siteConfig);
            savingPageToDataBase(indentedUrls, listSiteStatusCode, listContent, dbSite);

            if (siteService.isIndexingStopFlag()) {
                siteService.updateStatus(dbSite, SiteStatusType.FAILED);
                siteService.updateErrorMessage(dbSite, "Indexing Stopped");
                return new ResultResponse();
            }

            savingLinkToDataBase(indentedUrls, listSiteStatusCode, listContent, dbSite);

            if (siteService.isIndexingStopFlag()) {
                siteService.updateStatus(dbSite, SiteStatusType.FAILED);
                siteService.updateErrorMessage(dbSite, "Indexing Stopped");
                return new ResultResponse();
            }

            savingFieldToDataBase(listField, dbSite);

            if (siteService.isIndexingStopFlag()) {
                siteService.updateStatus(dbSite, SiteStatusType.FAILED);
                siteService.updateErrorMessage(dbSite, "Indexing Stopped");
                return new ResultResponse();
            }

            savingLemmaToDataBase(listContent, site, lemmatizeListContent, uniqalLemmaKeySet, dbSite);

            if (siteService.isIndexingStopFlag()) {
                siteService.updateStatus(dbSite, SiteStatusType.FAILED);
                siteService.updateErrorMessage(dbSite, "Indexing Stopped");
                return new ResultResponse();
            }

            savingIndexToDataBase(indentedUrls, uniqalLemmaKeySet, listSiteStatusCode, listContent, site, lemmatizeListContent, dbSite);

            if (siteService.isIndexingStopFlag()) {
                siteService.updateStatus(dbSite, SiteStatusType.FAILED);
                siteService.updateErrorMessage(dbSite, "Indexing Stopped");
                return new ResultResponse();
            }

            siteConfig.setStatusTime(new Timestamp(System.currentTimeMillis()));
            siteConfig.setStatus(SiteStatusType.INDEXING);
            dbSite.setStatus(SiteStatusType.INDEXING);

            siteService.setIndexingStarted(false);
            siteService.updateStatus(dbSite, SiteStatusType.INDEXED);
        }
        return new ResultResponse();
    }

    public Object startExtractSinglePage(String url, Site siteConfig) throws IOException {
        siteService.setIndexingStarted(true);
        siteService.setIndexingStopFlag(false);
        List<String> indentedUrls = null;

            try {
                jsoupConnection(url);
            } catch (Exception exp) {
                mainExceptions.error(exp);
            }
            Set<String> urls = Collections.synchronizedSet(new HashSet<>());                   // потокобезопасная коллекция для ссылок на дочерние страницы
            SiteExtractor rootTask = new SiteExtractor(urls, url);           // собираем переходы на дочерние страницы со всех страниц сайта, начиная с корневого
            new ForkJoinPool().invoke(rootTask);

            indentedUrls = urls.stream().sorted(Comparator.comparing(u -> u))        // упорядочиваем собранные ссылки, добавлем отступы и сохраняем в файл
                    .map(u -> StringUtils.repeat('\t', StringUtils.countMatches(u, "/") - 2) + u)
                    .collect(Collectors.toList());

            SiteExtractor site = new SiteExtractor(urls, url);

            List<HashMap<String, String>> listField = site.getField();
            List<String> listContent = site.getSiteContent();
            List<Integer> listSiteStatusCode = site.getSiteStatus();
            ArrayList<String> lemmatizeListContent = new ArrayList<>();
            Set<String> uniqalLemmaKeySet = new HashSet<>();                                                  // Создаём список уникальных лемм всего сайта
            for (String l : listContent) {
                lemmatizeListContent.add(LemmaFinder.lemmatize(l).keySet().toString());
            }

            // Saving entity to DB:

            Site dbSite = siteService.saveSiteIfNotExist(siteConfig);
            savingPageToDataBase(indentedUrls, listSiteStatusCode, listContent, dbSite);

        if (siteService.isIndexingStopFlag()) {
            siteService.updateStatus(dbSite, SiteStatusType.FAILED);
            siteService.updateErrorMessage(dbSite, "Indexing Stopped");
            return new ResultResponse();
        }

            savingLinkToDataBase(indentedUrls, listSiteStatusCode, listContent, dbSite);

        if (siteService.isIndexingStopFlag()) {
            siteService.updateStatus(dbSite, SiteStatusType.FAILED);
            siteService.updateErrorMessage(dbSite, "Indexing Stopped");
            return new ResultResponse();
        }

            savingFieldToDataBase(listField, dbSite);

        if (siteService.isIndexingStopFlag()) {
            siteService.updateStatus(dbSite, SiteStatusType.FAILED);
            siteService.updateErrorMessage(dbSite, "Indexing Stopped");
            return new ResultResponse();
        }

            savingLemmaToDataBase(listContent, site, lemmatizeListContent, uniqalLemmaKeySet, dbSite);

        if (siteService.isIndexingStopFlag()) {
            siteService.updateStatus(dbSite, SiteStatusType.FAILED);
            siteService.updateErrorMessage(dbSite, "Indexing Stopped");
            return new ResultResponse();
        }

            savingIndexToDataBase(indentedUrls, uniqalLemmaKeySet, listSiteStatusCode, listContent, site, lemmatizeListContent, dbSite);

        if (siteService.isIndexingStopFlag()) {
            siteService.updateStatus(dbSite, SiteStatusType.FAILED);
            siteService.updateErrorMessage(dbSite, "Indexing Stopped");
            return new ResultResponse();
        }

            siteConfig.setStatusTime(new Timestamp(System.currentTimeMillis()));
            siteConfig.setStatus(SiteStatusType.INDEXING);
            dbSite.setStatus(SiteStatusType.INDEXING);

            siteService.setIndexingStarted(false);
            siteService.updateStatus(dbSite, SiteStatusType.INDEXED);
        return new ResultResponse();
    }

    private void jsoupConnection(String urlSource) throws IOException {
        Document doc = Jsoup.connect(urlSource).maxBodySize(0)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .timeout(500)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .get();
    }


    private void savingPageToDataBase(List<String> indentedUrls, List<Integer> listSiteStatusCode, List<String> listContent, Site dbSite) {
        for (int i = 0; i < 10; i++) {                                                              // !!!! 10 поменять на indentedUrls.size()
            if (siteService.isIndexingStopFlag()) {
                siteService.updateStatus(dbSite, SiteStatusType.FAILED);
                siteService.updateErrorMessage(dbSite, "Indexing Stopped");
                break;
            }

            savePage(i, indentedUrls.get(i), listSiteStatusCode.get(i), listContent.get(i));
        }
    }


    private Page savePage(int id, String path, int code, String content) {
        try {
            Page page = new Page(id, path, code, content);
            pageRepository.save(page);
            System.out.println(page.toString());
            return page;
        } catch (Exception exp) {
            mainExceptions.error(exp);
            return null;
        }
    }


    private void savingLinkToDataBase(List<String> indentedUrls, List<Integer> listSiteStatusCode, List<String> listContent, Site dbSite) {
        for (int i = 0; i < 10; i++) {                                                              // !!!! 10 поменять на indentedUrls.size()
            if (siteService.isIndexingStopFlag()) {
                siteService.updateStatus(dbSite, SiteStatusType.FAILED);
                siteService.updateErrorMessage(dbSite, "Indexing Stopped");
                break;
            } else {
                siteService.updateStatus(dbSite, SiteStatusType.INDEXING);
            }

            saveLink(i, indentedUrls.get(i), listSiteStatusCode.get(i), listContent.get(i));
        }
    }


    private Link saveLink(int id, String path, Integer code, String content) {
        try {
            Link link = new Link(id, path, code, content);
            linkRepository.save(link);
            System.out.println(link.toString());
            return link;
        } catch (Exception exp) {
            mainExceptions.error(exp);
            return null;
        }
    }


    private void savingFieldToDataBase(List<HashMap<String, String>> listField, Site dbSite) {
        for (int i = 0; i < listField.size(); i += 2) {
            if (siteService.isIndexingStopFlag()) {
                siteService.updateStatus(dbSite, SiteStatusType.FAILED);
                siteService.updateErrorMessage(dbSite, "Indexing Stopped");
                break;
            }

            saveField(i, "title", listField.get(i).get("title"));
            saveField(i, "body", listField.get(i + 1).get("body"));
        }
    }


    private Field saveField(int id, String name, String selector) {
        try {
            float weight = name.equals("title") ? 1 : 0.8F;
            Field field = new Field(id, name, selector, weight);
            fieldRepository.save(field);
            System.out.println(field.toString());
            return field;
        } catch (Exception exp) {
            mainExceptions.error(exp);
            return null;
        }
    }


    private void savingLemmaToDataBase(List<String> listContent, SiteExtractor site,
                                       ArrayList<String> lemmatizeListContent, Set<String> uniqalLemmaKeySet, Site dbSite) throws IOException {
        for (String s : listContent) {
            if (siteService.isIndexingStopFlag()) {
                siteService.updateStatus(dbSite, SiteStatusType.FAILED);
                siteService.updateErrorMessage(dbSite, "Indexing Stopped");
                break;
            }

            HashMap<String, Integer> lemmaMap = LemmaFinder.lemmatize(s);
            uniqalLemmaKeySet.addAll(lemmaMap.keySet());
        }

        for (int i = 0; i <10; i++) {                                                                   // !!!! 10 поменять на uniqalLemmaKeySet.size()
            if (siteService.isIndexingStopFlag()) {
                siteService.updateStatus(dbSite, SiteStatusType.FAILED);
                siteService.updateErrorMessage(dbSite, "Indexing Stopped");
                break;
            }

            String singleLemma = uniqalLemmaKeySet.toArray()[i].toString();
            saveLemma(i, singleLemma, site.getFrequency(lemmatizeListContent, singleLemma));
        }
    }


    private Lemma saveLemma(int id, String lemma, int frequency) {
        try {
            Lemma newLemma = new Lemma(id, lemma, frequency);
            lemmaRepository.save(newLemma);
            lemmaEntityList.add(newLemma);
            System.out.println(newLemma.toString());
            return newLemma;
        } catch (Exception exp) {
            mainExceptions.error(exp);
            return null;
        }
    }


    private void savingIndexToDataBase(List<String> indentedUrls, Set<String> uniqalLemmaKeySet, List<Integer> listSiteStatusCode,
                                       List<String> listContent, SiteExtractor site, ArrayList<String> lemmatizeListContent, Site dbSite) {
        int indexId = 0;
        for (int i = 0; i < 10; i++) {                                                   // !!!! 10 поменять на indentedUrls.size()
            for (int j = 0; j < 5; j++) {                                                // !!!! 5 поменять на uniqalLemmaKeySet.size()
                if (siteService.isIndexingStopFlag()) {
                    siteService.updateStatus(dbSite, SiteStatusType.FAILED);
                    siteService.updateErrorMessage(dbSite, "Indexing Stopped");
                    break;
                }

                indexId++;
                saveIndex(indexId,
                        (savePage(i, indentedUrls.get(i), listSiteStatusCode.get(i), listContent.get(i))),
                        (saveLemma(j,
                                uniqalLemmaKeySet.toArray()[j].toString(),
                                site.getFrequency(lemmatizeListContent,
                                        uniqalLemmaKeySet.toArray()[j].toString()))),
                        site.getRank(site.getFrequency(lemmatizeListContent,
                                uniqalLemmaKeySet.toArray()[j].toString())),
                        listSiteStatusCode.get(i));
            }
        }
    }


    private void saveIndex(int id, Page page, Lemma lemma, float rank, int code) {
        try {
            if (code == 200) {
                Index index = new Index(id, page, lemma, rank);
                indexRepository.save(index);
                System.out.println(index.toString());
            }
        } catch (Exception exp) {
            mainExceptions.error(exp);
        }
    }


    public static String getTitle(String htmlText) {
        return Jsoup.parse(htmlText).title();
    }


    public static String getSnippetInHtml(String htmlText, String searchQuery) {
        Document doc = Jsoup.parse(htmlText);
        String textOfSearchQuery = doc.getElementsContainingOwnText(searchQuery).text();
        String[] queryWords = searchQuery.split("\\s+");

        if (!textOfSearchQuery.isEmpty()) {
            int firstIndexOfSnippet = textOfSearchQuery.indexOf(searchQuery) > 80 ?
                    textOfSearchQuery.indexOf(searchQuery) - 80 : 0;
            int lastIndexOfSnippet = Math.min(firstIndexOfSnippet + searchQuery.length() + 160,
                    textOfSearchQuery.length());

            String firstWordSnippet = textOfSearchQuery.substring(firstIndexOfSnippet, lastIndexOfSnippet)
                    .replaceAll(createSnippetRegex(queryWords[0]), "<b>" + queryWords[0]);

            return firstWordSnippet.replaceAll(createSnippetRegex(queryWords[queryWords.length - 1]),
                    queryWords[queryWords.length - 1] + "</b>");
        } else {
            StringBuilder snippetBuilder = new StringBuilder();

            for (String word : queryWords) {
                String substring = word.substring(0, word.length() - 2);
                String textOfSearchWord = doc.getElementsContainingOwnText(substring).text();
                if (!textOfSearchWord.isEmpty()) {
                    int firstIndexOfSnippet = textOfSearchWord.indexOf(word) > 30 ?
                            textOfSearchWord.indexOf(word) - 30 : 0;
                    int lastIndexOfSnippet = Math.min(firstIndexOfSnippet + word.length() + 80, textOfSearchWord.length() - 1);
                    String snippetPart = textOfSearchWord.substring(firstIndexOfSnippet, lastIndexOfSnippet)
                            .replaceAll(createSnippetRegex(substring), "<b>" + substring + "</b>");
                    snippetBuilder.append(snippetPart);
                    snippetBuilder.append("...");
                }
            }

            return snippetBuilder.toString();
        }
    }


    private static String createSnippetRegex(String word) {
        String firstChar = String.valueOf(word.charAt(0));
        return "(?i)([" + firstChar.toLowerCase(Locale.ROOT) + firstChar.toUpperCase(Locale.ROOT) + "]" +
                word.substring(1) + ")";
    }


    public Object stopIndexing() {
        siteService.setIndexingStarted(false);
        siteService.setIndexingStopFlag(true);
        System.out.println("Indexing Stopped");
        return new ResultResponse();
    }

    public static Connection createConnection(String url) {
        return Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").referrer("http://www.google.com")
                .ignoreHttpErrors(true).ignoreContentType(true);
    }

    public static Elements getElementsByTagA(Connection connection) throws InterruptedException, IOException {
        return connection.get().getElementsByTag("a");
    }

    public static int getResponseCode(Connection connection) throws IOException {
        Connection.Response response = connection.execute();
        return response.statusCode();
    }

    public static String getBodyText(Connection connection) throws IOException {
        return connection.get().body().text();
    }

    public static String getTitleText(Connection connection) throws IOException {
        return connection.get().title();
    }

}

