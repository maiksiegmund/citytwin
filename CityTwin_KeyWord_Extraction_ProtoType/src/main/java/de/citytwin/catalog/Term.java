package de.citytwin.catalog;

import java.util.ArrayList;
import java.util.List;

/**
 * this class present a simple data object
 *
 * @author Maik Siegmund, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class Term implements CatalogEntryHasName {

    private Boolean isCore = false;
    private String morphem = "";
    private String term = "";
    private List<String> ontologies = new ArrayList<String>();

    /**
     * constructor.
     */
    public Term() {
        isCore = false;
        morphem = "";
        term = "";
        ontologies = new ArrayList<String>();
    }

    /**
     * constructor.
     *
     * @param isCore
     * @param morphem
     * @param term
     * @param ontologies
     */
    public Term(Boolean isCore, String morphem, String term, List<String> ontologies) {
        this.isCore = isCore;
        this.morphem = morphem;
        this.term = term;
        this.ontologies.addAll(ontologies);
    }

    /**
     * @return {@code Boolean}
     */
    public Boolean getIsCore() {
        return isCore;
    }

    /**
     * @return {@code String}
     */
    public String getMorphem() {
        return morphem;
    }

    /* (non-Javadoc)
     * @see de.citytwin.catalog.HasName#getName() */
    @Override
    public String getName() {
        return term;
    }

    /**
     * @return {@code List<String>}
     */
    public List<String> getOntologies() {
        return ontologies;
    }

    /**
     * @return {@code String}
     */
    public String getTerm() {
        return term;
    }

    /**
     * @param isCore {@code Boolean}
     */
    public void setIsCore(Boolean isCore) {
        this.isCore = isCore;
    }

    /**
     * @param isCore {@code String}
     */
    public void setMorphem(String morphem) {
        this.morphem = morphem;
    }

    /**
     * @param isCore {@code List<String>}
     */
    public void setOntologies(List<String> ontologies) {
        this.ontologies = ontologies;
    }

    /**
     * @param isCore {@code String}
     */
    public void setTerm(String term) {
        this.term = term;
    }

    @Override
    public String toString() {
        return "Term [isCore=" + isCore + ", morphem=" + morphem + ", term=" + term + ", ontologies=" + ontologies + "]";
    }

}
