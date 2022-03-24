package ru.kutepov.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.kutepov.config.SitesConfig;
import ru.kutepov.model.Site;

import java.util.List;

@Service
public class FileTypeService {
    private final List<Site> fileTypes;

    @Autowired
    public FileTypeService(SitesConfig sitesConfig){
        this.fileTypes = sitesConfig.getFileTypes();
    }

    public List<Site> getFileTypes(){
        return this.fileTypes;
    }


}
