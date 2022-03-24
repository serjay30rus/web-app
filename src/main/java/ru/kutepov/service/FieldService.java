package ru.kutepov.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kutepov.model.Field;
import ru.kutepov.repository.FieldRepository;

import java.util.Optional;

@Service
public class FieldService {
    @Autowired
    private FieldRepository fieldRepository;

    @Transactional
    public Optional<Field> getById(int id) {
        Optional<Field> fieldOptional = fieldRepository.findById(id);
        return fieldOptional;
    }
}
