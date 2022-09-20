package de.citytwin.model;

import de.citytwin.catalog.HasName;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Keyword implements HasName {

    private String keyword = "";
    private Double weight = 0.0d;
    private Long id = 0L;

    @SuppressWarnings("unused")
    private Keyword() {

    }

    public Keyword(String keyword) {
        this.keyword = keyword;
        this.weight = 0.0d;
        this.id = 0L;
    }

    public Keyword(String keyword, Double weight) {
        this.keyword = keyword;
        this.weight = weight;
        this.id = 0L;
    }

    public Keyword(String keyword, Double weight, Long id) {
        this.keyword = keyword;
        this.weight = weight;
        this.id = id;
    }

    @Override
    public String getName() {
        return this.keyword;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((keyword == null) ? 0 : keyword.hashCode());
        result = prime * result + ((weight == null) ? 0 : weight.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Keyword other = (Keyword)obj;
        if (keyword == null) {
            if (other.keyword != null)
                return false;
        } else if (!keyword.equals(other.keyword))
            return false;
        if (weight == null) {
            if (other.weight != null)
                return false;
        } else if (!weight.equals(other.weight))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Keyword [keyword=" + keyword + ", weight=" + weight + "]";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;

    }

    public static Set<Keyword> toSet(Map<String, Double> keywords) {
        Set<Keyword> results = new HashSet<Keyword>(keywords.size());
        keywords.forEach((keyword, weight) -> results.add(new Keyword(keyword, weight)));
        return results;
    }

}
