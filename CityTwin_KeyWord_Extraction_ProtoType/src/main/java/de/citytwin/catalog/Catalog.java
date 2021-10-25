package de.citytwin.catalog;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.citytwin.converter.DocumentConverter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * this class is a subject specific catalog
 *
 * @author Maik, SRP GmbH, Berlin
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 * @param <T>
 */
public class Catalog<T extends CatalogEntryHasName> implements AutoCloseable {

    /**
     * this method return default properties
     *
     * @param <T>
     * @param clazz
     * @return
     */
    public static <T> Properties getDefaultProperties(Class<?> clazz) {
        Properties properties = new Properties();
        properties.put("path.2." + clazz.getName() + "catalog.file", "..\\t_catalog.json");
        return properties;
    }

    private Properties properties = null;
    private Map<String, T> catalog = null;

    private String className = "";

    /**
     * constructor.
     *
     * @param properties = {@code Catalog.getDefaultProperties(T.class)}
     * @param clazz
     * @throws IOException
     */
    public Catalog(Properties properties, Class<T> clazz) throws IOException {
        if (validateProperties(properties)) {
            initialize();
            className = clazz.getName();
        }

    }

    @Override
    public void close() throws Exception {
        catalog.clear();
        catalog = null;

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
     * this method initialize the catalog
     *
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    private void initialize() throws JsonParseException, JsonMappingException, IOException {
        String path = (String)properties.get("path.2." + className + "catalog.file");
        catalog = DocumentConverter.getDTOs(new TypeReference<HashMap<String, T>>() {}, path);
    }

    /**
     * this method validate the passing properties
     *
     * @param properties
     * @return
     * @throws IOException
     */
    private Boolean validateProperties(Properties properties) throws IOException {

        String property = (String)properties.get("path.2." + className + "catalog.file");
        if (property == null) {
            throw new IOException("set property --> path.2.catalog.file as String");
        }
        return true;
    }
}
