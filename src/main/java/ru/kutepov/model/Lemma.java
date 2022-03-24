package ru.kutepov.model;

import javax.persistence.*;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "_lemma")
@Data
public class Lemma implements Serializable, Comparable<Lemma> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lemma_id")
    private int id;
    @Column(length = 100_000)                            // Увеличиваем длину колонки
    private String lemma;                              // Нормальная форма слова;
    private int frequency;                             // Количество страниц, на которых слово встречается хотя бы один раз.
    @OneToMany(mappedBy = "lemma", cascade = CascadeType.ALL)
    private List<Index> index;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site siteBySiteId;

    public Lemma() {}

    public Lemma(int id, String lemma, int frequency) {
        this.id = id;
        this.lemma = lemma;
        this.frequency = frequency;
    }


    public Lemma(String lemma, int frequency, Site siteBySiteId) {
        this.lemma = lemma;
        this.frequency = frequency;
        this.siteBySiteId = siteBySiteId;
    }

    @Override
    public int compareTo(Lemma lemma) {
        return this.getFrequency() - lemma.getFrequency();
    }
}