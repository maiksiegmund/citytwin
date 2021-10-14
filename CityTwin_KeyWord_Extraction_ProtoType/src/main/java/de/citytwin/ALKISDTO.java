package de.citytwin;

/**
 * //TODO Javadoc! Consider renaming without 'DTO'.
 * 
 * data transfer object (deserialized)
 *
 * @author Maik Siegmund, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class ALKISDTO implements DTO {

    private String name;
    private String categorie;
    private Integer code;

    public ALKISDTO() {
    }

    /**
     * Konstruktor.
     *
     * @param name
     * @param categorie
     * @param code
     */
    public ALKISDTO(String name, String categorie, Integer code) {
        super();
        this.name = name;
        this.categorie = categorie;
        this.code = code;
    }

    public String getCategorie() {
        return categorie;
    }

    public Integer getCode() {
        return code;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "ALKISDTO [" + (name != null ? "name=" + name + ", " : "") + (categorie != null ? "categorie=" + categorie + ", " : "")
                + (code != null ? "code=" + code : "") + "]";
    }

}
