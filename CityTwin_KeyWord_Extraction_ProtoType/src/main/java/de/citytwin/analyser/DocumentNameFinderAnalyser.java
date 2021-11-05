package de.citytwin.analyser;

import de.citytwin.converter.DocumentConverter;
import de.citytwin.namedentities.NamedEntitiesExtractor;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * * this class is a named entities extractor <br>
 *
 * @author Maik Siegmund, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class DocumentNameFinderAnalyser implements NamedEntities, AutoCloseable {

    /** current version information */
    private static final String VERSION = "$Revision: 1.00 $";
    /** logger */
    private static final transient Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private DocumentConverter documentConverter = null;

    /**
     * constructor
     *
     * @param properties
     * @throws IOException
     */
    public DocumentNameFinderAnalyser(DocumentConverter documentConverter) throws IOException {

        // if (validateProperties(properties)) {
        // this.properties = new Properties();
        // this.properties.putAll(properties);
        // }
        this.documentConverter = documentConverter;

    }

    /**
     * this method return default properties
     *
     * @return
     */
    // public static Properties getDefaultProperties() {
    // Properties properties = new Properties();
    // properties.put("minThresHold", 0.5d);
    // return properties;
    //
    // }

    /**
     * this method validate passing properties
     *
     * @param properties
     * @return
     * @throws IOException
     */
    private Boolean validateProperties(Properties properties) throws IOException {

        Double value = (Double)properties.get("minThresHold");
        if (value == null) {
            throw new IOException("set property --> minThresHold as Double");
        }
        return true;
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
    public void close() throws Exception {
        // this.properties.clear();
        // this.properties = null;
        this.documentConverter = null;

    }

    @Override
    public Set<String> getNamedEntities(File file, NamedEntitiesExtractor namedEntitiesExtractor) throws Exception {

        BodyContentHandler bodyContentHandler = documentConverter.getBodyContentHandler(file);
        List<List<String>> textcorpus = documentConverter.getCleanedTextCorpus(bodyContentHandler);
        return namedEntitiesExtractor.getNamedEntities(textcorpus);

    }

}
