package de.citytwin.example;

import com.fasterxml.jackson.core.type.TypeReference;

import de.citytwin.algorithm.LocationEntitiesExtractor;
import de.citytwin.algorithm.keywords.TFIDFKeywordExtractor;
import de.citytwin.algorithm.keywords.TextRankKeywordExtractor;
import de.citytwin.algorithm.word2vec.Word2Vec;
import de.citytwin.analyser.DocumentKeywordAnalyser;
import de.citytwin.analyser.DocumentNameFinderAnalyser;
import de.citytwin.catalog.ALKIS;
import de.citytwin.catalog.Catalog;
import de.citytwin.catalog.CatalogEntryHasName;
import de.citytwin.catalog.Term;
import de.citytwin.catalog.WikiArticle;
import de.citytwin.config.ApplicationConfiguration;
import de.citytwin.converter.DocumentConverter;
import de.citytwin.database.DBController;
import de.citytwin.keywords.KeywordExtractor;
import de.citytwin.text.TextProcessing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class show some example for usage
 *
 * @author Maik Siegmund, FH Erfurt
 */
public class Example {

    /** Klassenspezifischer, aktueller Logger (Server: org.apache.log4j.Logger; Client: java.util.logging.Logger) */
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String PROPERTY_ARGUMENT = "-p";

    /**
     * this method is an example to create alkis catalog edit parameters
     *
     * @throws IOException
     * @throws Exception
     */
    public static void createALKISCatalog() throws IOException, Exception {

        Properties properties = new Properties();
        properties.putAll(Catalog.getDefaultProperties(ALKIS.class));

        try(Catalog<ALKIS> catalog = new Catalog<ALKIS>(properties, ALKIS.class)) {

            ALKIS alkis = (ALKIS)catalog.getEntry("Wasser");
            System.out.println(alkis.toString());
        }

    }

    /**
     * this method create specific catalogs by properties
     *
     * @param properties
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    private static List<Catalog<CatalogEntryHasName>> createCatalogs(Properties properties) throws ClassNotFoundException, NoSuchMethodException,
            SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        String temp = properties.getProperty(ApplicationConfiguration.KEYWORD_FILTER_CATALOGS);
        List<Catalog<CatalogEntryHasName>> catalogs = new ArrayList<Catalog<CatalogEntryHasName>>();
        String[] catalogClassNames = temp.split(",");
        for (String catalogClassName : catalogClassNames) {
            Class<?> catalogTypeClass = Class.forName(catalogClassName);
            Constructor<?> catalogConstructor = Catalog.class.getConstructor(Properties.class, catalogTypeClass.getClass());
            Catalog<CatalogEntryHasName> catalog = (Catalog<CatalogEntryHasName>)catalogConstructor.newInstance(properties, catalogTypeClass);
            catalogs.add(catalog);

        }
        return catalogs;
    }

    /**
     * this method create specific KeywordExtractors by properties
     *
     * @param properties
     * @param textProcessing
     * @return
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws ClassNotFoundException
     */
    private static List<KeywordExtractor> createKeywordExtractor(Properties properties, TextProcessing textProcessing) throws NoSuchMethodException,
            SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException {
        String temp = properties.getProperty(ApplicationConfiguration.KEYWORD_EXTRACTOR_ALGORITHMS);
        String[] keywordExtractorClassNames = temp.split(",");
        List<KeywordExtractor> keywordExtractors = new ArrayList<KeywordExtractor>();
        for (String keywordExtractorClassName : keywordExtractorClassNames) {
            Class<?> keyWordExtractorClass = Class.forName(keywordExtractorClassName);
            Constructor<?> constructor = keyWordExtractorClass.getConstructor(Properties.class, TextProcessing.class);
            KeywordExtractor keywordExtractor = (KeywordExtractor)constructor.newInstance(properties, textProcessing);
            keywordExtractors.add(keywordExtractor);
        }
        return keywordExtractors;
    }

    /**
     * this method analyzes documents and store the results properties load from file
     *
     * @param args
     * @throws Exception
     */
    public static void createPartOfCityGraph(String[] args) throws Exception {

        // check application property file
        String propertiesPath = validateProgramArgumentOrExit(args);
        Properties properties = new Properties();

        try(InputStream inputStream = new FileInputStream(propertiesPath)) {
            properties.load(inputStream);

        }
        // reading documents
        String documentsFolder = properties.getProperty(ApplicationConfiguration.DOCUMENTS_FOLDER);
        List<File> files = new ArrayList<File>();
        getFiles(documentsFolder, files);

        // run keyword extraction
        try(
                Word2Vec word2Vec = new Word2Vec(properties);
                TextProcessing textProcessing = new TextProcessing(properties);
                DocumentConverter documentConverter = new DocumentConverter(properties, textProcessing);
                DocumentKeywordAnalyser documentKeywordAnalyser = new DocumentKeywordAnalyser(properties, documentConverter, word2Vec);
                DBController dbController = new DBController(properties)) {

            Map<String, Double> keywords = new HashMap<String, Double>();

            for (File file : files) {
                // keyword extractor
                List<KeywordExtractor> keywordExtractors = createKeywordExtractor(properties, textProcessing);
                for (KeywordExtractor keywordExtractor : keywordExtractors) {
                    keywords.putAll(documentKeywordAnalyser.getKeywords(file, keywordExtractor));
                }
                // filtering
                Map<String, Double> filteredKeywords = new HashMap<String, Double>();
                List<Catalog<CatalogEntryHasName>> catalogs = Example.createCatalogs(properties);
                for (Catalog<CatalogEntryHasName> catalog : catalogs) {
                    filteredKeywords.putAll(documentKeywordAnalyser.filterKeywords(keywords, catalog));
                }
                // persist
                Metadata metaData = documentConverter.getMetaData(file);

                for (String keyword : filteredKeywords.keySet()) {
                    for (Catalog<CatalogEntryHasName> catalog : catalogs) {
                        CatalogEntryHasName catalogEntryHasName = catalog.getEntry(keyword);
                        dbController.buildGraph(keyword, metaData, catalogEntryHasName, filteredKeywords.get(keyword));
                    }
                }
            }
        }
    }

    /**
     * this method is an example to analysis a single document edit parameters
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
        properties.putAll(TextRankKeywordExtractor.getDefaultProperties());
        properties.putAll(Catalog.getDefaultProperties(Term.class));

        File file = new File("D:\\vms\\sharedFolder\\dokumente\\2_begruendung-9-11-ve.pdf");

        try(
                Word2Vec word2Vec = new Word2Vec(properties);
                TextProcessing textProcessing = new TextProcessing(properties);
                DocumentConverter documentConverter = new DocumentConverter(properties, textProcessing);
                DocumentKeywordAnalyser documentKeywordAnalyser = new DocumentKeywordAnalyser(properties, documentConverter, word2Vec);
                Catalog<Term> catalog = new Catalog<Term>(properties, Term.class)) {

            KeywordExtractor keywordExtractor = new TextRankKeywordExtractor(properties, textProcessing); // parameterize KeywordExtractor implementation using
                                                                                                          // properties

            Map<String, Double> temp = documentKeywordAnalyser.getKeywords(file, keywordExtractor);
            filteredKeywords = documentKeywordAnalyser.filterKeywords(temp, catalog);

        }
        LOGGER.info("document analysed");
        for (String key : filteredKeywords.keySet()) {
            System.out.println(MessageFormat.format("keyword {0} \t\t\t  score:{1} ", key, filteredKeywords.get(key)));
        }
        System.out.println(MessageFormat.format("founded keywords {0}", filteredKeywords.size()));

        return filteredKeywords;

    }

    /**
     * this method is an example to run location finding on a single document edit parameters!
     *
     * @throws Exception
     */
    public static void doLocationFinding() throws IOException, Exception {
        Properties properties = new Properties();
        properties.putAll(TextProcessing.getDefaultProperties());
        properties.putAll(DocumentConverter.getDefaultProperties());
        properties.putAll(LocationEntitiesExtractor.getDefaultProperties());

        // properties.put("path.2.ner.location.file", "D:\\VMS\\trained_model\\de-ner-location_maxent.bin");
        properties.put("path.2.ner.location.file", "D:\\VMS\\trained_model\\de-ner-location_naivebayes.bin");

        properties.put("path.2.sentence.detector.file", "D:\\VMS\\trained_model\\de-sent.bin");
        properties.put("path.2.pos-tagger.file", "D:\\VMS\\trained_model\\de-pos-maxent.bin");
        properties.put("path.2.sentence.tokenizer.file", "D:\\VMS\\trained_model\\de-token.bin");
        properties.put("path.2.stopword.file", "D:\\VMS\\trained_model\\de-stopswords.txt");
        properties.put("path.2.postag.file", "D:\\VMS\\trained_model\\de-posTags.txt");
        properties.put("minProbability", 0.95d);

        try(
                TextProcessing textProcessing = new TextProcessing(properties);
                DocumentConverter documentConverter = new DocumentConverter(properties, textProcessing);
                DocumentNameFinderAnalyser documentNameFinderAnalyser = new DocumentNameFinderAnalyser(documentConverter);
                LocationEntitiesExtractor locationEntitiesExtractor = new LocationEntitiesExtractor(properties);) {

            File file = new File("D:\\VMS\\sharedFolder\\2_begruendung-9-11-ve.pdf");
            Set<String> locations = documentNameFinderAnalyser.getNamedEntities(file, locationEntitiesExtractor);
            for (String location : locations) {
                System.out.println(MessageFormat.format("location: {0}", location));
            }
            System.out.println(MessageFormat.format("count: {0}", locations.size()));

        }
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
     * this method prints information for usage
     */
    private static void help() {
        System.out.println("-c --> path 2 properties file");
        System.out.println("e.g (windows)");
        System.out.println("java -jar documentInformationRetrieval.jar -p documentInformationRetrieval.properties");
    }

    /**
     * this method is an example to run textRank algorithm on a single document edit parameters
     *
     * @return
     * @throws Exception
     */
    public static Map<String, Double> runTextRank() throws Exception {

        Map<String, Double> keywords = new HashMap<String, Double>();
        Properties properties = new Properties();
        File file = new File("D:\\vms\\sharedFolder\\dokumente\\2_begruendung-9-11-ve.pdf");

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

            LOGGER.info("textRank finish");
        }

        return keywords;
    }

    /**
     * this method is an example to run textRank algorithm on a single document edit parameters!
     *
     * @return
     * @throws Exception
     */
    public static Map<String, Double> runTFIDF() throws Exception {

        Map<String, Double> keywords = new HashMap<String, Double>();
        Properties properties = new Properties();
        File file = new File("D:\\vms\\sharedFolder\\dokumente\\2_begruendung-9-11-ve.pdf");

        properties.putAll(TextProcessing.getDefaultProperties());
        properties.putAll(DocumentConverter.getDefaultProperties());
        properties.putAll(TFIDFKeywordExtractor.getDefaultProperties());

        try(
                TextProcessing textProcessing = new TextProcessing(properties);
                DocumentConverter documentConverter = new DocumentConverter(properties, textProcessing);
                TFIDFKeywordExtractor tfidfKeywordExtractor = new TFIDFKeywordExtractor(properties, textProcessing);) {

            BodyContentHandler bodyContentHandler = documentConverter.getBodyContentHandler(file);

            List<List<String>> textCorpus = documentConverter.getCleanedTextCorpus(bodyContentHandler);

            keywords = tfidfKeywordExtractor.getKeywords(textCorpus);

            LOGGER.info("TFIDF finish");
        }

        return keywords;
    }

    /**
     * this method is an example to save results in neo4j database used testdata
     *
     * @throws IOException
     * @throws Exception
     */
    public static void saveAnalyseResult() throws IOException, Exception {

        Properties properties = new Properties();
        // Properties properties = PropertiesLGF1000561.getProperties();
        // properties.putAll(Catalog.getDefaultProperties(ALKIS.class));
        // properties.putAll(Catalog.getDefaultProperties(Term.class));
        // properties.putAll(DBController.getDefaultProperties());
        // properties.putAll(DocumentConverter.getDefaultProperties());

        // test data
        Metadata metaData = new Metadata();
        metaData.add("title", "Strategie.pdf");
        metaData.add("date", "01.01.1970");
        metaData.add("author", "Senat Berlin");
        metaData.add("name", "2_begruendung-9-11-ve.pdf");

        Map<String, Double> filteredkeywords = new HashMap<String, Double>();
        filteredkeywords.put("Siedlungsentwicklung", 0.019d);
        filteredkeywords.put("Kita", 0.019d);
        filteredkeywords.put("Grundstückspreise", 0.019d);
        filteredkeywords.put("Wohnräume", 0.019d);
        filteredkeywords.put("Flora", 0.019d);
        filteredkeywords.put("Schulstandorte", 0.019d);
        filteredkeywords.put("Grenzwerte", 0.019d);
        filteredkeywords.put("Wasserversorgungsleitung", 0.019d);
        filteredkeywords.put("Verkehrserschließung", 0.019d);
        filteredkeywords.put("Lichtsignalanlage", 0.019d);

        try(
                DBController dbController = new DBController(properties);
                Catalog<Term> termCatalog = new Catalog<Term>(properties, Term.class);
                Catalog<ALKIS> ALKSICatalog = new Catalog<ALKIS>(properties, ALKIS.class);)

        {
            for (String keyword : filteredkeywords.keySet()) {
                CatalogEntryHasName catalogEntryHasName = termCatalog.getEntry(keyword);
                // dbController.persist(keyword, metaData, catalogEntryHasName, filteredkeywords.get(keyword));
                catalogEntryHasName = ALKSICatalog.getEntry("Wasser");
                dbController.buildGraph(keyword, metaData, catalogEntryHasName, filteredkeywords.get(keyword));
            }

        }

    }

    /**
     * this method create prepared property file
     *
     * @param file
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws IOException
     */
    public static void storePreparedProperties(File file) throws IllegalArgumentException, IllegalAccessException, IOException {

        Properties properties = new Properties();
        String values = "";
        List<String> algorithmsClasses = new ArrayList<>();
        List<String> catalogClasses = new ArrayList<>();

        try(OutputStream outputstream = new FileOutputStream(file);) {

            properties.setProperty(ApplicationConfiguration.CLEANING_PATTERN, "[^\\u2013\\u002D\\wäÄöÖüÜß,-/]");
            properties.setProperty(ApplicationConfiguration.DOCUMENTS_FOLDER, "D:\\VMS\\documents");
            properties.setProperty(ApplicationConfiguration.ITERATION, "10");

            algorithmsClasses.add(TFIDFKeywordExtractor.class.getName());
            algorithmsClasses.add(TextRankKeywordExtractor.class.getName());
            for (String algorithmsClass : algorithmsClasses) {
                values += algorithmsClass + ",";
            }
            values = values.substring(0, values.length() - 1);
            properties.setProperty(ApplicationConfiguration.KEYWORD_EXTRACTOR_ALGORITHMS, values);

            values = "";
            catalogClasses.add(ALKIS.class.getName());
            catalogClasses.add(Term.class.getName());
            for (String catalogClass : catalogClasses) {
                values += catalogClass + ",";
            }
            values = values.substring(0, values.length() - 1);
            properties.setProperty(ApplicationConfiguration.KEYWORD_FILTER_CATALOGS, values);
            properties.setProperty(ApplicationConfiguration.MAX_NEAREST, "10");
            properties.setProperty(ApplicationConfiguration.MAX_NEW_LINES, "5");
            properties.setProperty(ApplicationConfiguration.MIN_PROBABILITY, "66");
            properties.setProperty(ApplicationConfiguration.MIN_TABLE_OF_CONTENT, "50");
            properties.setProperty(ApplicationConfiguration.MIN_TERM_COUNT, "5");
            properties.setProperty(ApplicationConfiguration.MIN_TERM_LENGTH, "2");
            properties.setProperty(ApplicationConfiguration.NEO4J_PASSWORD, "C1tyTw1n!");
            properties.setProperty(ApplicationConfiguration.NEO4J_URI, "bolt://localhost:7687");
            properties.setProperty(ApplicationConfiguration.NEO4J_USER, "neo4j");
            properties.setProperty(ApplicationConfiguration.NORMALIZATION_TYPE, "none");
            properties.setProperty(ApplicationConfiguration.OUTPUT_FOLDER, "D:\\VMS\\output");
            properties.setProperty(ApplicationConfiguration.PATH_2_ALKIS_CATALOG_FILE, "D:\\VMS\\catalogs\\alkis_catalog.json");
            properties.setProperty(ApplicationConfiguration.PATH_2_NER_LOCATION_FILE, "D:\\VMS\\trained_model\\de-ner-location_maxent.bin");
            properties.setProperty(ApplicationConfiguration.PATH_2_POS_TAGGER_FILE, "D:\\VMS\\trained_model\\de-pos-maxent.bin");
            properties.setProperty(ApplicationConfiguration.PATH_2_POSTAGS_FILE, "D:\\VMS\\postags\\de-posTags.txt");
            properties.setProperty(ApplicationConfiguration.PATH_2_SENTENCE_DETECTOR_FILE, "D:\\VMS\\trained_model\\de-sent.bin");
            properties.setProperty(ApplicationConfiguration.PATH_2_SENTENCE_TOKENIZER_FILE, "D:\\VMS\\trained_model\\de-token.bin");
            properties.setProperty(ApplicationConfiguration.PATH_2_STOPWORDS_FILE, "D:\\VMS\\stopwords\\de-stopswords.txt");
            properties.setProperty(ApplicationConfiguration.PATH_2_Term_CATALOG_FILE, "D:\\VMS\\catalogs\\ct_terms_catalog.json");
            properties.setProperty(ApplicationConfiguration.PATH_2_WORD_2_VEC_FILE, "D:\\VMS\\trained_model\\word2vecnewTrained.bin");
            properties.setProperty(ApplicationConfiguration.POSTGRESQL_PASSWORD, "C1tyTw1n!");
            properties.setProperty(ApplicationConfiguration.POSTGRESQL_URI, "C1tyTw1n!");
            properties.setProperty(ApplicationConfiguration.POSTGRESQL_USER, "C1tyTw1n!");
            properties.setProperty(ApplicationConfiguration.RESULT_2_JSON, "false");
            properties.setProperty(ApplicationConfiguration.RESULT_2_NEO4J, "true");
            properties.setProperty(ApplicationConfiguration.RESULT_2_POSTGRESQL, "false");
            properties.setProperty(ApplicationConfiguration.SIMILARITY, "0.80d");
            properties.setProperty(ApplicationConfiguration.WITH_STEMMING, "false");
            properties.setProperty(ApplicationConfiguration.WITH_STOPWORDFILTER, "true");
            properties.setProperty(ApplicationConfiguration.WITH_VECTOR_NORMALIZATION, "true");
            properties.setProperty(ApplicationConfiguration.WORD_WINDOW_SIZE, "5");
            properties.store(outputstream, "File Updated");
        }
    }

    /**
     * this method is an example to train word to vector model and save on filesystem
     *
     * @throws Exception
     */
    public static void trainWord2Vec() throws Exception {

        Properties properties = new Properties();
        properties.putAll(TextProcessing.getDefaultProperties());
        properties.putAll(Word2Vec.getDefaultProperties());

        List<File> files = new ArrayList<File>();
        Example.getFiles("D:\\vms\\sharedFolder\\wikidumps\\text", files);

        try(
                TextProcessing textProcessing = new TextProcessing(properties);
                Word2Vec word2Vec = new Word2Vec(properties);) {

            List<String> articlesSentences = new ArrayList<String>();
            for (File file : files) {
                List<WikiArticle> articles = DocumentConverter.getObjects(new TypeReference<List<WikiArticle>>() {}, file.getAbsolutePath());
                for (WikiArticle article : articles) {
                    articlesSentences.addAll(textProcessing.tokenize2Sencences(article.getText()));
                }
            }
            HashMap<String, Integer> trainParameters = Word2Vec.getDefaultTrainParameters();
            word2Vec.trainModel(articlesSentences, trainParameters, textProcessing);
            word2Vec.saveModel("D:\\vms\\sharedFolder\\trainModels\\word2vec_" + LocalDate.now() + ".bin");
            LOGGER.info("word2vec model trained successfully");
        }

    }

    /**
     * this method validate program argument or exit this program first -p second path 2 property file
     *
     * @param args
     * @return
     */
    private static String validateProgramArgumentOrExit(String args[]) {

        try {
            if (args[0].toLowerCase().equals(Example.PROPERTY_ARGUMENT) && !args[1].isEmpty()) {
                return args[1];
            }
        } catch (IndexOutOfBoundsException exception) {
            Example.help();
            System.exit(0);
        }
        return null;
    }

}
