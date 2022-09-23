package de.citytwin.model;

import de.citytwin.catalog.HasName;

import java.util.ArrayList;
import java.util.List;

/**
 * this class present a simple data object
 *
 * @author Maik Siegmund, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */

public class Term implements HasName {

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
        this.type = type;
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

    @Override
    public String getName() {
        return term;
    }

    /**
     * @return {@code List<String>}
     */
    public List<Ontology> getOntologies() {
        List<Ontology> temps = new ArrayList<Ontology>();
        this.ontologies.forEach(val -> temps.add(new Ontology(val)));
        return temps;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((isCore == null) ? 0 : isCore.hashCode());
        result = prime * result + ((morphem == null) ? 0 : morphem.hashCode());
        result = prime * result + ((term == null) ? 0 : term.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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
        Term other = (Term)obj;
        if (isCore == null) {
            if (other.isCore != null)
                return false;
        } else if (!isCore.equals(other.isCore))
            return false;
        if (morphem == null) {
            if (other.morphem != null)
                return false;
        } else if (!morphem.equals(other.morphem))
            return false;
        if (term == null) {
            if (other.term != null)
                return false;
        } else if (!term.equals(other.term))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

}
