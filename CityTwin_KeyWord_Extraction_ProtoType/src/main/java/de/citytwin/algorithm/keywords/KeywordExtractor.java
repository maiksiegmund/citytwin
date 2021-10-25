package de.citytwin.algorithm.keywords;

import java.util.List;
import java.util.Map;

/**
 * this interface provide method(s) for keyword extraction
 *
 * @author Maik, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public interface KeywordExtractor {

    /**
     * this method extract keywords from text corpus
     *
     * @param textcorpus <br>
     *            {@code List<List<String>>} <br>
     *            first list is a sentence and this contains a list of terms
     * @return {@code  Map<String, Double>} <br>
     *         key = keyword, value = score
     * @throws Exception
     */

    public Map<String, Double> getKeywords(List<List<String>> textcorpus) throws Exception;
}
