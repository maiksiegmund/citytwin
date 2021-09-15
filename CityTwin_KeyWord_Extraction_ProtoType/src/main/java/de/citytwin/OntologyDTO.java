package de.citytwin;

/**
 * data transfer object (deserialized)
 *
 * @author Maik, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class OntologyDTO {

    private boolean isSemantic = false;
    private boolean isKeyWord = false;
    private boolean isCore = false;
    private String stemm = "";
    private String type = "";
    private String word = "";

    /**
     * Konstruktor.
     */
    public OntologyDTO() {
        this.isSemantic = false;
        this.isKeyWord = false;
        this.isCore = false;
        this.stemm = "";
        this.type = "";
        this.word = "";
    }

    public OntologyDTO(boolean isSemantic, boolean isKeyWord, boolean isCore, String stem, String type, String word) {
        this.isSemantic = isSemantic;
        this.isKeyWord = isKeyWord;
        this.isCore = isCore;
        this.stemm = stem;
        this.type = type;
        this.word = word;
    }

    public String getStemm() {
        return stemm;
    }

    public String getType() {
        return type;
    }

    public String getWord() {
        return word;
    }

    public boolean isCore() {
        return isCore;
    }

    public boolean isKeyWord() {
        return isKeyWord;
    }

    public boolean isSemantic() {
        return isSemantic;
    }

    public void setCore(boolean isCore) {
        this.isCore = isCore;
    }

    public void setKeyWord(boolean isKeyWord) {
        this.isKeyWord = isKeyWord;
    }

    public void setSemantic(boolean isSemantic) {
        this.isSemantic = isSemantic;
    }

    public void setStem(String stem) {
        this.stemm = stem;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setWord(String word) {
        this.word = word;
    }

    @Override
    public String toString() {
        return "OntologyDTO [isSemantic=" + isSemantic + ", isKeyWord=" + isKeyWord + ", isCore=" + isCore + ", stemm=" + stemm + ", type=" + type
                + ", word=" + word + "]";
    }

}
