package de.citytwin;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

/**
 * @author ma6284si, FH Erfurt,
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class TextRankAnalyser {

    private static boolean isInitialzied = false;
    private static POSTaggerME posTagger = null;
    private static Set<String> stopwords = new HashSet<String>();
    /** Aktuelle Versionsinformation */
    private static final String VERSION = "$Revision: 1.00 $";
    private String language = "german";
    /** Klassenspezifischer, aktueller Logger (Server: org.apache.log4j.Logger; Client: java.util.logging.Logger) */
    private transient final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    private SentenceDetectorME sentenceDetector = null;
    private int windowsSize = 4;

    /**
     * Konstruktor.
     *
     * @throws IOException
     */
    public TextRankAnalyser(int windowsSize) throws IOException {
        if (isInitialzied)
            initialize();
        this.windowsSize = windowsSize;
        // https://www.slideshare.net/shubi194/textrank-bringing-order-into-texts
        // https://jgrapht.org/guide/UserOverview#graph-generation
        // https://prakhartechviz.blogspot.com/2019/03/textrank-bringing-order-to-text.html?m=0
        // https://towardsdatascience.com/textrank-for-keyword-extraction-by-python-c0bae21bcec0
    }

    /**
     * this method filter (include) terms by posfilter and return a list of theme
     *
     * @param termsWithPosTags
     * @param posFilter
     * @return
     */
    private List<String> filterByPosTags(List<Pair<String, String>> termsWithPosTags, List<String> posFilters) {
        List<String> results = new ArrayList<String>();

        for (Pair<String, String> pair : termsWithPosTags) {
            for (String posFilter : posFilters) {
                if (posFilter.equals(pair.getRight()))
                    results.add(pair.getLeft());
            }
        }
        return results;
    }

    /**
     * This method remove terms they are are in the stopwords list.
     * <p>
     * {@link TFIDFTextAnalyser#stopwords}
     *
     * @param terms {@code List<String>}
     * @return new reference of {@code List<String>} filtered
     */
    private List<String> filterByStopWords(List<String> terms) {
        List<String> results = new ArrayList<String>();
        for (String term : terms) {
            if (!stopwords.contains(term.toLowerCase())) {
                results.add(term);
            }
        }
        return results;

    }

    /**
     * @param handBodyContentHandler
     * @return
     */
    private List<String> getSentences(BodyContentHandler bodyContentHandler) {

        List<String> results = new ArrayList<String>();
        String[] sentences = sentenceDetector.sentDetect(bodyContentHandler.toString());
        for (String sentence : sentences) {
            results.add(sentence);
        }
        return results;

    }

    /**
     * @param text
     * @throws IOException
     */
    private List<List<String>> buildGraph(BodyContentHandler bodyContentHandler) throws IOException {

        List<List<String>> results = new ArrayList<List<String>>();
        List<String> sentences = getSentences(bodyContentHandler);
        List<String> filteredSentences = null;

        for (String sentence : sentences) {
            List<Pair<String, String>> pairs = getTokensAndPosTags(sentence);
            filteredSentences = filterByPosTags(pairs, getTagFilters());
            filteredSentences = filterByStopWords(filteredSentences);
            if (filteredSentences.size() >= windowsSize)
                results.add(filteredSentences);
        }
        return results;

    }

    /**
     * This method splits an sentence in each term
     * <p>
     * and adds pos tag.
     *
     * @param sentence
     * @return
     * @throws IOException
     */
    private List<Pair<String, String>> getTokensAndPosTags(String sentence) throws IOException {

        List<Pair<String, String>> results = new ArrayList<Pair<String, String>>();
        List<String> terms = new ArrayList<String>();
        Analyzer analyzer;

        analyzer = CustomAnalyzer.builder()
                .withTokenizer("whitespace")
                .addTokenFilter("porterstem")
                .addTokenFilter("hyphenatedwords")
                .build();

        TokenStream stream = analyzer.tokenStream(null, new StringReader(sentence));
        CharTermAttribute attr = stream.addAttribute(CharTermAttribute.class);

        stream.reset();

        while (stream.incrementToken()) {
            terms.add(attr.toString());
        }

        String[] stringArray = new String[terms.size()];
        stringArray = terms.toArray(stringArray);
        String[] tags = posTagger.tag(stringArray);

        Pair<String, String> pair = null;
        for (int index = 0; index < terms.size(); ++index) {
            pair = Pair.of(terms.get(index), tags[index]);
            results.add(pair);
        }

        analyzer.close();
        stream.close();
        return results;
    }

    /**
     */
    private void initialize() throws IOException {


        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("de-sent.bin");
        SentenceModel sentenceModel = new SentenceModel(inputStream);
        sentenceDetector = new SentenceDetectorME(sentenceModel);
        inputStream.close();

        inputStream = getClass().getClassLoader().getResourceAsStream("de-pos-perceptron.bin");
        posTagger = new POSTaggerME(new POSModel(inputStream));
        inputStream.close();

        inputStream = getClass().getClassLoader().getResourceAsStream("stopswords_de.txt");
        Scanner scanner = new Scanner(inputStream).useDelimiter("\\r\\n");

        while (scanner.hasNext()) {
            stopwords.add(scanner.next());
        }
        inputStream.close();
        scanner.close();

        isInitialzied = true;

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

}
