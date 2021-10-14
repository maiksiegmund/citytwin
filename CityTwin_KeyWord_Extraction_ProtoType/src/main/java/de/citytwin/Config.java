package de.citytwin;

import com.google.common.io.Files;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: properties-File als Konfiguration nutzen
 * 
 * this class represent default config
 *
 * @author Maik, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class Config {

    /** Aktuelle Versionsinformation */
    /**  */
    private transient static final String VERSION = "$Revision: 1.00 $";
    /** Klassenspezifischer, aktueller Logger (Server: org.apache.log4j.Logger; Client: java.util.logging.Logger) */
    private transient static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public transient static final String CONFIGNAME = "documentAnalyser.cfg";

    // all public static members are saved and load
    public static String DATABASE_URI = "bolt://localhost:7687";

    public static String DATABASE_USER = "neo4j";
    public static String DATABASE_PASSWORD = "C1tyTw1n!";
    public static Boolean WITH_STOPWORD_FILTER = false;
    public static String STOPWORD_CATALOG = "D:\\keywordAnalyser\\stopswords_de.txt";

    public static Boolean WITH_STEMMING = false;
    public static String WORD2VEC_MODEL = "D:\\keywordAnalyser\\word2vectestModel.bin";

    public static Integer WORD2VEC_NEARESTCOUNT = 10;
    public static Integer WORD2VEC_SIMILARITY = 66;

    public static String ALKIS_CATALOG = "D:\\keywordAnalyser\\alkis_catalog.json";
    public static String TERM_CATALOG = "D:\\keywordAnalyser\\ct_terms_catalog.json";
    public static String NLP_POS_TAGGER = "D:\\keywordAnalyser\\de-pos-perceptron.bin";
    public static String NLP_SENTENCE_DETECTOR = "D:\\keywordAnalyser\\de-sent.bin";
    public static String NLP_SENTENCE_TOKENIZER = "D:\\keywordAnalyser\\de-token.bin";

    public static String OUTPUT_FOLDER = "D:\\keywordAnalyser\\";
    public static String INPUT_FOLDER = "D:\\keywordAnalyser\\files";

    public static TF_IDF_NormalizationType TF_IDF_NORMALIZATION_TYPE = TF_IDF_NormalizationType.NONE;
    public static Boolean TTF_IDF_Results = true;

    public static Integer TEXTRANK_WORDWINDOWSIZE = 5;
    public static Integer TEXTRANK_ITERATION = 5;
    public static Integer TEXTRANK_MAXLINKS = 3;
    public static Boolean TEXTRANK_WITH_MATRIX_NORMALIZE = false;
    public static Boolean TEXTRANK_WITH_VECTOR_NORMALIZE = false;
    public static Boolean TEXTRANK_Results = true;

    public static String GERMAN_TEXT_PROCESSING_CLEANING_PATTERN = "[^\\u2013\\u002D\\wäÄöÖüÜß,-/]";
    public static Integer GERMAN_TEXT_PROCESSING_MAX_NEWLINES = 10;
    public static Integer GERMAN_TEXT_PROCESSING_MIN_TERM_LENGTH = 2;
    public static Integer GERMAN_TEXT_PROCESSING_MIN_TERM_COUNT = 5;
    public static Integer GERMAN_TEXT_PROCESSING_TABLE_OF_CONTENT_THRESHOLD = 50;

    public static List<String> GERMAN_TEXT_PROCESSING_POSTAGS = new ArrayList<String>(Arrays.asList("ADJA",
            "ADJD",
            "ADV",
            "APPR",
            "APPRART",
            "APPO",
            "APZR",
            "ART",
            "CARD",
            "FM",
            "ITJ",
            "KOUI",
            "KOUS",
            "KON",
            "KOKOM",
            "NN",
            "NE",
            "PDS",
            "PDAT",
            "PIS",
            "PIAT",
            "PIDAT",
            "PPER",
            "PPOSAT",
            "PRELS",
            "PRELAT",
            "PRF",
            "PWS",
            "PWAT",
            "PWAV",
            "PAV",
            "PTKZU",
            "PTKNEG",
            "PTKVZ",
            "PTKANT",
            "PTKA",
            "TRUNC",
            "VVFIN",
            "VVIMP",
            "VVINF",
            "VVIZU",
            "VVPP",
            "VAFIN",
            "VAIMP",
            "VAINF",
            "VAPP",
            "VMFIN",
            "VMINF",
            "VMPP",
            "XY"));

    /**
     * this method clear default list, to avoid double entries and load entries from a file
     *
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    private static void clearLists() throws IllegalArgumentException, IllegalAccessException {

        Field[] fields = getStaticAndNonTransientFields();
        for (Field field : fields) {
            if (Collection.class.isAssignableFrom(field.getType())) {
                Collection collection = (Collection<?>)field.get(field.getName());
                collection.clear();
            }
        }
    }

    /**
     * this method serials current config parameter
     *
     * @return {@code String}
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public static String getConfigContent() throws IllegalArgumentException, IllegalAccessException {
        StringBuilder stringBuilder = new StringBuilder();

        Field[] staticFields = getStaticAndNonTransientFields();
        String fieldName = "";
        Object obejct = null;
        String value = "";

        stringBuilder.append("# comment lines and empty lines are skipped");
        stringBuilder.append("\n");
        stringBuilder.append("# TF_IDF_NORMALIZATION_TYPE = NONE or LOG or DOUBLE) ");
        stringBuilder.append("\n");

        for (Field staticField : staticFields) {
            // List
            if (Collection.class.isAssignableFrom(staticField.getType())) {
                stringBuilder.append(serialsList(staticField));
                continue;
            }
            fieldName = staticField.getName();
            obejct = staticField.get(fieldName);
            value = obejct.toString();
            stringBuilder.append(MessageFormat.format("{0} = {1}", fieldName, value));
            stringBuilder.append("\n");

        }

        return stringBuilder.toString();

    }

    /**
     * this method returns all saveable config parameters
     *
     * @return
     */
    private static Field[] getStaticAndNonTransientFields() {

        Field[] declaredFields = Config.class.getDeclaredFields();
        List<Field> fields = new ArrayList<Field>();
        for (Field field : declaredFields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers())) {
                fields.add(field);
            }
        }

        Field[] result = new Field[fields.size()];
        fields.toArray(result);

        return result;
    }

    /**
     * this method load a config file
     *
     * @param File
     * @throws IOException
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public static void load(final File file) throws IOException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {

        List<String> lines = (Files.readLines(file, Charset.defaultCharset()));

        clearLists();

        for (String line : lines) {
            if (line.startsWith("#") || line.isBlank()) {
                continue;
            }
            setField(line);

        }
        logger.info("load config successful: (" + file.getAbsolutePath() + ")");
    }

    /**
     * this method save current config-state in current running executionfolder as File "documentAnalyser.cfg"
     *
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public static void save(File file) throws IOException, IllegalArgumentException, IllegalAccessException {

        // TODO consider using try with resources instead
        BufferedWriter writer = new BufferedWriter(
                new BufferedWriter(new FileWriter(file, false)));
        writer.write(getConfigContent());
        writer.close();
        logger.info("saved config successful: (" + file.getAbsolutePath() + ")");

    }

    /**
     * this method serials a list, with fieldname and value
     *
     * @param field
     * @return String
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    private static String serialsList(Field field) throws IllegalArgumentException, IllegalAccessException {

        String fieldName = field.getName();
        Object object = field.get(fieldName);

        StringBuilder stringBuilder = new StringBuilder();
        List<?> values = new ArrayList<>((Collection<?>)object);

        for (Object value : values) {
            stringBuilder.append(MessageFormat.format("{0} = {1}", fieldName, value));
            stringBuilder.append("\n");
        }

        return stringBuilder.toString();

    }

    /**
     * this method set field value
     *
     * @param line (contains name of field and value)
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    private static void setField(String line)
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        String[] parts = line.split("=");
        Object fieldName = parts[0].trim();
        Object rawValue = parts[1].trim();
        Object value = null;

        Field field = Config.class.getDeclaredField(fieldName.toString());
        field.setAccessible(true);

        if (field.getType() == Double.class) {
            value = Double.parseDouble(rawValue.toString());
        }
        if (field.getType() == Boolean.class) {
            value = Boolean.parseBoolean(rawValue.toString());
        }
        if (field.getType() == Integer.class) {
            value = Integer.parseInt(rawValue.toString());
        }
        if (field.getType() == String.class) {
            value = rawValue.toString();
        }
        if (field.getType().isEnum()) {
            // to do, change to generic
            Object object = field.get(field);
            Enum<?> enuum = (Enum<?>)object;
            value = Enum.valueOf(enuum.getClass(), rawValue.toString());
            // value = TF_IDF_NormalizationType.valueOf(rawValue.toString());

        }
        if (Collection.class.isAssignableFrom(field.getType())) {

            Object object = field.get(field);
            List list = (List)object;
            list.add(rawValue.toString());

            // collection.add(value);
        } else {
            field.set(field, value);
        }

    }

    // TODO parameterize path and if not given using value from configuration?
    public static String Source() {
        String filePath = System.getProperty("user.dir");
        String absoultePath = filePath + "\\" + CONFIGNAME;
        return absoultePath;
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

/**
 * This enum to determines which normalization algorithm is use ...
 * <p>
 *
 * @see <a href=https://en.wikipedia.org/wiki/Tf%E2%80%93idf> tf idf calculation on wikipedia</a>
 */
enum TF_IDF_NormalizationType {
    NONE, LOG, DOUBLE
}
