package de.citytwin;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

/**
 * @author Maik, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
/**
 * @author Maik, SRP GmbH, Berlin
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
/**
 * @author Maik, SRP GmbH, Berlin
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class TextRankAnalyser {

    /** Aktuelle Versionsinformation */
    private static final String VERSION = "$Revision: 1.00 $";
    private StanfordCoreNLP pipeline;

    private String language = "german";
    /** Klassenspezifischer, aktueller Logger (Server: org.apache.log4j.Logger; Client: java.util.logging.Logger) */
    private transient final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public TextRankAnalyser() {
        initialize();
    }

    /**
     * @param text
     */
    public void getTextRank(String text) {
        Annotation annotation = pipeline.process(text);
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            List<CoreLabel> sentiments = sentence.get(CoreAnnotations.TokensAnnotation.class);
            for (CoreLabel sentiment : sentiments) {
                sentiment.toString();
            }
        }
    }

    /**
     *
     */
    private void initialize() {
        this.pipeline = new StanfordCoreNLP(language);

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

}
