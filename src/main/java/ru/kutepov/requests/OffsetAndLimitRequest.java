package ru.kutepov.requests;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class OffsetAndLimitRequest implements Pageable {
    private final int LIMIT;
    private final int OFFSET;


    public OffsetAndLimitRequest(int limit, int offset) {
        if (limit < 1) {
            throw new IllegalArgumentException("Лимит не должен быть меньше единицы!!!");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Смещение не должно быть меньше нуля!!!");
        }
        this.LIMIT = limit;
        this.OFFSET = offset;
    }


    @Override
    public int getPageNumber() {
        return OFFSET / LIMIT;
    }


    @Override
    public int getPageSize() {
        return LIMIT;
    }


    @Override
    public long getOffset() {
        return OFFSET;
    }


    @Override
    public Sort getSort() {
        return Sort.unsorted();
    }


    @Override
    public Pageable next() {
        return new OffsetAndLimitRequest(getPageSize(), (int) (getOffset() + getPageSize()));
    }


    public Pageable previous() {
        return hasPrevious() ?
                new OffsetAndLimitRequest(getPageSize(), (int) (getOffset() - getPageSize())) : this;
    }


    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? previous() : first();
    }


    @Override
    public Pageable first() {
        return new OffsetAndLimitRequest(getPageSize(), 0);
    }


    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetAndLimitRequest(getPageSize(), pageNumber);
    }


    @Override
    public boolean hasPrevious() {
        return OFFSET > LIMIT;
    }
}
