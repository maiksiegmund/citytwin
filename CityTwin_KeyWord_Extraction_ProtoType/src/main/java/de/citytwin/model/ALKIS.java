package de.citytwin.model;

import de.citytwin.catalog.CatalogEntryHasName;

/**
 * this class present a simple data object
 *
 * @author Maik Siegmund, FH Erfurt
 */
public class ALKIS implements CatalogEntryHasName {

    private String name;
    private String categorie;
    private Integer code;
    private String type;

    /**
     * constructor.
     */
    public ALKIS() {
        this.name = "";
        this.categorie = "";
        this.code = 0;
        this.type = "ALKIS";
    }


    /**
     * constructor.
     * @param name
     * @param categorie
     * @param code
     * @param type
     */
    public ALKIS(String name, String categorie, Integer code, String type) {
        super();
        this.name = name;
        this.categorie = categorie;
        this.code = code;
        this.type = type;
    }

    /**
     * @return {@code String}
     */
    public String getCategorie() {
        return categorie;
    }

    /**
     * @return {@code Integer}
     */
    public Integer getCode() {
        return code;
    }

    /**
     * @return {@code String}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @return
     */
    public String getType() {
        return type;
    }

    /**
     * this method set categorie
     *
     * @param categorie {@code String}
     */
    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    /**
     * this method set code
     *
     * @param categorie {@code Integer}
     */
    public void setCode(Integer code) {
        this.code = code;
    }

    /**
     * this method set name
     *
     * @param categorie {@code String}
     */

    public void setName(String name) {
        this.name = name;
    }

    /**
     * this method set type
     *
     * @param categorie {@code String}
     */
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "ALKIS [" + (name != null ? "name=" + name + ", " : "") + (categorie != null ? "categorie=" + categorie + ", " : "")
                + (code != null ? "code=" + code + ", " : "") + (type != null ? "type=" + type : "") + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((categorie == null) ? 0 : categorie.hashCode());
        result = prime * result + ((code == null) ? 0 : code.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        ALKIS other = (ALKIS)obj;
        if (categorie == null) {
            if (other.categorie != null)
                return false;
        } else if (!categorie.equals(other.categorie))
            return false;
        if (code == null) {
            if (other.code != null)
                return false;
        } else if (!code.equals(other.code))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

}
