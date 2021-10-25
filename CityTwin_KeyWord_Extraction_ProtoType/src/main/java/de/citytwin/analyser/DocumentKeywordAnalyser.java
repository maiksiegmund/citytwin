package de.citytwin.analyser;

import de.citytwin.algorithm.keywords.KeywordExtractor;
import de.citytwin.algorithm.word2vec.Word2Vec;
import de.citytwin.catalog.Catalog;
import de.citytwin.catalog.CatalogEntryHasName;
import de.citytwin.converter.DocumentConverter;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class provides methods to analyse a file
 *
 * @author Maik Siegmund, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class DocumentKeywordAnalyser implements Keywords, AutoCloseable {

    /** Aktuelle Versionsinformation */
    private static final String VERSION = "$Revision: 1.00 $";
    /** Klassenspezifischer, aktueller Logger (Server: org.apache.log4j.Logger; Client: java.util.logging.Logger) */
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static Properties getDefaultProperties() {
        Properties properties = new Properties();
        properties.put("similarity", 66);
        properties.put("maxNearest", 10);
        return properties;
    }

    private Properties properties = null;
    private DocumentConverter documentConverter = null;

    private Word2Vec word2vec = null;

    /**
     * Konstruktor.
     *
     * @param properties = {@code DocumentKeywordAnalyser.getDefaultProperties()}
     * @param documentConverter
     * @param word2vec
     * @throws IOException
     */
    public DocumentKeywordAnalyser(Properties properties, DocumentConverter documentConverter, Word2Vec word2vec) throws IOException {
        if (validateProperties(properties)) {
            properties.putAll(properties);
            this.documentConverter = documentConverter;
            this.word2vec = word2vec;
        }
    }

    @Override
    public void close() throws Exception {
        properties = null;
        documentConverter = null;
        word2vec = null;
    }

    @Override
    public Map<String, Double> filterKeywords(Map<String, Double> keywords, Catalog<? extends CatalogEntryHasName> catalog) throws IOException {

        Map<String, Double> filteredKeywords = new HashMap<String, Double>();
        double currentSimilarity = 0.0f;
        Double similarity = (Integer)properties.get("similarity") / 100.0d;
        CatalogEntryHasName hasName = null;

        for (String keyword : keywords.keySet()) {
            hasName = catalog.getEntry(keyword);
            if (hasName != null) {
                currentSimilarity = word2vec.similarity(keyword, hasName.getName());
                if (currentSimilarity > similarity) {
                    filteredKeywords.put(keyword, keywords.get(keyword));
                }
            }
        }

        return null;

    }

    @Override
    public Map<String, Double> getKeywords(File file, KeywordExtractor keywordExtractor) throws Exception {
        BodyContentHandler bodyContentHandler = documentConverter.getBodyContentHandler(file);
        List<List<String>> textcorpus = documentConverter.getCleanedTextCorpus(bodyContentHandler);
        return keywordExtractor.getKeywords(textcorpus);

    }

    /**
     * this method get nearst to a given word
     *
     * @param filterdWords
     * @throws IOException
     */
    public Map<String, List<String>> getNearestTo(List<String> keywords) throws IOException {

        Integer maxNearest = (Integer)properties.get("maxNearest");
        Map<String, List<String>> result = new HashMap<String, List<String>>(keywords.size());
        for (String keyword : keywords) {
            List<String> nearestWords = word2vec.wordsNearest(keyword, maxNearest);
            result.put(keyword, nearestWords);

        }
        return result;

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

    /**
     * this method validate passing properties
     *
     * @param properties
     * @return
     * @throws IOException
     */
    private Boolean validateProperties(Properties properties) throws IOException {
        Integer value = (Integer)properties.get("similarity");
        if (value == null) {
            throw new IOException("set property --> normalization as Integer");
        }
        value = (Integer)properties.get("maxNearest");
        if (value == null) {
            throw new IOException("set property --> maxNearest as Integer");
        }
        return true;
    }

}
