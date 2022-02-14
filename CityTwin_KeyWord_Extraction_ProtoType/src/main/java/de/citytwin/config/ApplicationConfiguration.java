package de.citytwin.config;

import de.citytwin.algorithm.keywords.TextRankKeywordExtractor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;

public class ApplicationConfiguration {

    /** path to pretrained location finding model {@link NameFinderME} */
    public static final String PATH_2_NER_LOCATION_FILE = "path.2.ner.location.file";
    /** minium required probability to add a founded result */
    public static final String MIN_PROBABILITY = "min.probability";

    /** how many terms have to contains a sentence */
    public final static String MIN_TERM_COUNT = "min.term.count";
    public final static String WORD_WINDOW_SIZE = "word.window.size";
    public final static String ITERATION = "iteration";
    public final static String WITH_VECTOR_NORMALIZATION = "with.vector.normalization";
    /** remove stopwords? */
    public static final String WITH_STOPWORDFILTER = "with.stopword.filter";
    /** use stemming */
    public static final String WITH_STEMMING = "with.stemming";
    /** which type of mathematically normalization (none, log, double) */
    public static final String NORMALIZATION_TYPE = "normalization";
    /** path to pretrained word2vec model {@link org.deeplearning4j.models.word2vec.Word2Vec} */
    public static final String PATH_2_WORD_2_VEC_FILE = "path.2.word2vec.file";
    /** similarity in percent */
    public static final String SIMILARITY = "similarity";
    /** how many nearest words, simultaneously maximum value */
    public static final String MAX_NEAREST = "max.nearest";
    /** maximum new lines sequences in a line */
    public static final String MAX_NEW_LINES = "max.newlines";
    /** which signs will be remove of term */
    public static final String CLEANING_PATTERN = "cleaning.pattern";
    /** minimum of term length */
    public static final String MIN_TERM_LENGTH = "min.term.length";
    /** threshold in percent */
    public static final String MIN_TABLE_OF_CONTENT = "min.table.of.Content";
    /** path to pretrained location finding model {@link SentenceDetectorME} */
    public static final String PATH_2_SENTENCE_DETECTOR_FILE = "path.2.sentence.detector.file";
    /** path to pretrained location finding model {@link POSTaggerME} */
    public static final String PATH_2_POS_TAGGER_FILE = "path.2.pos-tagger.file";
    /** path to pretrained location finding model {@link Tokenizer} */
    public static final String PATH_2_SENTENCE_TOKENIZER_FILE = "path.2.sentence.tokenizer.file";
    /** path to stopwordlist */
    public static final String PATH_2_STOPWORDS_FILE = "path.2.stopwords.file";
    /** path to postaglist (german) */
    public static final String PATH_2_POSTAGS_FILE = "path.2.postags.file";
    /** algorithm e.g tf-idf, textRank, full package / class name {@link TextRankKeywordExtractor} */
    public static final String KEYWORD_EXTRACTOR_ALGORITHMS = "keyword.extractor.algorithms";
    /** alkis, ct_terms */
    public static final String KEYWORD_FILTER_CATALOGS = "keyword.filter.catalogs";
    public static final String PATH_2_ALKIS_CATALOG_FILE = "path.2.alkis.catalog.file";
    public static final String PATH_2_Term_CATALOG_FILE = "path.2.Term.catalog.file";
    /** path to the analyze documents */
    public static final String DOCUMENTS_FOLDER = "documents.folder";
    /** path to output folder */
    public static final String OUTPUT_FOLDER = "output.folder";

    /** database */
    public static final String NEO4J_URI = "neo4j.uri";
    public static final String NEO4J_USER = "neo4j.user";
    public static final String NEO4J_PASSWORD = "neo4j.password";

    public static final String POSTGRESQL_URL = "postgreSQL.url";
    public static final String PATH_2_POSTGRESQL_PROPERTY_FILE = "path.2.postgreSQL.propertiy.file";

    /** result stored in neo4j, json, postgresql */
    public static final String RESULT_2_JSON = "result.2.json";
    public static final String RESULT_2_NEO4J = "result.2.neo4j";

    public static final String RESULT_2_POSTGRESQL = "result.2.postgreSQL";

    public static final String GEONAMES_WEBSERVICE = "geonames.api";
    public static final String GEONAMES_URL_2_DUMP_FILE = "geonames.url.2.dump.file";
    public static final String GEONAMES_ZIP_ENTRY = "geonames.zip.entry";

    public static final String ORIGIN_LOCATION_NAME = "origin.location.name";
    public static final String ORIGIN_LOCATION_LATITUDE = "origin.location.latitude";
    public static final String ORIGIN_LOCATION_LONGITUDE = "origin.location.longitude";

    public static final String GEONAMES_USER = "geonames.user";
    public static final String GEONAMES_COUNTRYCODE = "geonames.CountryCode";
    public static final String GEONAMES_MAXROWS = "geonames.maxrows";

    public static final String MAX_DISTANCE_IN_METERS = "max.distance.in.meters";
    public static final String MAX_STREET_COUNT = "max.street.count";
    public static final String MIN_NAMED_ENTITY_LENGTH = "min.named.entity.length";
    public static final String MAX_SECTION_COUNT = "max.section.count";

    /**
     * this methods return a properties with all keys, and empty (String) value
     *
     * @return new instance of {@code Properties}
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public static Properties getEmptyProperties() throws IllegalArgumentException, IllegalAccessException {
        Properties properties = new Properties();
        Field[] declaredFields = ApplicationConfiguration.class.getDeclaredFields();
        List<Field> fields = new ArrayList<Field>();
        for (Field field : declaredFields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                properties.setProperty(field.get(fields).toString(), "");
            }
        }
        return properties;
    }

    private ApplicationConfiguration() {
    }
}
