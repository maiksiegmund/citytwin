package de.citytwin.example;

import com.fasterxml.jackson.core.type.TypeReference;

import de.citytwin.algorithm.keywords.TFIDFKeywordExtractor;
import de.citytwin.algorithm.keywords.TextRankKeywordExtractor;
import de.citytwin.algorithm.word2vec.Word2Vec;
import de.citytwin.analyser.DocumentKeywordAnalyser;
import de.citytwin.analyser.DocumentNamedEntityAnalyser;
import de.citytwin.catalog.Catalog;
import de.citytwin.catalog.HasName;
import de.citytwin.config.ApplicationConfiguration;
import de.citytwin.converter.DocumentConverter;
import de.citytwin.database.Neo4JController;
import de.citytwin.database.PostgreSQLController;
import de.citytwin.keywords.KeywordExtractor;
import de.citytwin.location.LocationEntitiesExtractor;
import de.citytwin.model.ALKIS;
import de.citytwin.model.Address;
import de.citytwin.model.Location;
import de.citytwin.model.Term;
import de.citytwin.model.WikiArticle;
import de.citytwin.text.TextProcessing;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

    public static void persistTermCatalog(String[] args) throws IOException, Exception {

        String propertiesPath = validateProgramArgumentOrExit(args);
        InputStream inputStream = new FileInputStream(propertiesPath);
        Properties appProperties = new Properties();
        appProperties.load(inputStream);
        Properties termCatalogProperties = new Properties();
        termCatalogProperties.put(ApplicationConfiguration.PATH_2_Term_CATALOG_FILE,
                appProperties.getProperty(ApplicationConfiguration.PATH_2_Term_CATALOG_FILE));

        List<Term> added = new ArrayList<Term>();

        try(
                Catalog<Term> catalog = new Catalog<Term>(termCatalogProperties, Term.class);
                PostgreSQLController postgreSQLController = new PostgreSQLController(appProperties)) {
            int count = 1;

            for (String name : catalog.getNames()) {

                System.out.println(count++ + " of " + catalog.getNames().size() + " --> " + name);
                // Term term = (Term)catalog.getEntry(name);
                Term term = (Term)catalog.getEntry(name);
                long id = postgreSQLController.getId(term);
                if (id == 0) {
                    postgreSQLController.insert(term);
                    added.add(term);
                }

            }

            System.out.println("added: " + added.size() + " new terms");
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
    private static List<Catalog<HasName>> createCatalogs(Properties properties) throws ClassNotFoundException, NoSuchMethodException,
            SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        String temp = properties.getProperty(ApplicationConfiguration.KEYWORD_FILTER_CATALOGS);
        List<Catalog<HasName>> catalogs = new ArrayList<Catalog<HasName>>();
        String[] catalogClassNames = temp.split(",");
        for (String catalogClassName : catalogClassNames) {
            Class<?> catalogTypeClass = Class.forName(catalogClassName);
            Constructor<?> catalogConstructor = Catalog.class.getConstructor(Properties.class, catalogTypeClass.getClass());
            Catalog<HasName> catalog = (Catalog<HasName>)catalogConstructor.newInstance(properties, catalogTypeClass);
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

        String propertiesPath = validateProgramArgumentOrExit(args);

        InputStream inputStream = new FileInputStream(propertiesPath);
        Properties properties = new Properties();
        properties.load(inputStream);

        // reading documents
        String documentsFolder = properties.getProperty(ApplicationConfiguration.DOCUMENTS_FOLDER);
        List<File> files = new ArrayList<File>();
        // getFiles(documentsFolder, files, false);
        files.add(new File("D:\\VMS\\documents\\wohnungsmarktbericht_wohnungsbaupotenziale_pr__sentation.pdf"));

        // run keyword extraction
        try(
                Word2Vec word2Vec = new Word2Vec(properties);
                TextProcessing textProcessing = new TextProcessing(properties);
                DocumentConverter documentConverter = new DocumentConverter(properties, textProcessing);
                DocumentKeywordAnalyser documentKeywordAnalyser = new DocumentKeywordAnalyser(properties, documentConverter, word2Vec);
                // Neo4JController neo4JController = new Neo4JController(properties);
                PostgreSQLController postgreSQLController = new PostgreSQLController(properties)) {

            Map<String, Double> keywords = new HashMap<String, Double>();

            for (File file : files) {
                // keyword extractor
                ByteArrayInputStream byteArrayInputStream = Example.getByteArrayInputStream(file);
                String fileName = file.getName();
                List<KeywordExtractor> keywordExtractors = createKeywordExtractor(properties, textProcessing);
                for (KeywordExtractor keywordExtractor : keywordExtractors) {
                    keywords.putAll(documentKeywordAnalyser.getKeywords(byteArrayInputStream, fileName, keywordExtractor));
                }
                // filtering
                Map<String, Double> filteredKeywords = new HashMap<String, Double>();
                List<Catalog<HasName>> catalogs = Example.createCatalogs(properties);
                for (Catalog<HasName> catalog : catalogs) {
                    filteredKeywords.putAll(documentKeywordAnalyser.filterKeywords(keywords, catalog));
                }
                // persist
                Metadata metaData = documentConverter.getMetaData(byteArrayInputStream, fileName);
                metaData.add("Uri", "example");
                for (String keyword : filteredKeywords.keySet()) {
                    LOGGER.info("running");
                    for (Catalog<HasName> catalog : catalogs) {
                        HasName catalogEntryHasName = catalog.getEntry(keyword);
                        // neo4JController.buildGraph(metaData, keyword, catalogEntryHasName, filteredKeywords.get(keyword));
                        postgreSQLController.persist(metaData, keyword, catalogEntryHasName, filteredKeywords.get(keyword));
                    }
                }
                byteArrayInputStream.close();
            }
        }
        LOGGER.info("finished");
    }

    /**
     * this method is an example to analysis a single document edit parameters
     *
     * @return
     * @throws IOException
     * @throws Exception
     */
    public static Map<String, Double> doDocumentAnalyse(String[] args) throws IOException, Exception {

        Map<String, Double> filteredKeywords = new HashMap<String, Double>();
        Map<String, Double> keywords = new HashMap<String, Double>();
        String propertiesPath = validateProgramArgumentOrExit(args);

        InputStream inputStream = new FileInputStream(propertiesPath);
        Properties properties = new Properties();
        properties.load(inputStream);

        File file = new File("D:\\vms\\documents\\wohnungsmarktbericht_wohnungsbaupotenziale_pr__sentation.pdf");

        try(
                Word2Vec word2Vec = new Word2Vec(properties);
                TextProcessing textProcessing = new TextProcessing(properties);
                DocumentConverter documentConverter = new DocumentConverter(properties, textProcessing);
                DocumentKeywordAnalyser documentKeywordAnalyser = new DocumentKeywordAnalyser(properties, documentConverter, word2Vec);
                ByteArrayInputStream byteArrayInputStream = Example.getByteArrayInputStream(file);) {

            List<KeywordExtractor> keywordExtractors = createKeywordExtractor(properties, textProcessing);

            for (KeywordExtractor keywordExtractor : keywordExtractors) {
                Map<String, Double> temp = documentKeywordAnalyser.getKeywords(byteArrayInputStream, file.getName(), keywordExtractor);
                keywords.putAll(temp);
            }

            List<Catalog<HasName>> catalogs = Example.createCatalogs(properties);
            for (Catalog<HasName> catalog : catalogs) {
                filteredKeywords.putAll(documentKeywordAnalyser.filterKeywords(keywords, catalog));
            }

            filteredKeywords.forEach((keyword, value) -> {

                Set<String> texts = documentKeywordAnalyser.getTextPassages(keyword);
                if (texts.size() == 0) {
                    System.out.println("no entry for " + keyword);
                } else {
                    System.out.println(texts.size() + " entries for " + keyword);
                    texts.forEach(System.out::println);
                }

            });

        }
        LOGGER.info("document analysed");
        for (String key : filteredKeywords.keySet()) {
            System.out.println(MessageFormat.format("keyword {0} \t\t\t\t score:{1} ", key, filteredKeywords.get(key)));
        }
        System.out.println(MessageFormat.format("founded keywords {0}", filteredKeywords.size()));

        return filteredKeywords;

    }

    /**
     * this method is an example to run location finding on a single document edit parameters!
     *
     * @throws Exception
     */
    public static void namedEntitieAnalyse(String[] args) throws IOException, Exception {

        String propertiesPath = validateProgramArgumentOrExit(args);

        InputStream inputStream = new FileInputStream(propertiesPath);
        Properties properties = new Properties();
        properties.load(inputStream);

        Map<String, Double> filteredKeywords = null;

        try(
                TextProcessing textProcessing = new TextProcessing(properties);
                DocumentConverter documentConverter = new DocumentConverter(properties, textProcessing);
                DocumentNamedEntityAnalyser documentNamedEntityAnalyser = new DocumentNamedEntityAnalyser(properties, documentConverter);
                LocationEntitiesExtractor locationEntitiesExtractor = new LocationEntitiesExtractor(properties);
                PostgreSQLController postgreSQLController = new PostgreSQLController(properties);) {

            List<File> files = new ArrayList<File>();
            // getFiles("D:\\VMS\\documents", files, false);
            files.add(new File("D:\\VMS\\documents\\wohnungsmarktbericht_wohnungsbaupotenziale_pr__sentation.pdf"));
            for (File file : files) {
                ByteArrayInputStream byteArrayInputStream = Example.getByteArrayInputStream(file);
                String fileName = file.getName();
                Set<String> extractedLocations = documentNamedEntityAnalyser.getNamedEntities(byteArrayInputStream, fileName, locationEntitiesExtractor);
                Set<Address> validatedAddresses = documentNamedEntityAnalyser.validateAddresses(extractedLocations, postgreSQLController);
                Set<Location> validatedLocations = documentNamedEntityAnalyser.validateLocations(extractedLocations, postgreSQLController, false);
                Metadata metadata = documentConverter.getMetaData(byteArrayInputStream, fileName);

                validatedAddresses.forEach(address -> {
                    System.out.println(address.toString());
                    // System.out.println(documentNamedEntityAnalyser.getTextSections(address));
                });
                validatedLocations.forEach(location -> {
                    System.out.println(location.getName());
                    // System.out.println(documentNamedEntityAnalyser.getTextSections(location));
                });
                for (Address address : validatedAddresses) {
                    // postgreSQLController.persist(metadata, address);
                }
                for (Location location : validatedLocations) {
                    // postgreSQLController.persist(metadata, location);
                }
                byteArrayInputStream.close();
            }

        }
    }

    /**
     * get all files in a folder, run recursive
     *
     * @param path
     * @param foundedFiles reference to List of {@code List<File>}
     */

    public static void getFiles(String path, @Nonnull List<File> foundedFiles, boolean recursive) {

        File root = new File(path);
        File[] files = root.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory() && recursive) {
                getFiles(file.getAbsolutePath(), foundedFiles, recursive);
            } else {
                foundedFiles.add(file);
            }

        }
    }

    /**
     * this method prints information for usage
     */
    private static void help() {
        System.out.println("-p --> path 2 properties file");
        System.out.println("e.g (windows)");
        System.out.println("java -jar documentInformationRetrieval.jar -p documentInformationRetrieval.properties");
    }

    /**
     * this method is an example to run textRank algorithm on a single document edit parameters
     *
     * @return
     * @throws Exception
     */
    public static Map<String, Double> runTextRank(String[] args) throws Exception {

        Map<String, Double> keywords = new HashMap<String, Double>();
        File file = new File("D:\\VMS\\documents\\wohnungsmarktbericht_wohnungsbaupotenziale_pr__sentation.pdf");

        String propertiesPath = validateProgramArgumentOrExit(args);

        InputStream inputStream = new FileInputStream(propertiesPath);
        Properties properties = new Properties();
        properties.load(inputStream);

        try(
                TextProcessing textProcessing = new TextProcessing(properties);
                DocumentConverter documentConverter = new DocumentConverter(properties, textProcessing);
                TextRankKeywordExtractor textRankKeywordExtractor = new TextRankKeywordExtractor(properties, textProcessing);
                ByteArrayInputStream byteArrayInputStream = Example.getByteArrayInputStream(file);) {

            Boolean everySingleSentenc = false;
            properties.getProperty(ApplicationConfiguration.EVERY_SINGLE_SENTENCE);

            BodyContentHandler bodyContentHandler = documentConverter.getBodyContentHandler(byteArrayInputStream, file.getName());
            List<List<String>> textCorpus = documentConverter.getCleanedTextCorpus(bodyContentHandler, everySingleSentenc);
            keywords = textRankKeywordExtractor.getKeywords(textCorpus);
            keywords.forEach((key, value) -> System.out.println(key + ":" + value));
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
    public static Map<String, Double> runTFIDF(String[] args) throws Exception {

        Map<String, Double> keywords = new HashMap<String, Double>();
        File file = new File("D:\\VMS\\documents\\wohnungsmarktbericht_wohnungsbaupotenziale_pr__sentation.pdf");

        String propertiesPath = validateProgramArgumentOrExit(args);

        InputStream inputStream = new FileInputStream(propertiesPath);
        Properties properties = new Properties();
        properties.load(inputStream);

        try(
                TextProcessing textProcessing = new TextProcessing(properties);
                DocumentConverter documentConverter = new DocumentConverter(properties, textProcessing);
                TFIDFKeywordExtractor tfidfKeywordExtractor = new TFIDFKeywordExtractor(properties, textProcessing);
                ByteArrayInputStream byteArrayInputStream = Example.getByteArrayInputStream(file);) {

            String fileName = file.getName();
            BodyContentHandler bodyContentHandler = documentConverter.getBodyContentHandler(byteArrayInputStream, fileName);
            List<List<String>> textCorpus = documentConverter.getCleanedTextCorpus(bodyContentHandler, true);

            keywords = tfidfKeywordExtractor.getKeywords(textCorpus);
            keywords.forEach((key, value) -> System.out.println(key + ":" + value));
            LOGGER.info("TFIDF finish");
        }

        return keywords;
    }

    /**
     * this method returns default properties
     *
     * @return new reference of {@code Properties}
     */
    public static Properties getAllDefaultPorProperties() {

        Properties properties = new Properties();
        properties.putAll(TextRankKeywordExtractor.getDefaultProperties());
        properties.putAll(TFIDFKeywordExtractor.getDefaultProperties());
        properties.putAll(Word2Vec.getDefaultProperties());
        properties.putAll(DocumentKeywordAnalyser.getDefaultProperties());
        properties.putAll(DocumentNamedEntityAnalyser.getDefaultProperties());
        properties.putAll(DocumentConverter.getDefaultProperties());
        properties.putAll(Neo4JController.getDefaultProperties());
        properties.putAll(PostgreSQLController.getDefaultProperties());
        properties.putAll(LocationEntitiesExtractor.getDefaultProperties());
        properties.putAll(TextProcessing.getDefaultProperties());
        properties.putAll(ApplicationConfiguration.getDefaultProperties());
        return properties;
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

        try(OutputStream outputstream = new FileOutputStream(file);) {

            Properties properties = getAllDefaultPorProperties();
            // overwrite defaults
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
            properties.setProperty(ApplicationConfiguration.POSTGRESQL_URI, "jdbc:postgresql://83.135.47.253/citytwin");
            properties.setProperty(ApplicationConfiguration.PATH_2_POSTGRESQL_PROPERTY_FILE, "postgreSQL.properties");

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
        Example.getFiles("D:\\vms\\sharedFolder\\wikidumps\\text", files, false);

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

    /**
     * example to run document analyze, by stored property on file system, passing by program parameter
     *
     * @param args
     * @throws Exception
     */
    public static void run(String[] args) throws Exception {

        int size = 0;
        int batchSize = 5000;
        List<String> succeedDocuments = new ArrayList<String>();

        String propertiesPath = validateProgramArgumentOrExit(args);

        InputStream inputStream = new FileInputStream(propertiesPath);
        Properties properties = new Properties();
        properties.load(inputStream);

        boolean keywordAnalyse = Boolean.parseBoolean(properties.getProperty(ApplicationConfiguration.KEYWORD_ANALYSE));
        boolean namedEntitiesAnalyse = Boolean.parseBoolean(properties.getProperty(ApplicationConfiguration.NAMED_ENTITY_ANALYSE));
        boolean toPostGreSQL = Boolean.parseBoolean(properties.getProperty(ApplicationConfiguration.RESULT_2_POSTGRESQL));
        boolean toNeo4J = Boolean.parseBoolean(properties.getProperty(ApplicationConfiguration.RESULT_2_NEO4J));
        boolean containsInSynoyms = Boolean.parseBoolean(properties.getProperty(ApplicationConfiguration.CONTAINS_IN_SYNONYMS));

        try(
                TextProcessing textProcessing = new TextProcessing(properties);
                DocumentConverter documentConverter = new DocumentConverter(properties, textProcessing);
                LocationEntitiesExtractor locationEntitiesExtractor = new LocationEntitiesExtractor(properties);
                DocumentNamedEntityAnalyser documentNamedEntityAnalyser = new DocumentNamedEntityAnalyser(properties, documentConverter);
                PostgreSQLController postgreSQLController = new PostgreSQLController(properties);
                Neo4JController neo4JController = new Neo4JController(properties);) {

            Word2Vec word2Vec = null;
            DocumentKeywordAnalyser documentKeywordAnalyser = null;

            // fetch all unanalyzed documents
            Map<Long, Metadata> unanalyzedDocuments = new HashMap<Long, Metadata>();
            for (Long documentId : postgreSQLController.getUnanalyzeDocumentIDs()) {
                if (size == batchSize)
                    break;
                unanalyzedDocuments.put(documentId, postgreSQLController.getMetadata(documentId));
                size++;
            }
            LOGGER.info("analyse " + unanalyzedDocuments.size() + " documents");
            // init only when needed
            if (keywordAnalyse) {
                word2Vec = new Word2Vec(properties);
                documentKeywordAnalyser = new DocumentKeywordAnalyser(properties, documentConverter, word2Vec);
                Map<String, Double> keywords = new HashMap<String, Double>();
                Map<String, Double> filteredKeywords = new HashMap<String, Double>();
                List<KeywordExtractor> keywordExtractors = createKeywordExtractor(properties, textProcessing);
                List<Catalog<HasName>> catalogs = Example.createCatalogs(properties);
                for (Long documentId : unanalyzedDocuments.keySet()) {
                    try {
                        Metadata metaData = unanalyzedDocuments.get(documentId);
                        LOGGER.info("keywordanalyse for document:\n" + metaData.get("name"));
                        ByteArrayInputStream byteArrayInputStream = Example.getByteArrayInputStream(metaData);
                        if (byteArrayInputStream == null) {
                            postgreSQLController.deleteDocument(documentId);
                            succeedDocuments.add("documentid:" + documentId.toString() + "not available: " + metaData.get("Uri"));
                            LOGGER.info("document not available :\n" + metaData.get("Uri"));
                            continue;
                        }
                        // get keywords
                        for (KeywordExtractor keywordExtractor : keywordExtractors) {
                            keywords.putAll(documentKeywordAnalyser.getKeywords(byteArrayInputStream, metaData.get("name"), keywordExtractor));
                        }
                        // filtering
                        for (Catalog<HasName> catalog : catalogs) {
                            filteredKeywords.putAll(documentKeywordAnalyser.filterKeywords(keywords, catalog));
                            // persist
                            for (String filteredKeyword : filteredKeywords.keySet()) {
                                if (toPostGreSQL) {
                                    LOGGER.info("store in postgre");
                                    succeedDocuments.add("documentid:" + documentId.toString() + " keywords store in postgree: " + metaData.get("Uri"));
                                    postgreSQLController
                                            .persist(metaData, filteredKeyword, catalog.getEntry(filteredKeyword), filteredKeywords.get(filteredKeyword));
                                    postgreSQLController
                                            .persist(metaData, filteredKeyword, documentKeywordAnalyser.getTextPassages(filteredKeyword));
                                }

                                if (toNeo4J) {
                                    LOGGER.info("store in neo4j");
                                    succeedDocuments.add("documentid:" + documentId.toString() + " keywords store in neo4j: " + metaData.get("Uri"));
                                    neo4JController.buildGraph(metaData,
                                            filteredKeyword,
                                            catalog.getEntry(filteredKeyword),
                                            filteredKeywords.get(filteredKeyword),
                                            documentKeywordAnalyser.getTextPassages(filteredKeyword));
                                }
                            }
                        }
                        byteArrayInputStream.close();
                        postgreSQLController.setDocumentIsAnalysed(documentId);
                        LOGGER.info("keyordanalyse for document:\n" + metaData.get("name") + " finished");
                        keywords.clear();
                        filteredKeywords.clear();
                    } catch (Exception exception) {
                        String errorMessage = "error keyword analyze by document \n "
                                + "id:      {0} \n "
                                + "message: {1}";
                        LOGGER.error(MessageFormat.format(errorMessage, documentId, exception.getMessage()), exception);
                    }

                }
                word2Vec.close();
                documentKeywordAnalyser.close();
            }
            if (namedEntitiesAnalyse) {
                for (Long documentId : unanalyzedDocuments.keySet()) {
                    try {
                        Metadata metaData = unanalyzedDocuments.get(documentId);
                        LOGGER.info("named entity analyse for document:\n" + metaData.get("name"));
                        ByteArrayInputStream byteArrayInputStream = Example.getByteArrayInputStream(metaData);
                        if (byteArrayInputStream == null) {
                            postgreSQLController.deleteDocument(documentId);
                            LOGGER.info("document not available :\n" + metaData.get("Uri"));
                            succeedDocuments.add("documentid:" + documentId.toString() + " not available: " + metaData.get("Uri"));
                            continue;
                        }
                        Set<String> extractedLocations = documentNamedEntityAnalyser
                                .getNamedEntities(byteArrayInputStream, metaData.get("name"), locationEntitiesExtractor);
                        Set<Address> validatedAddresses = documentNamedEntityAnalyser.validateAddresses(extractedLocations, postgreSQLController);
                        Set<Location> validatedLocations = documentNamedEntityAnalyser
                                .validateLocations(extractedLocations, postgreSQLController, containsInSynoyms);
                        if (toPostGreSQL) {
                            for (Location location : validatedLocations) {
                                LOGGER.info("store in postgre");
                                succeedDocuments.add("documentid:" + documentId.toString() + " POIs store in postgree: " + metaData.get("Uri"));
                                postgreSQLController.persist(metaData, location);
                                postgreSQLController.persist(metaData, location, documentNamedEntityAnalyser.getTextPassages(location));
                            }
                            for (Address address : validatedAddresses) {
                                LOGGER.info("store in postgre");
                                succeedDocuments.add("documentid:" + documentId.toString() + " Addresses store in postgree: " + metaData.get("Uri"));
                                postgreSQLController.persist(metaData, address);
                                postgreSQLController.persist(metaData, address, documentNamedEntityAnalyser.getTextPassages(address));
                            }
                        }
                        if (toNeo4J) {
                            validatedAddresses
                                    .forEach(address -> {
                                        neo4JController.buildGraph(metaData, address, documentNamedEntityAnalyser.getTextPassages(address));
                                        succeedDocuments.add("documentid:" + documentId.toString() + " Adresses store in Neo4j: " + metaData.get("Uri"));
                                    });
                            validatedLocations
                                    .forEach(location -> {
                                        neo4JController.buildGraph(metaData, location, documentNamedEntityAnalyser.getTextPassages(location));
                                        succeedDocuments.add("documentid:" + documentId.toString() + " POIs store in Neo4j: " + metaData.get("Uri"));
                                    });
                        }
                        postgreSQLController.setDocumentIsAnalysed(documentId);
                        byteArrayInputStream.close();
                        LOGGER.info("named entity analyse for document:\n" + metaData.get("name") + " finished");
                    } catch (Exception exception) {

                        String errorMessage = "error named entitiy analyse by document \n "
                                + "id:      {0} \n "
                                + "message: {1}";
                        LOGGER.error(MessageFormat.format(errorMessage, documentId, exception.getMessage()), exception);
                    }

                }
            }
            LOGGER.info("processed " + unanalyzedDocuments.size() + " documents");
            LOGGER.info("process finished");
        }

        succeedDocuments.forEach(System.out::println);
    }

    // ToDo Errorhandling!
    public static ByteArrayInputStream getByteArrayInputStream(Metadata metaData) throws IOException {
        try {
            URL url = new URL(metaData.get("Uri"));
            byte[] bytes = url.openStream().readAllBytes();
            return new ByteArrayInputStream(bytes);
        } catch (Exception exception) {
            return null;
        }
    }

    public static ByteArrayInputStream getByteArrayInputStream(File file) throws IOException {
        byte[] bytes = file.toURI().toURL().openStream().readAllBytes();
        return new ByteArrayInputStream(bytes);
    }

    public static void getNearest(String[] args) throws Exception {

        Map<String, Double> filteredKeywords = new HashMap<String, Double>();
        Map<String, Double> keywords = new HashMap<String, Double>();
        String propertiesPath = validateProgramArgumentOrExit(args);

        InputStream inputStream = new FileInputStream(propertiesPath);
        Properties properties = new Properties();
        properties.load(inputStream);

        try(Word2Vec word2Vec = new Word2Vec(properties)) {
            word2Vec.wordsNearest("Wohnungsbau", 50).forEach((item) -> System.out.println(item));
            word2Vec.similarWordsInVocabTo("Wohnungsbau", 10.d).forEach((item) -> System.out.println(item));
            System.out.println("finished");
        }

    }

    public static void updateDocumentsAndKeywordsTextpassages(String[] args) throws ClassNotFoundException, SQLException, Exception {

        String propertiesPath = validateProgramArgumentOrExit(args);

        InputStream inputStream = new FileInputStream(propertiesPath);
        Properties properties = new Properties();
        properties.load(inputStream);

        try(
                TextProcessing textProcessing = new TextProcessing(properties);
                DocumentConverter documentConverter = new DocumentConverter(properties, textProcessing);
                PostgreSQLController postgreSQLController = new PostgreSQLController(properties);) {

            Map<Long, Set<Long>> mappedDocumentIdsAndKeywordIds = new HashMap<Long, Set<Long>>(); // = //
                                                                                                  // postgreSQLController.getDocumentIdsAndMappedKeywordIds();
            int current = 1;
            for (Map.Entry<Long, Set<Long>> entry : mappedDocumentIdsAndKeywordIds.entrySet()) {
                Long metaDataId = entry.getKey();
                Set<Long> keywordIds = entry.getValue();
                List<String> keywords = new ArrayList<String>();
                Metadata metadata = postgreSQLController.getMetadata(metaDataId);
                for (Long keywordId : keywordIds) {
                    String keyword = postgreSQLController.getKeyword(keywordId);
                    keywords.add(keyword);
                }

                BodyContentHandler bodyContentHandler = documentConverter.getBodyContentHandler(getByteArrayInputStream(metadata), metadata.get("name"));
                Map<String, List<String>> textPassages = documentConverter.getTextPassages(bodyContentHandler, keywords);
                for (Map.Entry<String, List<String>> textPassage : textPassages.entrySet()) {
                    String keyword = textPassage.getKey();
                    List<String> texts = textPassage.getValue();
                    postgreSQLController.persist(metadata, keyword, new HashSet<String>(texts));

                }
                keywords.clear();
                if (current == 3)
                    return;
                System.out.println(current + " of " + mappedDocumentIdsAndKeywordIds.size());
                current++;
            }

        }

    }


    /**
     * this method is an example to run location finding on a single document edit parameters!
     *
     * @throws Exception
     */
    public static void importLocationsNew(String[] args) throws IOException, Exception {

        String propertiesPath = validateProgramArgumentOrExit(args);

        InputStream inputStream = new FileInputStream(propertiesPath);
        Properties properties = new Properties();
        properties.load(inputStream);


        try(
                TextProcessing textProcessing = new TextProcessing(properties);
                DocumentConverter documentConverter = new DocumentConverter(properties, textProcessing);
                DocumentNamedEntityAnalyser documentNamedEntityAnalyser = new DocumentNamedEntityAnalyser(properties, documentConverter);
                PostgreSQLController postgreSQLController = new PostgreSQLController(properties);) {

            List<Location> locations =  documentNamedEntityAnalyser.getLocationsBasedOnGeoNamesDump();
            postgreSQLController.importLocationsTabel(locations);


        }
    }



}
