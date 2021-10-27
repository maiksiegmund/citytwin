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
    private String type = "";

    /**
     * constructor.
     */
    public Term() {
        isCore = false;
        morphem = "";
        term = "";
        ontologies = new ArrayList<String>();
        type = "Term";
    }

    /**
     * constructor.
     *
     * @param isCore
     * @param morphem
     * @param term
     * @param ontologies
     */
    public Term(Boolean isCore, String morphem, String term, List<String> ontologies, String type) {
        this.isCore = isCore;
        this.morphem = morphem;
        this.term = term;
        this.ontologies.addAll(ontologies);
        this.type = type;
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
     * @return
     */
    public String getType() {
        return type;
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

    /**
     * @param type
     */
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Term [" + (isCore != null ? "isCore=" + isCore + ", " : "") + (morphem != null ? "morphem=" + morphem + ", " : "")
                + (term != null ? "term=" + term + ", " : "") + (ontologies != null ? "ontologies=" + ontologies + ", " : "")
                + (type != null ? "type=" + type : "") + "]";
    }

}
