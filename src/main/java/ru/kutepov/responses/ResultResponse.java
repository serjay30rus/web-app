package ru.kutepov.responses;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
public class ResultResponse {
    private String result = "true";
}
