package de.citytwin.catalog;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.citytwin.config.ApplicationConfiguration;
import de.citytwin.converter.DocumentConverter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * this class is a subject specific catalog
 *
 * @author Maik Siegmund, FH Erfurt
 * @param <T>
 */
public class Catalog<T extends CatalogEntryHasName> implements AutoCloseable {

    private static final String classSimpleName = "Term";

    /**
     * this method return default properties
     *
     * @param <T>
     * @param clazz
     * @return
     */
    public static <T> Properties getDefaultProperties(Class<?> clazz) {
        Properties properties = new Properties();
        // hard coded
        if (clazz.getSimpleName().equals(classSimpleName)) {
            properties.put(ApplicationConfiguration.PATH_2_Term_CATALOG_FILE, "..\\Term_catalog.json");

        } else {
            properties.put(ApplicationConfiguration.PATH_2_ALKIS_CATALOG_FILE, "..\\ALKIS_catalog.json");

        }
        return properties;
    }

    private Map<String, T> catalog = null;
    private String path2catalogFile = null;
    private Class<T> clazz = null;

    /**
     * constructor.
     *
     * @param properties = {@code Catalog.getDefaultProperties(T.class)}
     * @param clazz
     * @throws IOException
     */
    public Catalog(Properties properties, Class<T> clazz) throws IOException {
        this.clazz = clazz;
        if (validateProperties(properties)) {
            initialize();

        }

    }

    @Override
    public void close() throws Exception {
        catalog.clear();
        catalog = null;

    }

    /**
     * this method check if the catalog contains the value
     *
     * @param name
     * @return
     */
    public Boolean contains(String name) {
        return catalog.containsKey(name);

    }

    /**
     * this method return a entry or null
     *
     * @param name
     * @return
     */
    public CatalogEntryHasName getEntry(String name) {
        return catalog.get(name);
    }

    /**
     * this methods return the keys
     *
     * @return
     */
    public Set<String> getNames() {
        return this.catalog.keySet();
    }

    /**
     * this method initialize the catalog
     *
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    private void initialize() throws JsonParseException, JsonMappingException, IOException {
        List<T> temps = DocumentConverter.getObjects(new TypeReference<List<T>>() {}, path2catalogFile);
        catalog = new HashMap<String, T>();
        for (T t : temps) {
            catalog.put(t.getName(), t);
        }
    }

    /**
     * this method validate the passing properties and set them
     *
     * @param properties
     * @return
     * @throws IOException
     */
    public Boolean validateProperties(Properties properties) throws IllegalArgumentException {
        // hard coded type information
        String property = (clazz.getSimpleName().equals(classSimpleName)) ? ApplicationConfiguration.PATH_2_Term_CATALOG_FILE
                : ApplicationConfiguration.PATH_2_ALKIS_CATALOG_FILE;
        path2catalogFile = properties.getProperty(property);
        if (path2catalogFile == null) {
            throw new IllegalArgumentException(
                    "set property --> " + "ApplicationConfiguration.PATH_2_Term_CATALOG_FILE" + " or " + "ApplicationConfiguration.PATH_2_ALKIS_CATALOG_FILE");
        }
        return true;
    }
}
