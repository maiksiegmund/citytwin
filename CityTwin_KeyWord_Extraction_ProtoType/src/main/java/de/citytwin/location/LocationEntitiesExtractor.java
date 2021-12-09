package de.citytwin.location;

import de.citytwin.config.ApplicationConfiguration;
import de.citytwin.namedentities.NamedEntitiesExtractor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
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
 */
public class LocationEntitiesExtractor implements NamedEntitiesExtractor, AutoCloseable {

    /** Klassenspezifischer, aktueller Logger (Server: org.apache.log4j.Logger; Client: java.util.logging.Logger) */
    private static transient final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * this method return default properties
     *
     * @return new reference of {@code Properties}
     */
    public static Properties getDefaultProperties() {
        Properties properties = new Properties();
        properties.setProperty(ApplicationConfiguration.PATH_2_NER_LOCATION_FILE, "..\\location-model.bin");
        properties.setProperty(ApplicationConfiguration.MIN_PROBABILITY, "0.95d");
        return properties;
    }

    private NameFinderME nameFinder = null;
    private Double minProbability = null;

    private String path2NerLocationFile = null;

    /**
     * constructor
     *
     * @param properties
     * @throws Exception
     */
    public LocationEntitiesExtractor(Properties properties) throws Exception {
        if (validateProperties(properties)) {
            initialize();
        }

    }

    @Override
    public void close() throws Exception {
        nameFinder = null;

    }

    @Override
    public Set<String> getNamedEntities(List<List<String>> textcorpus) {
        Set<String> results = new HashSet<String>();
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
        LOGGER.info(MessageFormat.format("locations found:    {0}", results.size()));
        return results;
    }

    /**
     * initialize class components
     *
     * @throws IOException
     */
    private void initialize() throws IOException {

        try(InputStream inputStream = new FileInputStream(path2NerLocationFile);) {
            TokenNameFinderModel tokenNameFinderModel = new TokenNameFinderModel(inputStream);
            nameFinder = new NameFinderME(tokenNameFinderModel);
        }

    }

    /**
     * checks properties and set them
     *
     * @param properties
     * @return boolean
     * @throws IllegalArgumentException
     */
    private Boolean validateProperties(Properties properties) throws IllegalArgumentException {
        path2NerLocationFile = properties.getProperty(ApplicationConfiguration.PATH_2_NER_LOCATION_FILE);
        if (path2NerLocationFile == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.PATH_2_NER_LOCATION_FILE);
        }
        String property = properties.getProperty(ApplicationConfiguration.MIN_PROBABILITY);
        if (property == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.MIN_PROBABILITY);
        }
        minProbability = Double.parseDouble(property);
        return true;
    }



}
