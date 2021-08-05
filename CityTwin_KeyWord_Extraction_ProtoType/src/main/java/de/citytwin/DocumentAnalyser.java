package de.citytwin;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Maik Siegmund, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class DocumentAnalyser {

    /** Aktuelle Versionsinformation */
    private static final String VERSION = "$Revision: 1.00 $";
    /** Klassenspezifischer, aktueller Logger (Server: org.apache.log4j.Logger; Client: java.util.logging.Logger) */
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected TextRankAnalyser textRankAnalyser = null;
    protected TFIDFTextAnalyser tfIdfAnalyser = null;
    protected Word2VecAnalyser word2vecAnalyser = null;
    protected DocumentConverter documentConverter = null;
    protected boolean isBuilt = false;
    private BodyContentHandler bodyContentHandler = null;
    private GermanTextProcessing germanTextProcessing = null;
    private List<ALKISDTO> alkisDTOs = new ArrayList<ALKISDTO>();

    private DocumentAnalyser() {
    }

    /**
     * R&uuml;ckgabe der Klasseninformation.
     * <p>
     * Gibt den Klassennamen und die CVS Revisionsnummer zur&uuml;ck.
     * <p>
     *
     * @return Klasseninformation
     */
    @Override
    public String toString() {
        return this.getClass().getName() + " " + VERSION;
    }

    public void analyseDocument(final File file) throws IOException {
        if (!isBuilt) {
            throw new IOException("perform ... build()");
        }

    }

    /**
     * this method deserialized alkis.json file (include as resource)
     *
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    protected void readAlkis() throws JsonParseException, JsonMappingException, IOException {

        ObjectMapper mapper = new ObjectMapper();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("ALKIS.json");
        alkisDTOs = Arrays.asList(mapper.readValue(inputStream, ALKISDTO[].class));

    }

    public void setAlkis(List<DocumentAnalyser.ALKISDTO> alkisDTOs) {
        this.alkisDTOs = alkisDTOs;
    }

    private ALKISDTO getAlkisDTObyName(String name) {
        ALKISDTO dto = alkisDTOs.stream()
                .filter(item -> name.equals(item.getName()))
                .findAny()
                .orElse(null);
        return dto;
    }

    public static class Builder {

        private boolean withOpenNLP = false;
        private boolean withStopwordFilter = false;
        private boolean withStemmening = false;
        private String pathToModel = "";

        public Builder withOpenNLP() {
            this.withOpenNLP = true;
            return this;

        }

        public Builder withLucene() {
            this.withOpenNLP = false;
            return this;
        }

        public Builder withStopwordFilter() {
            withStopwordFilter = true;
            return this;
        }

        public Builder Model(@Nonnull String pathToModel) {
            this.pathToModel = pathToModel;
            return this;
        }

        public DocumentAnalyser build() throws Exception {

            DocumentAnalyser documentAnalyser = new DocumentAnalyser();
            documentAnalyser.textRankAnalyser = (this.withOpenNLP) ? new TextRankAnalyser().withOpenNLP() : new TextRankAnalyser().withOpenNLP();
            documentAnalyser.tfIdfAnalyser = new TFIDFTextAnalyser();
            documentAnalyser.tfIdfAnalyser.setWithOpenNLP(withOpenNLP);
            documentAnalyser.tfIdfAnalyser.setWithStopWordFilter(withStopwordFilter);
            documentAnalyser.tfIdfAnalyser.setWithStemming(withStemmening);
            documentAnalyser.word2vecAnalyser = new Word2VecAnalyser().withModel(pathToModel);
            documentAnalyser.documentConverter = new DocumentConverter();
            documentAnalyser.readAlkis();
            documentAnalyser.isBuilt = true;
            return documentAnalyser;

        }

    }

    /**
     * @author Maik Siegmund, FH Erfurt
     * @version $Revision: 1.0 $
     * @since CityTwin_KeyWord_Extraction_ProtoType 1.0 simple data transfer object (deserialized)
     */
    public static class ALKISDTO {

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

        private String name;
        private String categorie;
        private Integer code;

        public ALKISDTO() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCategorie() {
            return categorie;
        }

        public void setCategorie(String categorie) {
            this.categorie = categorie;
        }

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return "ALKISDTO [" + (name != null ? "name=" + name + ", " : "") + (categorie != null ? "categorie=" + categorie + ", " : "")
                    + (code != null ? "code=" + code : "") + "]";
        }

    }

}
