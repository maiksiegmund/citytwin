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

import org.apache.commons.lang3.tuple.Pair;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * @author Maik Siegmund, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class DocumentAnalyser implements AutoCloseable {

    public static class Builder {

        @SuppressWarnings("resource")
        public DocumentAnalyser build() throws Exception {

            DocumentAnalyser documentAnalyser = new DocumentAnalyser();
            documentAnalyser.textRankAnalyser = new TextRankAnalyser();
            documentAnalyser.tfIdfAnalyser = new TFIDFTextAnalyser();
            documentAnalyser.word2vecAnalyser = new Word2VecAnalyser().withModel(Config.WORD2VEC_MODEL);
            documentAnalyser.documentConverter = new DocumentConverter();
            documentAnalyser.alkisDTOs = documentAnalyser.documentConverter.getDTOs(new TypeReference<List<ALKISDTO>>() {},
                    Config.ALKIS_RESOURCE);
            documentAnalyser.ontologyDTOs = documentAnalyser.documentConverter.getDTOs(new TypeReference<List<OntologyDTO>>() {},
                    Config.ONTOLOGY_RESOURCE);

            documentAnalyser.isBuilt = true;
            return documentAnalyser;

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

    // private int word2vecAnalyserCount = 10;
    // intern use (store keywords)
    private Map<String, Double> tfIDFResults = null;
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
        textRankLinkResults = textRankAnalyser.getLinkedTerms(textRankResults, Config.TEXTRANK_MAXLINKS);
        logger.info(MessageFormat.format("founded overall {0}", textRankLinkResults.size()));

        Map<String, Pair<ALKISDTO, Double>> filteredKeyWordsAlkis = filterTextKeywordsBySimilarity(ALKISDTO.class, alkisDTOs);
        Map<String, Pair<OntologyDTO, Double>> filteredKeyWordsOntology = filterTextKeywordsBySimilarity(OntologyDTO.class, ontologyDTOs);

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

        DBController dbController = new DBController(Config.DATABASE_URI, Config.DATABASE_USER, Config.DATABASE_PASSWORD);

        dbController.persist(filteredKeyWordsAlkis, metadata);
        dbController.persist(null, metadata);

    }

    @Override
    public void close() throws Exception {
        word2vecAnalyser.close();

    }

    public <T> Map<String, Pair<T, Double>> filterTextKeywordsBySimilarity(Class<? extends DTO> clazz, List<T> dtos) {
        Map<String, Pair<T, Double>> result = new HashMap<String, Pair<T, Double>>();
        logger.info("filter keywords " + clazz.getClass().getName());
        double currentSimilarity = 0.0f;
        double similarity = Config.WORD2VEC_SIMILARITY / 100.00d;
        for (T dto : dtos) {
            for (String key : textRankResults.keySet()) {
                currentSimilarity = word2vecAnalyser.similarity(((DTO)dto).getName(), key);
                // logger.info(MessageFormat.format("term:{0} = {1} | similarity {2}", key, ontologyDTO.word, currentSimilarity));
                if (currentSimilarity > similarity) {
                    result.put(key, Pair.of(dto, textRankResults.get(key)));
                }
            }
            for (String key : tfIDFResults.keySet()) {
                currentSimilarity = word2vecAnalyser.similarity(((DTO)dto).getName(), key);
                // logger.info(MessageFormat.format("term:{0} = {1} | similarity {2}", key, ontologyDTO.word, currentSimilarity));
                if (currentSimilarity > similarity) {
                    result.put(key, Pair.of(dto, tfIDFResults.get(key)));
                }
            }

        }
        return result;
    }

    // public Map<String, Pair<ALKISDTO, Double>> filterTextKeywordsBySimilarityALKISDTO() {
    // Map<String, Pair<ALKISDTO, Double>> result = new HashMap<String, Pair<ALKISDTO, Double>>();
    // logger.info("filter keywords (ALKIS)");
    // double currentSimilarity = 0.0f;
    // double similarity = Config.WORD2VEC_SIMILARITY / 100.00d;
    //
    // for (ALKISDTO alkisdto : alkisDTOs) {
    // for (String key : textRankResults.keySet()) {
    // currentSimilarity = word2vecAnalyser.similarity(alkisdto.getName(), key);
    // // logger.info(MessageFormat.format("term:{0} = {1} | similarity {2}", key, alkisdto.name, currentSimilarity));
    // if (currentSimilarity > similarity) {
    // result.put(key, Pair.of(alkisdto, textRankResults.get(key)));
    // }
    // }
    // for (String key : tfIDFResults.keySet()) {
    // currentSimilarity = word2vecAnalyser.similarity(alkisdto.getName(), key);
    // // logger.info(MessageFormat.format("term:{0} = {1} | similarity {2}", key, alkisdto.name, currentSimilarity));
    // if (currentSimilarity > similarity) {
    // result.put(key, Pair.of(alkisdto, tfIDFResults.get(key)));
    // }
    // }
    // }
    // return result;
    // }

    /**
     * this method get near
     *
     * @param filterdWords
     */
    public Map<String, List<String>> getNearestTo(List<String> filterdTerms) {

        Map<String, List<String>> result = new HashMap<String, List<String>>(filterdTerms.size());
        for (String term : filterdTerms) {
            List<String> nearestWords = word2vecAnalyser.wordsNearest(term, Config.WORD2VEC_NEARESTCOUNT);
            result.put(term, filterdTerms);

        }
        // todo filter!
        return result;

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

    public void performKeyWordExtraction(final File file) throws SAXException, TikaException, Exception {
        if (!isBuilt) {
            throw new IOException("perform ... build()");
        }

        bodyContentHandler = documentConverter.getBodyContentHandler(file);
        metadata = documentConverter.getMetaData(file);

        logger.info("run tf idf");
        tfIDFResults = tfIdfAnalyser
                .getTermsAndScores(bodyContentHandler);
        logger.info(MessageFormat.format("tf idf founded {0} keywords", tfIDFResults.size()));

        logger.info("run text rank");
        textRankResults = textRankAnalyser.getTermsAndScores(bodyContentHandler);
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
