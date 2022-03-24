package ru.kutepov.extractor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

public class SiteExtractor extends RecursiveAction {

    private final String USER_AGENT = "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) " +
                                      "Gecko/20070725 Firefox/2.0.0.6";
    private String pageUrl;
    private Set<String> urls;
    private static Logger siteExtractorExceptions = LogManager.getLogger("searchFile");
    List<String> indentedUrls;


    public SiteExtractor(Set<String> urls, String pageUrl) {
        this.pageUrl = pageUrl;
        this.urls = urls;
    }


    public Set<String> getUrls() {
        return urls;
    }


    @Override
    protected void compute() {
        urls.add(pageUrl);                                                       // Добавляем строку-ссылку в коллекцию
        List<SiteExtractor> tasks = new ArrayList<>();                           // Создаём список задач для парсинга дочерних страниц

        for (String childUrl : getChildUrls()) {
            if (!urls.contains(childUrl)) {                                      // Проверяем наличие ссылки в коллекции
                SiteExtractor task = new SiteExtractor(urls, childUrl);          // В случае её отсутстви запускаем задачу для каждой дочерней ссылки
                task.fork();                                                     // Запускаем задачу асинхронно
                tasks.add(task);                                                 // Добавляем задачу в список дочерних страниц
            }
        }
        tasks.forEach(ForkJoinTask::join);                                       // Запускаем задачи в пуле ForkJoinPool
    }


    private Set<String> getChildUrls() {                                         // Парсит текущую страницу и возвращает ссылки на дочерние страницы
        Set<String> urls = new HashSet<>();                                      // Создаём коллекцию URL адресов
        try {
            Document doc = Jsoup                                                 // Создаём переменную для загрузки строк
                    .connect(pageUrl)                                            // Коннектим URL
                    .userAgent(USER_AGENT)
                    .ignoreHttpErrors(true)                                      // Соединение не вызовет ошибку при возврате кода состояния ошибки (404 и т.д.)
                    .ignoreContentType(true)                                     // Игнорируем тип данных
                    .get();                                                      // Получаем данные
            URL baseUrl = new URL(pageUrl);
            urls = doc.select("a").stream()                             // Фильтруем полученные строки
                    .map(e -> getChildUrl(baseUrl, e.attr("href")))
                    .filter(u -> u.startsWith(pageUrl))
                    .collect(Collectors.toSet());
        } catch (Exception ex) {
            siteExtractorExceptions.error("Ошибка парсинга страницы " + pageUrl + ex.getMessage());     // Пишем в лог исключение, если такое выпадает
        }
        return urls;                                                             // Возвращаем результат - ссылки на дочерние страницы
    }


    private String getChildUrl(URL baseUrl, String href) {                       // Возвращает абсолютную ссылку на основе содержимого href, отбрасывает часть ссылки после символа '#' (anchor)
        try {
            String childUrl = new URL(baseUrl, href).toString();
            int anchorIndex = childUrl.indexOf('#');
            if (anchorIndex > 0) {
                childUrl = childUrl.substring(0, anchorIndex);
            }
            return childUrl;
        } catch (MalformedURLException ex) {
            return "";
        }
    }


    public List<Integer> getSiteStatus() throws IOException {                           // Метод для получения кода ответа от запрашиваемой страницы
        List<Integer> statusCodeList = new ArrayList<>();
        for (int i = 0; i < urls.size(); i++) {
            Connection.Response response = Jsoup.connect(StringCase.SOURCE_URL).maxBodySize(0)
                    .userAgent(USER_AGENT)
                    .execute();

            int statusCode = response.statusCode();                                      // Инициализируем код ответа через класс Response
            statusCodeList.add(statusCode);
        }
        return statusCodeList;
    }


    public List<String> getSiteContent() throws IOException {
        List<String> contentList = new ArrayList<>();
        for (String s : urls) {
            Document doc = Jsoup.connect(s).maxBodySize(0)                                // Получаем HTML и снимаем ограничения на полученные данные
                    .userAgent(USER_AGENT)
                    .ignoreHttpErrors(true)                                               // Соединение не вызовет ошибку при возврате кода состояния ошибки (404 и т.д.)
                    .ignoreContentType(true)                                              // Игнорируем тип данных
                    .get();                                                               // Получаем данные
            Elements el = doc.select("html");
            contentList.add(el.text());
        }
        return contentList;
    }


    public List<HashMap<String, String>> getField() throws IOException {
        ArrayList<HashMap<String, String>> list = new ArrayList<>();                      // Создаём список title/body
        for (String s : urls) {
            Document doc = Jsoup.connect(s).maxBodySize(0)                                // Получаем HTML и снимаем ограничения на полученные данные
                    .userAgent(StringCase.SOURCE_URL)
                    .ignoreHttpErrors(true)                                               // Соединение не вызовет ошибку при возврате кода состояния ошибки (404 и т.д.)
                    .ignoreContentType(true)                                              // Игнорируем тип данных
                    .get();                                                               // Получаем данные
            Elements elTitle = doc.select("title");
            Elements elBody = doc.select("body");
            HashMap<String, String> titleMap = new HashMap<>();                           // Создаём новый словарь
            titleMap.put("title", elTitle.text());                                        // кладём в него ключ и значение
            HashMap<String, String> bodyMap = new HashMap<>();                            // Создаём новый словарь
            bodyMap.put("body", elBody.text());                                           // кладём в него ключ и значение
            list.add(titleMap);                                                           // Кладём туда словарь title
            list.add(bodyMap);                                                            // и body
        }
        return list;
    }


    public Integer getFrequency (List<String> listContent, String lemma) {   // Метод для получения частоты лемм
        int frequency = 0;
        for (String s : listContent) {
            String[] lemmaArray = s.replaceAll("[\\W\\w&&[^а-яА-Я\\s]]", "").split("\\s+");
            Set<String> unicalWordsSet = new HashSet<>(Arrays.asList(lemmaArray));
            for (String str : unicalWordsSet) {
                if (str.equals(lemma)) {
                    frequency++;
                }
            }
        }
        return frequency;
    }


    public float getRank(int frequency) {
        return (float) (frequency * (0.8 + 1));
    }
}
