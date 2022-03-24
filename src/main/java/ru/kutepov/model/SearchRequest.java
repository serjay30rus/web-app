package ru.kutepov.model;

public class SearchRequest {
    private String query;
    private String site;
    private int offset;
    private int limit;


    public SearchRequest(){}


    public SearchRequest(String query, String site, int offset, int limit) {
        this.query = query;
        this.site = site;
        this.offset = offset;
        this.limit = limit;
    }


    public String getQuery() {
        return query;
    }


    public String getSite() {
        return site;
    }


    public int getOffset() {
        return offset;
    }


    public int getLimit() {
        return limit;
    }


    public void setQuery(String query) {
        this.query = query;
    }

}
