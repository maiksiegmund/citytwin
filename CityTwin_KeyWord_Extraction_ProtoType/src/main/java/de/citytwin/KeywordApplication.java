package de.citytwin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.tika.sax.BodyContentHandler;
import org.javatuples.Quartet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeywordApplication {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String OUTPUT_FOLDER = "output";
    private static final String INPUT_FOLDER = "D:\\vms\\sharedFolder\\";

    /**
     * this method calculate textRank score an return theme in a stringbuilder
     *
     * @param file
     * @return new reference of {@code StringBuilder}
     */
    private static StringBuilder calculateTextRank(File file) {

        StringBuilder stringBuilder = new StringBuilder();
        try {
            DocumentConverter documentConverter = new DocumentConverter(file);
            BodyContentHandler bodyContentHandler = documentConverter.getBodyContentHandler();
            TextRankAnalyser textRankAnalyser = new TextRankAnalyser().withOpenNLP();
            Formatter formatter = new Formatter(stringBuilder, Locale.GERMAN);

            formatter.format("%1$35s --> %2$s11",
                    "term",
                    "score");
            stringBuilder.append("\n");

            Map<String, Double> result = textRankAnalyser.calculateTextRank(bodyContentHandler, 5, 35);

            for (String key : result.keySet()) {
                formatter.format("%1$35s --> %2$.13f",
                        key,
                        result.get(key));
                stringBuilder.append("\n");

            }
            return stringBuilder;

        } catch (Exception exception) {

            logger.error(exception.getMessage(), exception);
        }
        return stringBuilder;
    }

    /**
     * this method calculate tfidf score an return theme in a stringbuilder
     *
     * @param {@code File}
     * @param {@code TFIDFTextAnalyser}
     * @return {@code StringBuilder}
     */
    private static StringBuilder calculateTFIDF(File file, TFIDFTextAnalyser tfidfTextAnalyser) {

        StringBuilder stringBuilder = new StringBuilder();
        Formatter formatter = new Formatter(stringBuilder, Locale.GERMAN);
        BodyContentHandler bodyContentHandler = null;

        Map<String, Quartet<Integer, Double, String, Set<Integer>>> results = null;
        Quartet<Integer, Double, String, Set<Integer>> quartet = null;

        try {

            DocumentConverter documentConverter = new DocumentConverter(file);
            bodyContentHandler = documentConverter.getBodyContentHandler();
            results = tfidfTextAnalyser.calculateTFIDF(bodyContentHandler,
                    GermanTextProcessing.getPosTagList(),
                    TFIDFTextAnalyser.NormalizationType.NONE);
            LocalDateTime endTime = LocalDateTime.now();
            // caption
            formatter.format(" %1$35s --> %2$11s", "term", "TFIDF Score");
            stringBuilder.append("\n");

            for (String key : results.keySet()) {
                quartet = results.get(key);
                formatter.format("%1$35s --> %2$11f", key, quartet.getValue1().doubleValue());
                stringBuilder.append("\n");
            }
            formatter.close();
        }

        catch (

        Exception e) {
            logger.error(e.getMessage(), e);
        }

        return stringBuilder;

    }

    // todo fix result merge
    public static void combineResult(int max) {

        for (File file : getFiles()) {
            try {
                StringBuilder stringBuilder = new StringBuilder();
                Formatter formatter = new Formatter(stringBuilder, Locale.GERMAN);
                TFIDFTextAnalyser tdTfidfTextAnalyser = new TFIDFTextAnalyser().withOpenNLP().withStopwordFilter();
                TextRankAnalyser textRankAnalyser = new TextRankAnalyser();
                DocumentConverter documentConverter = new DocumentConverter(file);
                BodyContentHandler bobBodyContentHandler = documentConverter.getBodyContentHandler();

                Map<String, Quartet<Integer, Double, String, Set<Integer>>> tfidfresults = tdTfidfTextAnalyser
                        .calculateTFIDF(bobBodyContentHandler, GermanTextProcessing.getPosTagList(), TFIDFTextAnalyser.NormalizationType.NONE);

                Map<String, Double> textRankResult = textRankAnalyser.calculateTextRank(bobBodyContentHandler, 4, 15);
                formatter.format("%1$35s --> %2$15s --> %3$15s",
                        "term",
                        "tfidf-score",
                        "textRank-score");
                stringBuilder.append("\n");

                Map<String, Pair<Double, Double>> combineResults = new HashMap<String, Pair<Double, Double>>();
                Double tfidfScore = 0.0d;
                Double textRankScore = 0.0d;

                for (String key : textRankResult.keySet()) {
                    Quartet<Integer, Double, String, Set<Integer>> tfidf = tfidfresults.get(key);
                    tfidfScore = (tfidf != null) ? tfidf.getValue1() : Double.NEGATIVE_INFINITY;
                    textRankScore = textRankResult.get(key);
                    combineResults.put(key, Pair.of(tfidfScore, textRankScore));
                }

                tfidfScore = 0.0d;
                textRankScore = 0.0d;

                for (String key : tfidfresults.keySet()) {
                    if (!combineResults.containsKey(key)) {
                        continue;
                    }
                    textRankScore = textRankResult.get(key);
                    tfidfScore = Double.NEGATIVE_INFINITY;
                    combineResults.put(key, Pair.of(tfidfScore, textRankScore));
                }
                int current = 0;
                for (String key : combineResults.keySet()) {
                    if (current++ > max) {
                        break;
                    }

                    tfidfScore = combineResults.get(key).getLeft();
                    textRankScore = combineResults.get(key).getRight();
                    formatter.format("%1$35s --> %2$15f --> %3$15f",
                            key,
                            tfidfScore,
                            textRankScore);
                    stringBuilder.append("\n");
                }

                System.out.println(stringBuilder.toString());

            } catch (Exception exception) {

                logger.error(exception.getMessage(), exception);
            }

        }

    }

    /**
     * @return {@code List<File>}
     */
    private static List<File> getFiles() {

        List<File> results = new ArrayList<File>();
        // results.add(new File(INPUT_FOLDER + "begruendung11-14a.pdf"));
        results.add(new File(INPUT_FOLDER + "biologische_vielfalt_strategie.docx"));
        // results.add(new File(INPUT_FOLDER + "einlegeblatt_gruenanlagensanierung.docx"));
        // results.add(new File(INPUT_FOLDER + "festsetzungbegruendung-xvii-50aa.pdf"));
        // results.add(new File(INPUT_FOLDER + "mdb-beg4b_004.pdf"));
        // results.add(new File(INPUT_FOLDER + "Stadtgrün Selbstverpflichtung.docx"));
        // results.add(new File(INPUT_FOLDER + "StEPWohnen2030-Langfassung.pdf"));
        // results.add(new File(INPUT_FOLDER + "Strategie_Smart_City_Berlin.pdf"));
        // results.add(new File(INPUT_FOLDER + "Strategie-Stadtlandschaft-Berlin.pdf"));
        // results.add(new File(INPUT_FOLDER + "UVPG.pdf"));
        // results.add(new File(INPUT_FOLDER + "Wasseratlas.pdf"));
        // results.add(new File(INPUT_FOLDER + "xix-58a_begruendung.pdf"));

        return results;

    }

    /**
     * create output folder
     *
     * @param subfolderName
     * @return {@code File}
     * @throws IOException
     */
    private static File getOutputFolder(String subfolderName) throws IOException {

        File folder = new File(OUTPUT_FOLDER + "/" + subfolderName + "/");

        if (!folder.exists()) {
            boolean created = folder.mkdirs();

            if (!created) {
                throw new IOException("Ordner konnte nicht erstellt werden!");
            }
        }

        return folder;
    }

    /**
     * this method store results of textRankcalculation in separate files
     */
    public static void getTextRankResults() {

        StringBuilder stringBuilder = null;
        try {
            for (File file : getFiles()) {
                stringBuilder = KeywordApplication.calculateTextRank(file);
                File outputFolder = getOutputFolder("textRank");
                File resultfile = new File(outputFolder, "textRank_" + file.getName() + ".txt");
                BufferedWriter writer = new BufferedWriter(
                        new BufferedWriter(new FileWriter(resultfile, false)));
                writer.write(stringBuilder.toString());
                writer.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * this method store results of tfidfcalculation in separate files
     */
    public static void getTFIDFResults() {

        try {

            StringBuilder stringBuilder = null;
            TFIDFTextAnalyser textAnalysers = new TFIDFTextAnalyser().withOpenNLP().withStopwordFilter();

            try {
                for (File file : getFiles()) {
                    DocumentConverter documentConverter = new DocumentConverter(file);
                    stringBuilder = KeywordApplication.calculateTFIDF(file, textAnalysers);
                    File outputFolder = getOutputFolder("tfidf");
                    File resultfile = new File(outputFolder, "tfidf_" + file.getName() + ".txt");
                    BufferedWriter writer = new BufferedWriter(
                            new BufferedWriter(new FileWriter(resultfile, false)));
                    writer.write(stringBuilder.toString());
                    writer.close();
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

    }

    public static void main(String[] args) {

        combineResult(50);
    }

}
