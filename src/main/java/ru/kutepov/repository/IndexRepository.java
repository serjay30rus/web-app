package ru.kutepov.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.kutepov.model.Index;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.kutepov.model.Lemma;
import ru.kutepov.model.dto.interfaces.IndexPageId;
import ru.kutepov.model.dto.interfaces.PageRelevanceAndData;

import java.util.List;

@Repository
public interface IndexRepository extends CrudRepository<Index, Integer> {
    List<IndexPageId> findByLemmaId(int lemmaId);

    List<IndexPageId> findByLemmaIdAndPageIdIn(int lemmaId, List<Integer> pageIds);

    @Query(value = "SELECT _site.url AS site, " +
            "_site.name AS siteName, " +
            "page.path AS uri, " +
            "page.content AS content, " +
            "SUM(lemma_rank)/relrev.maxrev AS relevance " +
            "FROM _index JOIN " +
            "(SELECT MAX(absrev) AS maxrev FROM (SELECT page_id, SUM(lemma_rank) AS absrev FROM _index " +
            "WHERE page_id IN (?1) " +
            "AND lemma_id IN (?2) " +
            "GROUP BY page_id) AS result) AS relrev " +
            "JOIN _page AS page ON _index.page_id = page.id " +
            "JOIN _site ON page.site_id = _site.id " +
            "WHERE page_id IN (?1) " +
            "AND lemma_id IN (?2) " +
            "GROUP BY page_id ORDER BY relevance DESC", nativeQuery = true)
    List<PageRelevanceAndData> findPageRelevanceAndData(List<Integer> pageIds, List<Integer> lemmaIds, Pageable pageable);

    @Override
    @Modifying
    @Query("DELETE FROM Index")
    void deleteAll();
}
