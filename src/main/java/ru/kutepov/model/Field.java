package ru.kutepov.model;

import javax.persistence.*;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "_field")
@Data
public class Field implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "field_id")
    private int id;
    private String name;                                 // Имя поля;
    @Column(length = 100_000)                            // Увеличиваем длину колонки
    private String selector;                             // CSS-выражение, позволяющее получить содержимое конкретного поля;
    private float weight;                                // Релевантность (вес) поля от 0 до 1.

    public Field() {}

    public Field(String name, String selector, float weight) {
        this.name = name;
        this.selector = selector;
        this.weight = weight;
    }

    public Field(int id, String name, String selector, float weight) {
        this.id = id;
        this.name = name;
        this.selector = selector;
        this.weight = weight;
    }


}
