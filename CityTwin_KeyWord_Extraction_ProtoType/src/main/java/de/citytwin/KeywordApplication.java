package de.citytwin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
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
     * @param maxLines
     * @return new reference of {@code StringBuilder}
     */
    private static StringBuilder calculateTextRank(File file, int maxLines) {

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

            Map<String, Double> result = textRankAnalyser.getTermsAndScores(bodyContentHandler, 5, 35);
            int currentLine = 0;
            for (String key : result.keySet()) {
                if (currentLine++ > maxLines) {
                    break;
                }
                formatter.format("%1$35s --> %2$.13f",
                        key,
                        result.get(key));
                stringBuilder.append("\n");

            }
            formatter.close();
            return stringBuilder;

        } catch (Exception exception) {

            logger.error(exception.getMessage(), exception);
        }

        return stringBuilder;
    }

    /**
     * this method return term, them linked other terms and his score calculated by textRank
     *
     * @param file
     * @param maxLines
     * @return new reference of {@code StringBuilder}
     */
    private static StringBuilder calculateTextRankPairTerms(File file, int maxLines) {
        StringBuilder stringBuilder = new StringBuilder();
        try {

            DocumentConverter documentConverter = new DocumentConverter(file);
            BodyContentHandler bodyContentHandler = documentConverter.getBodyContentHandler();
            TextRankAnalyser textRankAnalyser = new TextRankAnalyser().withOpenNLP();
            Formatter formatter = new Formatter(stringBuilder, Locale.GERMAN);

            formatter.format("%1$160s --> %2$s11",
                    "term",
                    "score");
            stringBuilder.append("\n");

            Map<String, Double> result = textRankAnalyser.getPairTermsAndScores(bodyContentHandler, 3);
            int currentLine = 0;
            for (String key : result.keySet()) {
                if (currentLine++ > maxLines) {
                    break;
                }
                formatter.format("%1$160s --> %2$.13f",
                        key,
                        result.get(key));
                stringBuilder.append("\n");

            }
            formatter.close();
            return stringBuilder;

        } catch (Exception exception) {

            logger.error(exception.getMessage(), exception);
        }
        return stringBuilder;

    }

    /**
     * this method calculate textRank score for each sentence in a textcorpus and return them.
     *
     * @param file
     * @param maxLines
     * @return new reference of {@code StringBuilder}
     */
    private static StringBuilder calculateTextRankSentencesResults(File file, int maxLines) {
        StringBuilder stringBuilder = new StringBuilder();
        try {

            DocumentConverter documentConverter = new DocumentConverter(file);
            BodyContentHandler bodyContentHandler = documentConverter.getBodyContentHandler();
            TextRankAnalyser textRankAnalyser = new TextRankAnalyser().withOpenNLP();
            Formatter formatter = new Formatter(stringBuilder, Locale.GERMAN);

            Map<String, Double> result = textRankAnalyser.getSentencesAndScores(bodyContentHandler, 35);
            int currentLine = 0;
            for (String key : result.keySet()) {
                if (currentLine++ > maxLines) {
                    break;
                }

                formatter.format("score --> %1$f11", result.get(key));
                stringBuilder.append("\n");
                stringBuilder.append(key);
                stringBuilder.append("\n");

            }
            formatter.close();
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
     * @param maxLines
     * @return {@code StringBuilder}
     */
    private static StringBuilder calculateTFIDF(File file, TFIDFTextAnalyser tfidfTextAnalyser, int maxLines) {

        StringBuilder stringBuilder = new StringBuilder();
        Formatter formatter = new Formatter(stringBuilder, Locale.GERMAN);
        BodyContentHandler bodyContentHandler = null;

        Map<String, Quartet<Integer, Double, String, Set<Integer>>> results = null;
        Quartet<Integer, Double, String, Set<Integer>> quartet = null;

        try {

            DocumentConverter documentConverter = new DocumentConverter(file);
            bodyContentHandler = documentConverter.getBodyContentHandler();
            results = tfidfTextAnalyser.getTermsAndScores(bodyContentHandler,
                    GermanTextProcessing.getPosTagList(),
                    TFIDFTextAnalyser.NormalizationType.NONE);
            // caption
            formatter.format(" %1$35s --> %2$11s", "term", "TFIDF Score");
            stringBuilder.append("\n");
            int currentLine = 0;
            for (String key : results.keySet()) {
                if (currentLine++ > maxLines) {
                    break;
                }
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

    //
    /**
     * this method use {@code TextRankAnalyser} and {@code TFIDFTextAnalyser}
     *
     * @param maxLines
     */
    public static void getBothResult(int maxLines) {

        for (File file : getFiles()) {
            try {
                StringBuilder stringBuilder = new StringBuilder();
                Formatter formatter = new Formatter(stringBuilder, Locale.GERMAN);
                TFIDFTextAnalyser tdTfidfTextAnalyser = new TFIDFTextAnalyser().withOpenNLP().withStopwordFilter();
                TextRankAnalyser textRankAnalyser = new TextRankAnalyser().withOpenNLP();
                DocumentConverter documentConverter = new DocumentConverter(file);
                BodyContentHandler bobBodyContentHandler = documentConverter.getBodyContentHandler();

                Map<String, Quartet<Integer, Double, String, Set<Integer>>> tfidfresults = tdTfidfTextAnalyser
                        .getTermsAndScores(bobBodyContentHandler, null, TFIDFTextAnalyser.NormalizationType.NONE);

                Map<String, Double> textRankResult = textRankAnalyser.getTermsAndScores(bobBodyContentHandler, 4, 15);
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

                int currentLine = 0;
                for (String key : combineResults.keySet()) {
                    if (currentLine++ > maxLines) {
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

                File outputFolder = getOutputFolder("both");
                File resultfile = new File(outputFolder, "both_" + file.getName() + ".txt");
                BufferedWriter writer = new BufferedWriter(
                        new BufferedWriter(new FileWriter(resultfile, false)));
                writer.write(stringBuilder.toString());
                writer.close();

                System.out.println(stringBuilder.toString());
                formatter.close();

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
        results.add(new File(INPUT_FOLDER + "begruendung11-14a.pdf"));
        results.add(new File(INPUT_FOLDER + "biologische_vielfalt_strategie.docx"));
        results.add(new File(INPUT_FOLDER + "einlegeblatt_gruenanlagensanierung.docx"));
        results.add(new File(INPUT_FOLDER + "festsetzungbegruendung-xvii-50aa.pdf"));
        results.add(new File(INPUT_FOLDER + "mdb-beg4b_004.pdf"));
        results.add(new File(INPUT_FOLDER + "StEPWohnen2030-Langfassung.pdf"));
        results.add(new File(INPUT_FOLDER + "Strategie_Smart_City_Berlin.pdf"));
        results.add(new File(INPUT_FOLDER + "Strategie-Stadtlandschaft-Berlin.pdf"));
        results.add(new File(INPUT_FOLDER + "UVPG.pdf"));
        results.add(new File(INPUT_FOLDER + "Stadtgrün Selbstverpflichtung.docx"));
        results.add(new File(INPUT_FOLDER + "Wasseratlas.pdf"));
        results.add(new File(INPUT_FOLDER + "TG0100061100201300.xls"));
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
     * this method store results of TextRankPairTerms calculation in separate files
     *
     * @param maxLines
     */
    public static void getTextRankPairTermResults(int maxLines) {

        StringBuilder stringBuilder = null;

        try {
            for (File file : getFiles()) {
                stringBuilder = KeywordApplication.calculateTextRankPairTerms(file, maxLines);
                File outputFolder = getOutputFolder("textRankPairTerms");
                File resultfile = new File(outputFolder, "textRankPairTerms_" + file.getName() + ".txt");
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
     * this method store results of textRankcalculation in separate files
     *
     * @param maxLines
     */
    public static void getTextRankResults(int maxLines) {

        StringBuilder stringBuilder = null;
        try {
            for (File file : getFiles()) {
                stringBuilder = KeywordApplication.calculateTextRank(file, maxLines);
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
     * this method store results of TextRankPairTerms calculation in separate files
     *
     * @param maxLines
     */
    public static void getTextRankSentencesResults(int maxLines) {

        StringBuilder stringBuilder = null;

        try {
            for (File file : getFiles()) {
                stringBuilder = KeywordApplication.calculateTextRankSentencesResults(file, maxLines);
                File outputFolder = getOutputFolder("textRankSentences");
                File resultfile = new File(outputFolder, "textRankSentences_" + file.getName() + ".txt");
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
     * this method store results of tfidf calculation in separate files
     *
     * @param maxLines
     */
    public static void getTFIDFResults(int maxLines) {

        try {

            StringBuilder stringBuilder = null;
            TFIDFTextAnalyser textAnalysers = new TFIDFTextAnalyser().withOpenNLP().withStopwordFilter();

            try {
                for (File file : getFiles()) {
                    stringBuilder = KeywordApplication.calculateTFIDF(file, textAnalysers, maxLines);
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

    public static void calculateWord2Vec() {

        try {
            String path = "D:\\vms\\sharedFolder\\wikidumps\\text\\AE";
            // String path = "D:\\Keyword extraction";
            String fileName = "\\wiki_31";
            Word2VecAnalyser analyser = new Word2VecAnalyser();
            List<BodyContentHandler> bodyContentHandlers = new ArrayList<BodyContentHandler>();
            for (File file : getFiles()) {
                bodyContentHandlers.add(new DocumentConverter(file).getBodyContentHandler()) ;
            }

            analyser.getWord2Vec(bodyContentHandlers);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            logger.error(e.getMessage(), e);
        }

    }

    public static void main(String[] args) {

        // getTextRankResults(100);
        // getBothResult(100);
        // getTFIDFResults(100);
        // getTextRankSentencesResults(100);
        // getTextRankPairTermResults(100);
        calculateWord2Vec();

    }

}
