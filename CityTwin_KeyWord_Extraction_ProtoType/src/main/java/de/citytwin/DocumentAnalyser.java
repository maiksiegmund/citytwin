package de.citytwin;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.tika.sax.BodyContentHandler;
import org.javatuples.Quartet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Maik Siegmund, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class DocumentAnalyser {

    /**
     * @author Maik Siegmund, FH Erfurt
     * @version $Revision: 1.0 $
     * @since CityTwin_KeyWord_Extraction_ProtoType 1.0 simple data transfer object (deserialized)
     */
    public static class ALKISDTO {

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

    public static class Builder {

        private boolean withOpenNLP = false;
        private boolean withStopwordFilter = false;
        private boolean withStemmening = false;
        private String pathToModel = "";

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
            documentAnalyser.readOntology();
            documentAnalyser.isBuilt = true;
            return documentAnalyser;

        }

        public Builder Model(@Nonnull String pathToModel) {
            this.pathToModel = pathToModel;
            return this;
        }

        public Builder withLucene() {
            this.withOpenNLP = false;
            return this;
        }

        public Builder withOpenNLP() {
            this.withOpenNLP = true;
            return this;

        }

        public Builder withStopwordFilter() {
            withStopwordFilter = true;
            return this;
        }

    }

    /**
     * @author Maik, FH Erfurt
     * @version $Revision: 1.0 $
     * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
     */
    public static class OntologyDTO {

        private boolean isSemantic = false;
        private boolean isKeyWord = false;
        private boolean isCore = false;
        private String stem = "";
        private String type = "";
        private String word = "";

        /**
         * Konstruktor.
         */
        public OntologyDTO() {
            this.isSemantic = false;
            this.isKeyWord = false;
            this.isCore = false;
            this.stem = "";
            this.type = "";
            this.word = "";
        }

        public OntologyDTO(boolean isSemantic, boolean isKeyWord, boolean isCore, String stem, String type, String word) {
            this.isSemantic = isSemantic;
            this.isKeyWord = isKeyWord;
            this.isCore = isCore;
            this.stem = stem;
            this.type = type;
            this.word = word;
        }

        public String getStem() {
            return stem;
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
            this.stem = stem;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setWord(String word) {
            this.word = word;
        }

        @Override
        public String toString() {
            return "OntologyDTO [isSemantic=" + isSemantic + ", isKeyWord=" + isKeyWord + ", isCore=" + isCore + ", stem=" + stem + ", type=" + type
                    + ", word=" + word + "]";
        }

    }

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
    private List<OntologyDTO> ontologyDTOs = new ArrayList<OntologyDTO>();

    private int textRankWordWindowSize = 5;
    private int textRankIteration = 5;
    private int textRankMaxLinks = 3;

    private Map<String, Quartet<Integer, Double, String, Set<Integer>>> tfIDFResults = null;

    private Map<String, Double> textRankResults = null;

    private Map<String, Map<String, List<String>>> textRankLinkResults = null;

    private DocumentAnalyser() {
    }

    public void analyse() throws IOException {
        if (!isBuilt) {
            throw new IOException("perform ... performKeyWordExtraction(...)");
        }

        logger.info("seeking in and outbound terms");
        textRankLinkResults = textRankAnalyser.getLinkedTerms(textRankResults, textRankMaxLinks);
        logger.info("finished");

    }

    public boolean isTermInALKIS(String term) {
        List<Pair<String, String>> stemmed = germanTextProcessing.stemm(Arrays.asList(term));

        Pair<String, String> pair = stemmed.get(0);
        ALKISDTO dto = alkisDTOs.stream()
                .filter(item -> pair.getLeft().equals(item.name) || pair.getRight().equals(item.name))
                .findFirst()
                .orElse(null);
        return (dto == null);
    }

    public boolean isTermInOntology(String term) {

        List<Pair<String, String>> stemmed = germanTextProcessing.stemm(Arrays.asList(term));

        Pair<String, String> pair = stemmed.get(0);
        OntologyDTO dto = ontologyDTOs.stream()
                .filter(item -> pair.getLeft().equals(item.word) || pair.getRight().equals(item.stem))
                .findFirst()
                .orElse(null);

        return (dto == null);

    }

    public Map<String, Double> filterKeyWords(Map<String, Double> keywords, double accuracy){
        Map<String, Double> result = new HashMap<String, Double>();
        word2vecAnalyser.accuracy(null);
        word2vecAnalyser.wordNearest(null, null, textRankIteration);
        return result;


    }

    public void performKeyWordExtraction(final File file) throws IOException {
        if (!isBuilt) {
            throw new IOException("perform ... build()");
        }
        logger.info("run tf idf");
        tfIDFResults = tfIdfAnalyser
                .getTermsAndScores(bodyContentHandler, GermanTextProcessing.getPosTagList(), TFIDFTextAnalyser.NormalizationType.NONE);
        logger.info(MessageFormat.format("tf idf founded {0} keywords", tfIDFResults.size()));

        logger.info("run text rank");
        textRankResults = textRankAnalyser.getTermsAndScores(bodyContentHandler, textRankWordWindowSize, textRankIteration);
        logger.info(MessageFormat.format("text rank founded {0} keywords", textRankResults.size()));

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

    protected void readOntology() throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("ontology.json");
        ontologyDTOs = Arrays.asList(mapper.readValue(inputStream, OntologyDTO[].class));
    }

    public void setAlkis(List<ALKISDTO> alkisDTOs) {
        this.alkisDTOs = new ArrayList<ALKISDTO>(alkisDTOs);
    }

    public void setOntology(List<OntologyDTO> ontologyDTOs) {
        this.ontologyDTOs = new ArrayList<OntologyDTO>(ontologyDTOs);
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

}
