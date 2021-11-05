package de.citytwin.algorithm;

import de.citytwin.namedentities.NamedEntitiesExtractor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;

/**
 * @author Maik Siegmund, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class LocationEntitiesExtractor implements NamedEntitiesExtractor, AutoCloseable {

    /** Aktuelle Versionsinformation */
    private static final String VERSION = "$Revision: 1.00 $";
    /** Klassenspezifischer, aktueller Logger (Server: org.apache.log4j.Logger; Client: java.util.logging.Logger) */
    private static transient final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Properties properties = null;
    private NameFinderME nameFinder = null;

    public static Properties getDefaultProperties() {
        Properties properties = new Properties();
        properties.put("path.2.ner.location.file", "..\\location-model.bin");
        properties.put("minProbability", 0.95d);
        return properties;
    }

    public LocationEntitiesExtractor(Properties properties) throws IOException {
        if (validateProperties(properties)) {
            this.properties = new Properties();
            this.properties.putAll(properties);

            initialize();
        }

    }

    private Boolean validateProperties(Properties properties) throws IOException {
        String property = (String)properties.get("path.2.ner.location.file");
        if (property == null) {
            throw new IOException("set property --> path.2.ner.location.file as String");
        }
        Double value = (Double)properties.get("minProbability");
        if (value == null) {
            throw new IOException("set property --> minProbability as Double");
        }
        return true;

    }

    private void initialize() throws IOException {

        String property = "";
        property = properties.getProperty("path.2.ner.location.file");
        try(InputStream inputStream = new FileInputStream(property);) {
            TokenNameFinderModel tokenNameFinderModel = new TokenNameFinderModel(inputStream);
            nameFinder = new NameFinderME(tokenNameFinderModel);
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

    @Override
    public Set<String> getNamedEntities(List<List<String>> textcorpus) throws Exception {
        Set<String> results = new HashSet<String>();
        Double minProbability = (Double)properties.get("minProbability");
        String location = "";

        for (List<String> sentence : textcorpus) {

            String[] tempArray = new String[sentence.size()];
            sentence.toArray(tempArray);
            // span holds index of term in current sentence
            Span spans[] = nameFinder.find(tempArray);
            for (Span span : spans) {
                if (span.getProb() < minProbability) {
                    continue;
                }

                location = "";
                for (int index = span.getStart(); index < span.getEnd(); ++index) {
                    location += tempArray[index] + " ";
                }
                results.add(location);
            }

        }
        nameFinder.clearAdaptiveData();

        return results;
    }

    @Override
    public void close() throws Exception {
        properties.clear();
        properties = null;
        nameFinder = null;

    }
}
