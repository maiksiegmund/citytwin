package de.citytwin;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.io.IOException;
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
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.sax.BodyContentHandler;
import org.javatuples.Quartet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * @author Maik Siegmund, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class DocumentAnalyser {

    private static String ALKIS_RESOURCE = "alkis.json";
    private static String ONTOLOGY_RESOURCE = "ontology.json";
    private static String URI = "bolt://localhost:7687";
    private static String USER = "neo4j";
    private static String PASSWORD = "C1tyTw1n!";

    public static class Builder {

        private boolean withOpenNLP = false;
        private boolean withStopwordFilter = false;
        private boolean withStemmening = false;
        private String pathToModel = "";

        public DocumentAnalyser build() throws Exception {

            DocumentAnalyser documentAnalyser = new DocumentAnalyser();
            documentAnalyser.textRankAnalyser = (this.withOpenNLP) ? new TextRankAnalyser().withOpenNLP() : new TextRankAnalyser().withLucene();
            documentAnalyser.tfIdfAnalyser = new TFIDFTextAnalyser();
            documentAnalyser.tfIdfAnalyser.setWithOpenNLP(withOpenNLP);
            documentAnalyser.tfIdfAnalyser.setWithStopWordFilter(withStopwordFilter);
            documentAnalyser.tfIdfAnalyser.setWithStemming(withStemmening);
            documentAnalyser.word2vecAnalyser = new Word2VecAnalyser().withModel(pathToModel);
            documentAnalyser.documentConverter = new DocumentConverter();
            documentAnalyser.alkisDTOs = documentAnalyser.documentConverter.getDTOs(new TypeReference<List<ALKISDTO>>() {},
                    DocumentAnalyser.ALKIS_RESOURCE);
            documentAnalyser.ontologyDTOs = documentAnalyser.documentConverter.getDTOs(new TypeReference<List<OntologyDTO>>() {},
                    DocumentAnalyser.ONTOLOGY_RESOURCE);

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
    private Metadata metadata = null;
    private GermanTextProcessing germanTextProcessing = null;
    protected List<ALKISDTO> alkisDTOs = new ArrayList<ALKISDTO>();
    protected List<OntologyDTO> ontologyDTOs = new ArrayList<OntologyDTO>();

    private int textRankWordWindowSize = 5;
    private int textRankIteration = 5;
    private int textRankMaxLinks = 3;

    private int word2vecAnalyserCount = 10;
    // intern use (store keywords)
    private Map<String, Quartet<Integer, Double, String, Set<Integer>>> tfIDFResults = null;
    // intern use (store keywords)
    private Map<String, Double> textRankResults = null;

    private Map<String, Map<String, List<String>>> textRankLinkResults = null;

    private DocumentAnalyser() {
    }

    public void analyse(double similarity) throws IOException {
        if (!isBuilt) {
            throw new IOException("perform ... performKeyWordExtraction(...)");
        }

        logger.info("seeking in- and outbound terms");
        textRankLinkResults = textRankAnalyser.getLinkedTerms(textRankResults, textRankMaxLinks);
        logger.info(MessageFormat.format("founded overall {0}", textRankLinkResults.size()));

        Map<String, Pair<ALKISDTO, Double>> filteredKeyWordsAlkis = filterTextKeywordsBySimilarityALKISDTO(similarity);
        Map<String, Pair<OntologyDTO, Double>> filteredKeyWordsOntology = filterTextKeywordsBySimilarityOntologyDTO(similarity);

        Pair<ALKISDTO, Double> keyWordalkisDTO = null;
        Pair<OntologyDTO, Double> keyWordOntologyDTO = null;

        for (String key : filteredKeyWordsAlkis.keySet()) {
            keyWordalkisDTO = filteredKeyWordsAlkis.get(key);
            System.out.print(key + " --> ");
            System.out.println(keyWordalkisDTO.getLeft().toString());
        }

        for (String key : filteredKeyWordsOntology.keySet()) {
            keyWordOntologyDTO = filteredKeyWordsOntology.get(key);
            System.out.print(key + " --> ");
            System.out.println(keyWordOntologyDTO.getLeft().toString());
        }

        logger.info(MessageFormat.format("founded ALKIS:{0}, founded ONTOLOGY: {1}, overall: {2} ",
                filteredKeyWordsAlkis.size(),
                filteredKeyWordsOntology.size(),
                (filteredKeyWordsAlkis.size() + filteredKeyWordsOntology.size())));

        DBController dbController = new DBController(DocumentAnalyser.URI, DocumentAnalyser.USER, DocumentAnalyser.PASSWORD);

        dbController.persist(filteredKeyWordsAlkis, metadata);

    }

    public ALKISDTO getTermInALKIS(String term) {
        List<Pair<String, String>> stemmed = germanTextProcessing.stemm(Arrays.asList(term));

        Pair<String, String> pair = stemmed.get(0);
        ALKISDTO dto = alkisDTOs.stream()
                .filter(item -> pair.getLeft().equals(item.getName()) || pair.getRight().equals(item.getName()))
                .findFirst()
                .orElse(null);
        return dto;
    }

    public OntologyDTO getTermInOntology(String term) {

        List<Pair<String, String>> stemmed = germanTextProcessing.stemm(Arrays.asList(term));

        Pair<String, String> pair = stemmed.get(0);
        OntologyDTO dto = ontologyDTOs.stream()
                .filter(item -> pair.getLeft().equals(item.getWord()) || pair.getRight().equals(item.getStemm()))
                .findFirst()
                .orElse(null);

        return dto;

    }

    public Map<String, Pair<ALKISDTO, Double>> filterTextKeywordsBySimilarityALKISDTO(double similarity) {
        Map<String, Pair<ALKISDTO, Double>> result = new HashMap<String, Pair<ALKISDTO, Double>>();
        logger.info("filter keywords (ALKIS)");
        double currentSimilarity = 0.0f;

        for (ALKISDTO alkisdto : alkisDTOs) {
            for (String key : textRankResults.keySet()) {
                currentSimilarity = word2vecAnalyser.similarity(alkisdto.getName(), key);
                // logger.info(MessageFormat.format("term:{0} = {1} | similarity {2}", key, alkisdto.name, currentSimilarity));
                if (currentSimilarity > similarity) {
                    result.put(key, Pair.of(alkisdto, textRankResults.get(key)));
                }
            }
            for (String key : tfIDFResults.keySet()) {
                currentSimilarity = word2vecAnalyser.similarity(alkisdto.getName(), key);
                // logger.info(MessageFormat.format("term:{0} = {1} | similarity {2}", key, alkisdto.name, currentSimilarity));
                if (currentSimilarity > similarity) {
                    result.put(key, Pair.of(alkisdto, tfIDFResults.get(key).getValue1()));
                }
            }
        }
        return result;
    }

    public Map<String, Pair<OntologyDTO, Double>> filterTextKeywordsBySimilarityOntologyDTO(double similarity) {
        Map<String, Pair<OntologyDTO, Double>> result = new HashMap<String, Pair<OntologyDTO, Double>>();
        logger.info("filter keywords (Ontology)");
        double currentSimilarity = 0.0f;

        for (OntologyDTO ontologyDTO : ontologyDTOs) {
            for (String key : textRankResults.keySet()) {
                currentSimilarity = word2vecAnalyser.similarity(ontologyDTO.getWord(), key);
                // logger.info(MessageFormat.format("term:{0} = {1} | similarity {2}", key, ontologyDTO.word, currentSimilarity));
                if (currentSimilarity > similarity) {
                    result.put(key, Pair.of(ontologyDTO, textRankResults.get(key)));
                }
            }
            for (String key : tfIDFResults.keySet()) {
                currentSimilarity = word2vecAnalyser.similarity(ontologyDTO.getWord(), key);
                // logger.info(MessageFormat.format("term:{0} = {1} | similarity {2}", key, ontologyDTO.word, currentSimilarity));
                if (currentSimilarity > similarity) {
                    result.put(key, Pair.of(ontologyDTO, tfIDFResults.get(key).getValue1()));
                }
            }

        }
        return result;
    }

    public void performKeyWordExtraction(final File file) throws SAXException, TikaException, Exception {
        if (!isBuilt) {
            throw new IOException("perform ... build()");
        }

        bodyContentHandler = documentConverter.getBodyContentHandler(file);
        metadata = documentConverter.getMetaData(file);

        logger.info("run tf idf");
        tfIDFResults = tfIdfAnalyser
                .getTermsAndScores(bodyContentHandler, GermanTextProcessing.getPosTagList(), TFIDFTextAnalyser.NormalizationType.NONE);
        logger.info(MessageFormat.format("tf idf founded {0} keywords", tfIDFResults.size()));

        logger.info("run text rank");
        textRankResults = textRankAnalyser.getTermsAndScores(bodyContentHandler, textRankWordWindowSize, textRankIteration);
        logger.info(MessageFormat.format("text rank founded {0} keywords", textRankResults.size()));

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
