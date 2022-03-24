package ru.kutepov.model;

import lombok.Data;
import javax.persistence.*;

@Entity
@Table(name = "_link")
@Data
public class Link {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String path;
    private int code;
    @Column(length = 100_000)
    private String content;

    public Link() {
    }

    public Link(int id, String path, int code, String content) {
        this.id = id;
        this.path = path;
        this.code = code;
        this.content = content;
    }
}
