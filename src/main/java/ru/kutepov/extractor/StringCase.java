package ru.kutepov.extractor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "application-url.sites")
@Configuration
public class StringCase {
    public static String SOURCE_URL = "https://dombulgakova.ru";

    public String sourceUrl;
    public String siteName;

    public StringCase(String sourceUrl, String siteName) {
        this.sourceUrl = sourceUrl;
        this.siteName = siteName;
    }

    public StringCase() {}

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }
}
