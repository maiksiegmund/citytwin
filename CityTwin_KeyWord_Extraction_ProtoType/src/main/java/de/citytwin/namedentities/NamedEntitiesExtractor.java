package de.citytwin.namedentities;

import java.util.List;
import java.util.Set;

/**
 * this interface provide method(s) for named entities recognition
 *
 * @author Maik Siegmund, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public interface NamedEntitiesExtractor {

    /**
     * this method extract named entities from text corpus
     *
     * @param textcorpus {@code List<List<String>>} <br>
     *            first list is a sentence and this contains a list of terms
     * @return new reference {@code Set<String>}
     * @throws Exception
     */

    public Set<String> getNamedEntities(List<List<String>> textcorpus) throws Exception;
}
