package de.citytwin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.tika.sax.BodyContentHandler;
import org.javatuples.Quartet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeywordApplication {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String OUTPUT_FOLDER = "output";
    private static final String INPUT_FOLDER = "D:\\vms\\\\sharedFolder\\";

    public static StringBuilder calculateTFIDF(List<File> files, TFIDFTextAnalyser tfidfTextAnalyser,
            List<String> posTags, String description) {

        StringBuilder stringBuilder = new StringBuilder();
        Formatter formatter = new Formatter(stringBuilder, Locale.GERMAN);
        BodyContentHandler bodyContentHandler = null;
        DocumentConverter documentConverter = new DocumentConverter();
        Map<String, Quartet<Integer, Double, String, Set<Integer>>> results = null;
        Quartet<Integer, Double, String, Set<Integer>> quartet = null;

        try {

            for (File file : files) {

                bodyContentHandler = documentConverter.documentToBodyContentHandler(file);
                LocalDateTime startTime = LocalDateTime.now();
                results = tfidfTextAnalyser.calculateTFIDF(bodyContentHandler,
                        posTags,
                        TFIDFTextAnalyser.NormalizationType.NONE);
                LocalDateTime endTime = LocalDateTime.now();
                // caption
                formatter.format("#file: %1$34s", file.getName() + "\n");
                formatter.format("#start %1$34s --> %2$10s --> %3$15s --> %4$10s --> %5$s",
                        "term",
                        "count",
                        "TFIDF Score",
                        "PosTAG",
                        "SentenceIndex \n");

                for (String key : results.keySet()) {

                    quartet = results.get(key);
                    String sentenceIndies = "";
                    int count = 0;

                    for (Integer index : quartet.getValue3()) {

                        if (count < 10) {
                            sentenceIndies += index.toString() + ",";
                        } else {
                            sentenceIndies += "...";
                            break;
                        }

                        count++;
                    }

                    formatter.format("%1$35s ### %2$10s ### %3$.15f ### %4$10s ### %5$s",
                            key,
                            quartet.getValue0().toString(),
                            quartet.getValue1().doubleValue(),
                            quartet.getValue2(),
                            sentenceIndies);
                    stringBuilder.append("\n");
                }

                stringBuilder.append("#end duration calculated td idf: "
                        + String.valueOf(startTime.until(endTime, ChronoUnit.MINUTES)) + " min(s) " + description
                        + " \n");
            }

            formatter.close();

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return stringBuilder;

    }

    public static List<File> getFiles() {

        String directory = "D:\\vms\\sharedFolder\\";

        List<File> results = new ArrayList<File>();
        results.add(new File(directory + "festsetzungbegruendung-xvii-50aa.pdf"));
        // results.add(new File(directory + "Angebotsmassnahmen_2017.pdf"));
        // results.add(new File(directory + "beg4b-042.pdf"));
        // results.add(new File(directory + "Bekanntmachungstext.pdf"));
        results.add(new File(directory + "FNP Bericht 2020.pdf"));
        // results.add(new File(directory + "Strategie-Stadtlandschaft-Berlin.pdf"));
        results.add(new File(directory + "biologische_vielfalt_strategie.pdf"));
        // results.add(new File(directory + "modell_baulandentwicklung.docx"));
        results.add(new File(directory + "Charta Stadtgrün.docx"));

        return results;

    }

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

    public static void getResults() {

        try {

            String[] descriptions = {
                // "Lucene ",
                // "Lucene + Stemming ",
                "Lucene + Stemming + Stopwordfilter ",
                // "opennlp ",
                // "opennlp + Stemming ",
                "opennlp + Stopwordfilter "
            };

            TFIDFTextAnalyser[] textAnalysers = {
                // new TFIDFTextAnalyser().withLucene(),
                // new TFIDFTextAnalyser().withLucene().withStemming(),
                new TFIDFTextAnalyser().withLucene().withStemming().withStopwordFilter(),
                // new TFIDFTextAnalyser().withOpenNLP(),
                // new TFIDFTextAnalyser().withOpenNLP().withStemming(),
                new TFIDFTextAnalyser().withOpenNLP().withStopwordFilter() };
            StringBuilder stringBuilder = new StringBuilder();

            try {
                for (int index = 0; index < textAnalysers.length; index++) {
                    stringBuilder.append(KeywordApplication.calculateTFIDF(getFiles(),
                            textAnalysers[index],
                            null,
                            descriptions[index]));
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            File outputFolder = getOutputFolder("tfidf");
            File file = new File(outputFolder, "tfidf_result.txt");

            BufferedWriter writer = new BufferedWriter(
                    new BufferedWriter(new FileWriter(file, false)));
            writer.write(stringBuilder.toString());
            writer.close();

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

    }

    public static List<String> getTagFilters() {

        List<String> tagFilters = new ArrayList<String>();

        // tagFilters.add("CC");
        // tagFilters.add("CD");
        // tagFilters.add("DT");
        // tagFilters.add("EX");
        // tagFilters.add("FW");
        // tagFilters.add("IN");
        // tagFilters.add("JJ");
        // tagFilters.add("JJR");
        // tagFilters.add("JJS");
        // tagFilters.add("LS");
        tagFilters.add("MD");
        tagFilters.add("NN");
        tagFilters.add("NNS");
        tagFilters.add("NNP");
        tagFilters.add("NNPS");
        // tagFilters.add("PDT");
        // tagFilters.add("POS");
        // tagFilters.add("PRP");
        // tagFilters.add("PRP$");
        // tagFilters.add("RB");
        // tagFilters.add("RBR");
        // tagFilters.add("RBS");
        // tagFilters.add("RP");
        // tagFilters.add("SYM");
        // tagFilters.add("TO");
        // tagFilters.add("UH");
        // tagFilters.add("VB");
        // tagFilters.add("VBD");
        // tagFilters.add("VBG");
        // tagFilters.add("VBN");
        // tagFilters.add("VBP");
        // tagFilters.add("VBZ");
        // tagFilters.add("WDT");
        // tagFilters.add("WP");
        // tagFilters.add("WP$");
        // tagFilters.add("WRB");

        return tagFilters;

    }

    public static void main(String[] args) {

        // runTFIDF();
        //getResults();
        runTextRank();
    }

    public static void runTFIDF() {

        try {

            StringBuilder stringBuilder = new StringBuilder();
            DocumentConverter documentConverter = new DocumentConverter();
            TFIDFTextAnalyser tFIDFTextAnalyser = new TFIDFTextAnalyser().withOpenNLP();
            Map<String, Quartet<Integer, Double, String, Set<Integer>>> result;

            File inputFile = new File("D:\\vms\\sharedFolder\\festsetzungbegruendung-xvii-50aa.pdf");
            // File file = new File("D:\\vms\\sharedFolder\\testdata.txt");
            Formatter formatter = new Formatter(stringBuilder, Locale.GERMAN);
            BodyContentHandler bodyContentHandler = documentConverter.documentToBodyContentHandler(inputFile);
            // documentConverter.saveAsTextFile(bodyContentHandler,
            // "D:\\vms\\sharedFolder\\festsetzungbegruendung-xvii-50aa.txt");

            result = tFIDFTextAnalyser.calculateTFIDF(bodyContentHandler,
                    null,
                    TFIDFTextAnalyser.NormalizationType.DOUBLE);
            Quartet<Integer, Double, String, Set<Integer>> quartet = null;

            formatter.format("%1$35s --> %2$5s --> %3$15s --> %4$10s --> %5$15s",
                    "term",
                    "count",
                    "TFIDF Score",
                    "Pos TAG",
                    "Sent Index\n");

            for (String key : result.keySet()) {

                quartet = result.get(key);
                String sentenceIndies = "";
                for (Integer index : quartet.getValue3()) {
                    sentenceIndies += index.toString() + ", ";
                }

                formatter.format("%1$35s --> %2$5s --> %3$.13f --> %4$10s --> %5$s",
                        key,
                        quartet.getValue0().toString(),
                        quartet.getValue1().doubleValue(),
                        quartet.getValue2(),
                        sentenceIndies);
                stringBuilder.append("\n");

                // System.out.print(MessageFormat.format("spliter: {0} --> word: {1} --> count: {2} ", spliter, word,
                // tfresults.get(spliter).get(word)));
                // System.out.print("\n");
            }

            File outputFolder = getOutputFolder("tfidf");
            File file = new File(outputFolder, "tfidf_" + inputFile.getName() + "_opennlp.txt");
            formatter.close();
            BufferedWriter writer = new BufferedWriter(new BufferedWriter(
                    new FileWriter(file, false)));
            writer.write(stringBuilder.toString());

            writer.close();

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    public static void writeSentencesToFile(final List<String> sentences, final String destination) {

        try {

            StringBuilder stringBuilder = new StringBuilder();

            for (String sentence : sentences) {
                stringBuilder.append(sentence);
                stringBuilder
                        .append("################################################################################\n");
            }

            BufferedWriter writer = new BufferedWriter(new BufferedWriter(new FileWriter(destination, false)));
            writer.write(stringBuilder.toString());
            writer.close();

        } catch (IOException exception) {
            logger.error(exception.getMessage(), exception);
        }

    }

    public static void runTextRank() {

        try {
//            File file = new File(INPUT_FOLDER + "testdata_german_simple.txt");
            File file = new File(INPUT_FOLDER + "simple_testdata_german.txt");
            DocumentConverter documentConverter = new DocumentConverter();
            BodyContentHandler bodyContentHandler = documentConverter.documentToBodyContentHandler(file);
            String text =bodyContentHandler.toString();
            TextRankAnalyser textRankAnalyser = new TextRankAnalyser(3);
            textRankAnalyser.runTextRank(bodyContentHandler);


        } catch (Exception exception) {
            // TODO Auto-generated catch block
            exception.printStackTrace();
        }
    }
}
