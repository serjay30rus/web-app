package ru.kutepov.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kutepov.model.Page;
import ru.kutepov.model.Site;
import ru.kutepov.repository.PageRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class PageService {
    @Autowired
    private PageRepository pageRepository;

    public Page createPageAndSave(String path, int code, String content, Site site) {
        Page page = new Page();
        page.setPath(path);
        page.setCode(code);
        page.setContent(content);
        page.setSiteBySiteId(site);
        pageRepository.save(page);
        return page;
    }

    @Transactional
    public Optional<Page> getPageByPath(String path, Site site) {
        return pageRepository.findByPathAndSiteBySiteId(path, site);
    }

    @Transactional
    public void deletePage(Page page) {
        pageRepository.delete(page);
    }

    @Transactional
    public void deleteAllPageData(){
        pageRepository.deleteAll();
    }

}
