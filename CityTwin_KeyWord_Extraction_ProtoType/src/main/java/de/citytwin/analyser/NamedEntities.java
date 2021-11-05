package de.citytwin.analyser;

import de.citytwin.namedentities.NamedEntitiesExtractor;

import java.io.File;
import java.util.Set;

public interface NamedEntities {

    /**
     * this method extract named entities from a file
     *
     * @param file
     * @param keywordExtractor
     * @return
     * @throws Exception
     */
    public Set<String> getNamedEntities(File file, NamedEntitiesExtractor namedEntitiesExtractor) throws Exception;

}
