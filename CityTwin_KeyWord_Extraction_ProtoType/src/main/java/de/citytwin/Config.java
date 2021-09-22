package de.citytwin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
    private transient final static String SEPARATOR = ";";

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

    public static void save() throws IllegalArgumentException, IllegalAccessException, IOException {

        StringBuilder stringBuilder = new StringBuilder();
        String filePath = System.getProperty("user.dir");
        String absoultePath = filePath + "\\" + CONFIGNAME;
        logger.info("save config to: (" + absoultePath + ")");

        Field[] staticFields = getStaticFields();
        String fieldName = "";
        Object obejct = null;
        String value = "";
        for (Field staticField : staticFields) {
            fieldName = staticField.getName();
            obejct = staticField.get(fieldName);
            value = obejct.toString();
            // List
            if (Collection.class.isAssignableFrom(staticField.getType())) {
                value = serialsList(obejct);
            }

            stringBuilder.append(MessageFormat.format("{0} = {1}", fieldName, value));
            stringBuilder.append("\n");

        }
        File resultfile = new File(absoultePath);
        BufferedWriter writer = new BufferedWriter(
                new BufferedWriter(new FileWriter(resultfile, false)));
        writer.write(stringBuilder.toString());
        writer.close();

    }

    private static String serialsList(Object object) {
        String result = "";
        StringBuilder stringBuilder = new StringBuilder();
        List<?> values = new ArrayList<>((Collection<?>)object);

        for (Object value : values) {
            stringBuilder.append(value.toString() + SEPARATOR);
            stringBuilder.append("\n");
            stringBuilder.append("\t");
            stringBuilder.append("\t");

        }
        result = stringBuilder.toString();
        return result.substring(0, result.length() - 2);

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

}
