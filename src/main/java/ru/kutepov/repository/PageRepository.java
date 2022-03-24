package ru.kutepov.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.kutepov.model.Page;
import org.springframework.stereotype.Repository;
import ru.kutepov.model.Site;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    long countBySiteBySiteId(Site siteBySiteId);

    Optional<Page> findByPathAndSiteBySiteId(String path, Site site);

    @Override
    @Modifying
    @Query("DELETE FROM Page")
    void deleteAll();

}
