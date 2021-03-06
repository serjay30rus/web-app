package ru.kutepov.model;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;

@Entity
@Data
@Table(name = "_site")
public class Site implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "site_id")
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "enum('INDEXING', 'INDEXED', 'FAILED')")
    private SiteStatusType status;

    @Column(name = "status_time")
    private Timestamp statusTime;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "url")
    private String url;

    @Column(name = "name")
    private String name;

    @OneToMany(mappedBy = "siteBySiteId")
    private Collection<Lemma> lemmataById;

    @OneToMany(mappedBy = "siteBySiteId")
    private Collection<Page> pagesById;


    public Site(int id, SiteStatusType status, Timestamp statusTime, String lastError, String url, String name) {
        this.id = id;
        this.status = status;
        this.statusTime = statusTime;
        this.lastError = lastError;
        this.url = url;
        this.name = name;
    }

    public Site() {
    }



}
