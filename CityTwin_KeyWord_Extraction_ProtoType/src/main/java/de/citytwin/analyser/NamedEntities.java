package de.citytwin.analyser;

import de.citytwin.namedentities.NamedEntitiesExtractor;

import java.io.ByteArrayInputStream;
import java.util.Set;

/**
 * this interface provides methods for named entities extraction
 *
 * @author Maik Siegmund, FH Erfurt
 */
public interface NamedEntities {

    /**
     * this method extract named entities from a file
     *
     * @param byteArrayInputStream
     * @param fileName
     * @param namedEntitiesExtractor
     * @return
     * @throws Exception
     */
    public Set<String> getNamedEntities(
            final ByteArrayInputStream byteArrayInputStream, final String fileName, NamedEntitiesExtractor namedEntitiesExtractor)
            throws Exception;

}
