package de.citytwin;

import java.util.ArrayList;
import java.util.List;

public class TermDTO implements DTO {

    private Boolean isCore = false;
    private String morphem = "";
    private String term = "";
    private List<String> ontologies = new ArrayList<String>();

    public TermDTO() {
        isCore = false;
        morphem = "";
        term = "";
        ontologies = new ArrayList<String>();
    }

    public TermDTO(Boolean isCore, String morphem, String term, List<String> ontologies) {
        this.isCore = isCore;
        this.morphem = morphem;
        this.term = term;
        this.ontologies.addAll(ontologies);
    }

    public Boolean getIsCore() {
        return isCore;
    }

    public String getMorphem() {
        return morphem;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return term;
    }

    public List<String> getOntologies() {
        return ontologies;
    }

    public String getTerm() {
        return term;
    }

    public void setIsCore(Boolean isCore) {
        this.isCore = isCore;
    }

    public void setMorphem(String morphem) {
        this.morphem = morphem;
    }

    public void setOntologies(List<String> ontologies) {
        this.ontologies = ontologies;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    @Override
    public String toString() {
        return "TermDTO [isCore=" + isCore + ", morphem=" + morphem + ", term=" + term + ", ontologies=" + ontologies + "]";
    }

}
