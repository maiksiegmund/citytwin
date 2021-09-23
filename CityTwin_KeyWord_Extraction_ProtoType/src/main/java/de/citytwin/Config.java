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
    private transient final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private transient static Boolean ISLISTSET = false;

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

    public transient static final String CONFIGNAME = "keywordextraction.cfg";

    public static String DATABASE_URI = "bolt://localhost:7687";
    public static String DATABASE_USER = "neo4j";
    public static String DATABASE_PASSWORD = "C1tyTw1n!";

    public static Boolean WITH_STOPWORD_FILTER = false;
    public static Boolean WITH_STEMMING = false;

    public static String WORD2VEC_MODEL = "D:\\Word2Vec.bin";
    public static Integer WORD2VEC_NEARESTCOUNT = 10;

    public static String ALKIS_RESOURCE = "alkis.json";
    public static String ONTOLOGY_RESOURCE = "ontology.json";
    public static String OUTPUT_FOLDER = "output";
    public static String INPUT_FOLDER = "D:\\vms\\sharedFolder\\";

    public static Integer TEXTRANK_WORDWINDOWSIZE = 5;
    public static Integer TEXTRANK_ITERATION = 5;
    public static Integer TEXTRANK_MAXLINKS = 3;
    public static Boolean TEXTRANK_WITH_MATRIX_NORMALIZE = false;
    public static Boolean TEXTRANK_WITH_VECTOR_NORMALIZE = false;

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
    public static String GERMAN_TEXT_PROCESSING_CLEANING_PATTERN = "[^\\u2013\\u002D\\wäÄöÖüÜß,-/]";
    public static Integer GERMAN_TEXT_PROCESSING_MAX_NEWLINES = 10;
    public static Integer GERMAN_TEXT_PROCESSING_MIN_TERM_LENGTH = 2;
    public static Integer GERMAN_TEXT_PROCESSING_MIN_TERM_COUNT = 5;

    public static Integer TF_IDF_NORMALIZATION_TYPE = 0;

    public static void save(String text) throws IOException {

        String filePath = System.getProperty("user.dir");
        String absoultePath = filePath + "\\" + CONFIGNAME;

        File resultfile = new File(absoultePath);
        BufferedWriter writer = new BufferedWriter(
                new BufferedWriter(new FileWriter(resultfile, false)));
        writer.write(text);
        writer.close();
        logger.info("saved config successful: (" + absoultePath + ")");

    }

    public static void load() throws IOException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {

        String filePath = System.getProperty("user.dir");
        String absoultePath = filePath + "\\" + CONFIGNAME;
        List<String> lines = (Files.readLines(new File(absoultePath), Charset.defaultCharset()));

        clearLists();

        for (String line : lines) {
            if (line.startsWith("#") || line.isBlank()) {
                continue;
            }
            setField(line);

        }

    }

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

    private static Field[] getStaticFields() {

        Field[] declaredFields = Config.class.getDeclaredFields();
        List<Field> staticFields = new ArrayList<Field>();
        for (Field field : declaredFields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers())) {
                staticFields.add(field);
            }
        }

        Field[] result = new Field[staticFields.size()];
        staticFields.toArray(result);

        return result;
    }

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
        if (Collection.class.isAssignableFrom(field.getType())) {

            Object tst = field.get(field);
            List lists = (List)tst;
            lists.add(rawValue.toString());

            // collection.add(value);
        } else {
            field.set(field, value);
        }

    }

    private static void clearLists() throws IllegalArgumentException, IllegalAccessException {

        Field[] fields = getStaticFields();
        for (Field field : fields) {
            if (Collection.class.isAssignableFrom(field.getType())) {

                Collection collection = (Collection<?>)field.get(field.getName());
                collection.clear();
            }
        }
    }

    public static Boolean exsit() {

        String filePath = System.getProperty("user.dir");
        String absoultePath = filePath + "\\" + CONFIGNAME;
        File file = new File(absoultePath);
        return file.exists();

    }

    public static String asString() throws IllegalArgumentException, IllegalAccessException {
        StringBuilder stringBuilder = new StringBuilder();

        Field[] staticFields = getStaticFields();
        String fieldName = "";
        Object obejct = null;
        String value = "";

        stringBuilder.append("# comment lines and empty lines are skipped");
        stringBuilder.append("\n");
        stringBuilder.append("# TF_IDF_NORMALIZATION_TYPE = (0 = NONE or 1 = LOG or 2 = DOUBEL) ");
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

}
