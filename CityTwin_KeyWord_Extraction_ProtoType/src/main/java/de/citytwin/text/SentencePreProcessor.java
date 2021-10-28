package de.citytwin.text;

/**
 * this class implements {@link org.deeplearning4j.text.sentenceiterator.SentencePreProcessor}
 *
 * @author Maik Siegmund, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class SentencePreProcessor implements org.deeplearning4j.text.sentenceiterator.SentencePreProcessor {

    private static final long serialVersionUID = 1L;

    // sentence already prepared
    @Override
    public String preProcess(String sentence) {
        return sentence;
    }
}
