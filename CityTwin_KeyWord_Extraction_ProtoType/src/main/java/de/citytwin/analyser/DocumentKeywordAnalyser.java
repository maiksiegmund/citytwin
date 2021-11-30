package de.citytwin.analyser;

import de.citytwin.algorithm.word2vec.Word2Vec;
import de.citytwin.catalog.Catalog;
import de.citytwin.catalog.CatalogEntryHasName;
import de.citytwin.config.ApplicationConfiguration;
import de.citytwin.converter.DocumentConverter;
import de.citytwin.keywords.KeywordExtractor;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
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
 */
public class DocumentKeywordAnalyser implements Keywords, AutoCloseable {

    /** Klassenspezifischer, aktueller Logger (Server: org.apache.log4j.Logger; Client: java.util.logging.Logger) */
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static Properties getDefaultProperties() {
        Properties properties = new Properties();
        properties.setProperty(ApplicationConfiguration.SIMILARITY, "66");
        properties.setProperty(ApplicationConfiguration.MAX_NEAREST, "10");
        return properties;
    }

    private DocumentConverter documentConverter = null;
    private Word2Vec word2vec = null;
    private Double similarity = null;
    private Integer maxNearest = null;

    /**
     * constructor.
     *
     * @param properties = {@code DocumentKeywordAnalyser.getDefaultProperties()}
     * @param documentConverter
     * @param word2vec
     * @throws IOException
     */
    public DocumentKeywordAnalyser(Properties properties, DocumentConverter documentConverter, Word2Vec word2vec) throws IOException {
        if (validateProperties(properties)) {
            this.documentConverter = documentConverter;
            this.word2vec = word2vec;
        }
    }

    @Override
    public void close() throws Exception {
        documentConverter = null;
        word2vec = null;
    }

    @Override
    public Map<String, Double> filterKeywords(Map<String, Double> keywords, Catalog<? extends CatalogEntryHasName> catalog) throws IOException {

        Map<String, Double> filteredKeywords = new HashMap<String, Double>();
        double currentSimilarity = 0.0f;

        for (String keyword : keywords.keySet()) {
            for (String name : catalog.getNames()) {
                currentSimilarity = word2vec.similarity(keyword, name);
                if (currentSimilarity > similarity) {
                    filteredKeywords.put(name, keywords.get(keyword));
                }
            }
            if (catalog.contains(keyword)) {
                filteredKeywords.put(keyword, keywords.get(keyword));
            }

        }

        LOGGER.info(MessageFormat.format(
                "keywords removed: {0} \n" +
                        "similarity:       {1} \n" +
                        "origin contains:  {2} \n" +
                        "remain contains:  {3}",
                keywords.size() - filteredKeywords.size(),
                similarity,
                keywords.size(),
                filteredKeywords.size()));

        return filteredKeywords;

    }

    @Override
    public Map<String, Double> getKeywords(File file, KeywordExtractor keywordExtractor) throws Exception {
        BodyContentHandler bodyContentHandler = documentConverter.getBodyContentHandler(file);
        List<List<String>> textcorpus = documentConverter.getCleanedTextCorpus(bodyContentHandler);
        return keywordExtractor.getKeywords(textcorpus);

    }

    /**
     * this method get nearest to a given word
     *
     * @param filterdWords
     * @throws IOException
     */
    public Map<String, List<String>> getNearestTo(List<String> keywords) throws IOException {

        Map<String, List<String>> result = new HashMap<String, List<String>>(keywords.size());
        for (String keyword : keywords) {
            List<String> nearestWords = word2vec.wordsNearest(keyword, maxNearest);
            result.put(keyword, nearestWords);

        }
        return result;

    }

    /**
     * this method validate passing properties and set them
     *
     * @param properties
     * @return
     * @throws IOException
     */
    private Boolean validateProperties(Properties properties) throws IllegalArgumentException {
        String property = properties.getProperty(ApplicationConfiguration.SIMILARITY);
        if (property == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.SIMILARITY);
        }
        similarity = Double.parseDouble(property) / 100.0d;
        property = properties.getProperty(ApplicationConfiguration.MAX_NEAREST);
        if (property == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.MAX_NEAREST);
        }
        maxNearest = Integer.parseInt(property);
        return true;
    }

}
