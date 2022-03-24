package ru.kutepov.model;
import javax.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Entity
@Table(name = "_index")
@Data
public class Index implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id")
    private Page page;                                             // Идентификатор страницы;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id")
    private Lemma lemma;                                            // Идентификатор леммы;
    @Column(name = "lemma_rank")
    private float rank;                                             // Ранг леммы в данном поле этой страницы



    public Index() {
    }

    public Index(int id, Page page, Lemma lemma, float rank) {
        this.id = id;
        this.page = page;
        this.lemma = lemma;
        this.rank = rank;
    }
}
