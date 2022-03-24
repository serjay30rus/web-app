package ru.kutepov.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import ru.kutepov.model.Site;

import java.util.List;

@Component
@Configuration
@ConfigurationProperties(prefix = "sites")
@Getter
@Setter
public class SitesConfig {
    private List<Site> fileTypes;
}
