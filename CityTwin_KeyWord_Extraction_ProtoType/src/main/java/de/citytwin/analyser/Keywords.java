package de.citytwin.analyser;

import de.citytwin.catalog.Catalog;
import de.citytwin.catalog.CatalogEntryHasName;
import de.citytwin.keywords.KeywordExtractor;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * this interface provides methods for keyword extraction and filtering
 *
 * @author Maik Siegmund, FH Erfurt
 */
public interface Keywords {

    /**
     * this method filter keywords by a catalog
     *
     * @param keywords
     * @param catalog
     * @return
     * @throws IOException
     */
    public Map<String, Double> filterKeywords(Map<String, Double> keywords, Catalog<? extends CatalogEntryHasName> catalog) throws Exception;

    /**
     * this method extract keywords from a file
     *
     * @param file
     * @param keywordExtractor
     * @return
     * @throws Exception
     */
    public Map<String, Double> getKeywords(File file, KeywordExtractor keywordExtractor) throws Exception;
}
