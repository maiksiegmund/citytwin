package de.citytwin;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

import javax.annotation.Nonnull;

import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Maik Siegmund, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class DocumentAnalyser {

    /** Aktuelle Versionsinformation */
    private static final String VERSION = "$Revision: 1.00 $";
    /** Klassenspezifischer, aktueller Logger (Server: org.apache.log4j.Logger; Client: java.util.logging.Logger) */
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected TextRankAnalyser textRankAnalyser = null;
    protected TFIDFTextAnalyser tfIdfAnalyser = null;
    protected Word2VecAnalyser word2vecAnalyser = null;
    protected DocumentConverter documentConverter = null;
    protected boolean isBuilt = false;
    private BodyContentHandler bodyContentHandler = null;
    private GermanTextProcessing germanTextProcessing = null;

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

    public void analyseDocument(final File file) throws IOException {
        if (!isBuilt) {
            throw new IOException("perform ... build()");
        }



    }

    private void checkAlkis() {

    }

    public static class Builder {

        private boolean withOpenNLP = false;
        private boolean withStopwordFilter = false;
        private boolean withStemmening = false;
        private String pathToModel = "";

        public Builder withOpenNLP() {
            this.withOpenNLP = true;
            return this;

        }

        public Builder withLucene() {
            this.withOpenNLP = false;
            return this;
        }

        public Builder withStopwordFilter() {
            withStopwordFilter = true;
            return this;
        }

        public Builder Model(@Nonnull String pathToModel) {
            this.pathToModel = pathToModel;
            return this;
        }

        public DocumentAnalyser build() throws Exception {

            DocumentAnalyser documentAnalyser = new DocumentAnalyser();
            documentAnalyser.textRankAnalyser = (this.withOpenNLP) ? new TextRankAnalyser().withOpenNLP() : new TextRankAnalyser().withOpenNLP();
            documentAnalyser.tfIdfAnalyser = new TFIDFTextAnalyser();
            documentAnalyser.tfIdfAnalyser.setWithOpenNLP(withOpenNLP);
            documentAnalyser.tfIdfAnalyser.setWithStopWordFilter(withStopwordFilter);
            documentAnalyser.tfIdfAnalyser.setWithStemming(withStemmening);
            documentAnalyser.word2vecAnalyser = new Word2VecAnalyser().withModel(pathToModel);
            documentAnalyser.documentConverter = new DocumentConverter();
            documentAnalyser.isBuilt = true;
            return documentAnalyser;

        }

    }

}
