package de.citytwin.catalog;

/**
 * this class present a simple data object
 *
 * @author Maik Siegmund, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class ALKIS implements CatalogEntryHasName {

    private String name;
    private String categorie;
    private Integer code;
    private String type;

    /**
     * Konstruktor.
     */
    public ALKIS() {
        this.name = "";
        this.categorie = "";
        this.code = 0;
        this.type = "ALKIS";
    }

    /**
     * constructor
     *
     * @param name
     * @param categorie
     * @param code
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

}