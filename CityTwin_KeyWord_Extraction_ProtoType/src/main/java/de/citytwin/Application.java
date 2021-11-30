package de.citytwin;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * main
 *
 * @author Maik Siegmund, FH Erfurt
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
            // Example.doLocationFinding();
            // Example.createPartOfCityGraph();
            // 1. Example.storePreparedProperties(new File("application.properties"));
            /** set program argument -p documentInformationRetrieval.properties */
            // 2. Example.createPartOfCityGraph(args);

        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }

    }

}
