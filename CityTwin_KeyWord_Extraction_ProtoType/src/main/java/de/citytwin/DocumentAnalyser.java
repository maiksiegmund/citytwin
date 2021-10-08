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
public class DocumentAnalyser {

    public static class Builder {

        public DocumentAnalyser build() throws Exception {
            DocumentAnalyser documentAnalyser = new DocumentAnalyser();
            documentAnalyser.textRankAnalyser = new TextRankAnalyser();
            documentAnalyser.tfIdfAnalyser = new TFIDFTextAnalyser();
            documentAnalyser.word2vecAnalyser = new Word2VecAnalyser().withModel(Config.WORD2VEC_MODEL);
            documentAnalyser.documentConverter = new DocumentConverter();
            documentAnalyser.alkisDTOs = documentAnalyser.documentConverter.getDTOs(new TypeReference<List<ALKISDTO>>() {},
                    Config.ALKIS_RESOURCE);
            documentAnalyser.termDTOs = documentAnalyser.documentConverter.getDTOs(new TypeReference<List<TermDTO>>() {}, Config.TERM_RESOURCE);

            documentAnalyser.isBuilt = true;

            return documentAnalyser;
        }

    }

    /** Aktuelle Versionsinformation */
    private static final String VERSION = "$Revision: 1.00 $";

    /** Klassenspezifischer, aktueller Logger (Server: org.apache.log4j.Logger; Client: java.util.logging.Logger) */
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static String TF_IDF = "TF_IDF";
    public static String TextRank = "textRank";
    protected TextRankAnalyser textRankAnalyser = null;
    protected TFIDFTextAnalyser tfIdfAnalyser = null;
    protected Word2VecAnalyser word2vecAnalyser = null;
    protected DocumentConverter documentConverter = null;
    protected boolean isBuilt = false;
    private BodyContentHandler bodyContentHandler = null;
    protected GermanTextProcessing germanTextProcessing = null;
    protected List<ALKISDTO> alkisDTOs = new ArrayList<ALKISDTO>();

    protected List<TermDTO> termDTOs = new ArrayList<TermDTO>();
    // private int word2vecAnalyserCount = 10;
    // intern use (store keywords)
    private Map<String, Double> tfIDFResults = null;

    // intern use (store keywords)
    private Map<String, Double> textRankResults = null;

    private Map<String, Map<String, List<String>>> textRankLinkResults = null;

    private DocumentAnalyser() {

    }

    public Map<String, Pair<ALKISDTO, Double>> analyse2ALKIS(final File file) throws SAXException, TikaException, Exception {

        performKeyWordExtraction(file);
        Map<String, Pair<ALKISDTO, Double>> result = new HashMap<String, Pair<ALKISDTO, Double>>();
        if (Config.TTF_IDF_Results) {
            Map<String, Pair<ALKISDTO, Double>> temp = filterTextKeywordsBySimilarity(ALKISDTO.class, alkisDTOs, tfIDFResults);
            result.putAll(temp);
        }
        if (Config.TEXTRANK_Results) {
            Map<String, Pair<ALKISDTO, Double>> temp = filterTextKeywordsBySimilarity(ALKISDTO.class, alkisDTOs, textRankResults);
            result.putAll(temp);
        }
        return result;

    }

    public Map<String, Pair<TermDTO, Double>> analyse2LinkedTerms(final File file) throws SAXException, TikaException, Exception {

        if (!Config.TEXTRANK_Results) {
            throw new IOException("set TEXTRANK_Results = true in " + Config.Source());
        }

        performKeyWordExtraction(file);
        logger.info("seeking in- and outbound terms");
        textRankLinkResults = textRankAnalyser.getLinkedTerms(textRankResults, Config.TEXTRANK_MAXLINKS);
        logger.info(MessageFormat.format("founded overall {0}", textRankLinkResults.size()));
        return null;
    }

    public Map<String, Pair<TermDTO, Double>> analyse2Terms(final File file) throws SAXException, TikaException, Exception {

        performKeyWordExtraction(file);
        Map<String, Pair<TermDTO, Double>> result = new HashMap<String, Pair<TermDTO, Double>>();
        if (Config.TTF_IDF_Results) {
            Map<String, Pair<TermDTO, Double>> temp = filterTextKeywordsBySimilarity(TermDTO.class, termDTOs, tfIDFResults);
            result.putAll(temp);
        }
        if (Config.TEXTRANK_Results) {
            Map<String, Pair<TermDTO, Double>> temp = filterTextKeywordsBySimilarity(TermDTO.class, termDTOs, textRankResults);
            result.putAll(temp);
        }
        return result;

    }

    public <T extends DTO> Map<String, Pair<T, Double>> filterTextKeywordsBySimilarity(Class<T> clazz, List<T> dtos, final Map<String, Double> keywords) {
        Map<String, Pair<T, Double>> result = new HashMap<String, Pair<T, Double>>();
        logger.info("filter keywords " + clazz.getClass().getName());
        double currentSimilarity = 0.0f;
        double similarity = Config.WORD2VEC_SIMILARITY / 100.00d;
        for (T dto : dtos) {
            for (String key : keywords.keySet()) {
                currentSimilarity = word2vecAnalyser.similarity(((DTO)dto).getName(), key);
                //logger.info(MessageFormat.format("term:{0} = {1} | similarity {2}", key, ((DTO)dto).getName(), currentSimilarity));
                if (currentSimilarity > similarity) {
                    result.put(key, Pair.of(dto, keywords.get(key)));
                }
            }
        }
        return result;
    }

    public ALKISDTO getALKISDTO(String term) {
        List<Pair<String, String>> stemmed = germanTextProcessing.stemm(Arrays.asList(term));

        Pair<String, String> pair = stemmed.get(0);
        ALKISDTO dto = alkisDTOs.stream()
                .filter(item -> pair.getLeft().equals(item.getName()) || pair.getRight().equals(item.getName()))
                .findFirst()
                .orElse(null);
        return dto;
    }

    public Metadata getMetaData(File file) throws SAXException, TikaException, IOException, Exception {
        return documentConverter.getMetaData(file);
    }

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
        return result;

    }

    public TermDTO getTermDTO(String term) {

        List<Pair<String, String>> stemmed = germanTextProcessing.stemm(Arrays.asList(term));

        Pair<String, String> pair = stemmed.get(0);
        TermDTO dto = termDTOs.stream()
                .filter(item -> pair.getLeft().equals(item.getName()))
                .findFirst()
                .orElse(null);

        return dto;

    }

    private void performKeyWordExtraction(final File file) throws SAXException, TikaException, Exception {

        bodyContentHandler = documentConverter.getBodyContentHandler(file);
        if (Config.TTF_IDF_Results) {
            logger.info("run tf idf");
            tfIDFResults = tfIdfAnalyser
                    .getTermsAndScores(bodyContentHandler);
            logger.info(MessageFormat.format("tf idf founded {0} keywords", tfIDFResults.size()));
        }
        if (Config.TEXTRANK_Results) {
            logger.info("run text rank");
            textRankResults = textRankAnalyser.getTermsAndScores(bodyContentHandler);
            logger.info(MessageFormat.format("text rank founded {0} keywords", textRankResults.size()));

        }
        if (!Config.TEXTRANK_Results && !Config.TEXTRANK_Results) {
            throw new IOException(
                    "no results set, \n change one or both properties (TTF_IDF_Results = true, TEXTRANK_Results = true) in \n " + Config.Source());
        }

    }

    public void setAlkis(List<ALKISDTO> alkisDTOs) {
        this.alkisDTOs = new ArrayList<ALKISDTO>(alkisDTOs);
    }

    public void setTerms(List<TermDTO> termDTOs) {
        this.termDTOs = new ArrayList<TermDTO>(termDTOs);
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
