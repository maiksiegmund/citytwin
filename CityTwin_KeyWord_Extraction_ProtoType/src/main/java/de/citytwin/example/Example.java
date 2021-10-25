package de.citytwin.example;

import com.fasterxml.jackson.core.type.TypeReference;

import de.citytwin.algorithm.keywords.KeywordExtractor;
import de.citytwin.algorithm.keywords.TFIDFKeywordExtractor;
import de.citytwin.algorithm.keywords.TextRankKeywordExtractor;
import de.citytwin.algorithm.word2vec.Word2Vec;
import de.citytwin.analyser.DocumentKeywordAnalyser;
import de.citytwin.catalog.ALKIS;
import de.citytwin.catalog.Catalog;
import de.citytwin.catalog.CatalogEntryHasName;
import de.citytwin.catalog.Term;
import de.citytwin.catalog.WikiArticle;
import de.citytwin.converter.DocumentConverter;
import de.citytwin.database.DBController;
import de.citytwin.text.TextProcessing;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class show some example for usage
 *
 * @author Maik, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class Example {

    /** Aktuelle Versionsinformation */
    private static final String VERSION = "$Revision: 1.00 $";
    /** Klassenspezifischer, aktueller Logger (Server: org.apache.log4j.Logger; Client: java.util.logging.Logger) */
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * this method is an example to analyses a single document
     *
     * @return
     * @throws IOException
     * @throws Exception
     */
    public static Map<String, Double> doDocumentAnalyse() throws IOException, Exception {

        Map<String, Double> filteredKeywords = null;
        Properties properties = new Properties();
        properties.putAll(Word2Vec.getDefaultProperties());
        properties.putAll(TextProcessing.getDefaultProperties());
        properties.putAll(DocumentKeywordAnalyser.getDefaultProperties());
        properties.putAll(DocumentConverter.getDefaultProperties());
        properties.putAll(TFIDFKeywordExtractor.getDefaultProperties());
        properties.putAll(Catalog.getDefaultProperties(Term.class));

        File file = new File("..\\Documents\\Strategie.pdf");

        try(
                Word2Vec word2Vec = new Word2Vec(properties);
                TextProcessing textProcessing = new TextProcessing(properties);
                DocumentConverter documentConverter = new DocumentConverter(properties, textProcessing);
                DocumentKeywordAnalyser documentKeywordAnalyser = new DocumentKeywordAnalyser(properties, documentConverter, word2Vec);
                Catalog<Term> catalog = new Catalog<Term>(properties, Term.class);) {

            KeywordExtractor keywordExtractor = new TFIDFKeywordExtractor(properties, textProcessing);

            Map<String, Double> temp = documentKeywordAnalyser.getKeywords(file, keywordExtractor);
            filteredKeywords = documentKeywordAnalyser.filterKeywords(temp, catalog);

        }
        return filteredKeywords;

    }

    /**
     * get all files in a folder, run recursive
     *
     * @param path
     * @param foundedFiles reference to List of {@code List<File>}
     */

    public static void getFiles(String path, @Nonnull List<File> foundedFiles) {

        File root = new File(path);
        File[] files = root.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                getFiles(file.getAbsolutePath(), foundedFiles);
            } else {
                foundedFiles.add(file);
            }

        }
    }

    /**
     * this method is an example to run textRank algorithm on a single document
     *
     * @return
     * @throws Exception
     */
    public static Map<String, Double> runTextRank() throws Exception {

        Map<String, Double> keywords = new HashMap<String, Double>();
        Properties properties = new Properties();
        File file = new File("..\\Documents\\Strategie.pdf");

        properties.putAll(TextProcessing.getDefaultProperties());
        properties.putAll(DocumentConverter.getDefaultProperties());
        properties.putAll(TextRankKeywordExtractor.getDefaultProperties());

        try(
                TextProcessing textProcessing = new TextProcessing(properties);
                DocumentConverter documentConverter = new DocumentConverter(properties, textProcessing);
                TextRankKeywordExtractor textRankKeywordExtractor = new TextRankKeywordExtractor(properties, textProcessing);) {

            BodyContentHandler bodyContentHandler = documentConverter.getBodyContentHandler(file);
            List<List<String>> textCorpus = documentConverter.getCleanedTextCorpus(bodyContentHandler);
            keywords = textRankKeywordExtractor.getKeywords(textCorpus);
        }

        logger.info("textRank finish");
        return keywords;
    }

    /**
     * this method is an example to save results in neo4j database by testdata!
     *
     * @throws IOException
     * @throws Exception
     */
    public static void saveAnalyseResult() throws IOException, Exception {

        File file = new File("..\\Documents\\Strategie.pdf");
        Properties properties = new Properties();
        properties.putAll(Catalog.getDefaultProperties(ALKIS.class));
        properties.putAll(Catalog.getDefaultProperties(Term.class));
        properties.putAll(DBController.getDefaultProperties());
        properties.putAll(DocumentConverter.getDefaultProperties());

        // test data
        Metadata metaData = new Metadata();
        metaData.add("Title", "Strategie.pdf");
        metaData.add("Date", "01.01.1970");
        metaData.add("Author", "Senat Berlin");
        metaData.add("name", file.getName());

        Map<String, Double> filteredkeywords = Example.doDocumentAnalyse();

        try(
                DBController dbController = new DBController(properties);
                Catalog<Term> termCatalog = new Catalog<Term>(properties, Term.class);
                Catalog<ALKIS> ALKSICatalog = new Catalog<ALKIS>(properties, ALKIS.class);)

        {
            for (String keyword : filteredkeywords.keySet()) {
                CatalogEntryHasName hasName = termCatalog.getEntry(keyword);
                dbController.persist(keyword, metaData, hasName, filteredkeywords.get(keyword));
                hasName = ALKSICatalog.getEntry(keyword);
                dbController.persist(keyword, metaData, hasName, filteredkeywords.get(keyword));
            }

        }

    }

    /**
     * this method is an example to train word to vector model and save on filesystem
     *
     * @throws Exception
     */
    public static void trainWord2Vec() throws Exception {

        Properties properties = new Properties();
        properties.putAll(Word2Vec.getDefaultProperties());
        properties.putAll(TextProcessing.getDefaultProperties());

        List<File> files = new ArrayList<File>();
        Example.getFiles("D:\\vms\\sharedFolder\\wikidumps\\text\\", files);

        try(
                TextProcessing textProcessing = new TextProcessing(properties);
                Word2Vec word2Vec = new Word2Vec(properties);) {

            List<String> articlesSentences = new ArrayList<String>();
            for (File file : files) {
                List<WikiArticle> articles = DocumentConverter.getDTOs(new TypeReference<List<WikiArticle>>() {}, file.getAbsolutePath());
                for (WikiArticle article : articles) {
                    articlesSentences.addAll(textProcessing.tokenize2Sencences(article.getText()));
                }
            }
            HashMap<String, Integer> trainParameters = Word2Vec.getDefaultTrainParameters();
            word2Vec.trainModel(articlesSentences, trainParameters, textProcessing);
            word2Vec.saveModel("D:\\vms\\sharedFolder\\trainModels\\word2vec.bin");
            logger.info("word2vec model trained successfully");
        }

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
