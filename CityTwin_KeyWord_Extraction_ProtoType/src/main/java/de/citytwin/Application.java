package de.citytwin;

import de.citytwin.example.Example;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * main
 *
 * @author Maik Siegmund, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void main(String[] args) {

        try {

            // Example.trainWord2Vec();
            // Example.doDocumentAnalyse();
            // Example.runTFIDF();
            // Example.runTextRank();
            // Example.saveAnalyseResult();
            Example.doLocationFinding();

        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }

    }

}
