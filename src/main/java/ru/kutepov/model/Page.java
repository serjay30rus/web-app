package ru.kutepov.model;

import javax.persistence.*;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "_page")
@Data
public class Page implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "page_id")
    private int id;
    private String path;                            // Адрес страницы от корня сайта (должен начинаться со слеша);
    private int code;                               // Код ответа, полученный при запросе страницы
    @Column(length = 100_000)
    private String content;                         // Контент страницы (HTML-код).
    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL)
    private List<Index> index;
    @ManyToOne
    @JoinColumn(name = "site_id")
    private Site siteBySiteId;

    public Page() {}

    public Page(int id, String path, int code, String content) {
        this.id = id;
        this.path = path;
        this.code = code;
        this.content = content;
    }


}
