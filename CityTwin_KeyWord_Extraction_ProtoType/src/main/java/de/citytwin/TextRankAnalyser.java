package de.citytwin;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.tika.sax.BodyContentHandler;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.DepthFirstIterator;
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

    /**
     * This inner class represent textrankmatrix only use here. used as struct ... contains a graph and there adjazenzmatrix
     *
     * @author ma6284si, FH Erfurt
     * @version $Revision: 1.0 $
     * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
     */
    public class TextRankMatrix {

        public final transient Graph<String, DefaultEdge> graph;

        private Map<String, Integer> indexOfTerm;
        private Double[][] matrix;
        private Map<String, Double[]> values;

        /**
         * Konstruktor.
         *
         * @param graph
         */
        public TextRankMatrix(Graph<String, DefaultEdge> graph) {
            super();
            this.graph = graph;
            initialize();
        }

        /**
         * @return {@code Map<String, Integer>}
         */
        public Map<String, Integer> getIndexOfTerm() {
            return indexOfTerm;
        }

        /**
         * @return {@code Double[][]} simple adjazenzmatrix
         */
        public Double[][] getMatrix() {
            return matrix;
        }

        /**
         * @param edge {@code DefaultEdge}
         * @return {@code String} target of DefaultEdge
         */
        private String getTarget(DefaultEdge edge) {
            // return "(" + source + " : " + target + ")";
            String temp = edge.toString();
            int start = temp.indexOf(" : ") + " : ".length();
            int end = temp.indexOf(")");
            temp = temp.substring(start, end);
            return temp;
        }

        /**
         * @return {@code Map<String, Double[]> } adjazenzmatrix
         */
        public Map<String, Double[]> getValues() {
            return values;
        }

        /**
         * this method initialize the classfields <br>
         * {@link TextRankMatrix#values} (adjazenzmatrix) {@code Map<String, Double[]> } (term : rowVector ) <br>
         * {@link TextRankMatrix#matrix} simple adjazenzmatrix {@code Double[][]} <br>
         * {@link TextRankMatrix#indexOfTerm} mapping rowVector[] to term
         */
        private void initialize() {
            int size = graph.vertexSet().size();
            int countOut = 0;
            int index = 0;
            double initializeValue = 0.0d;
            String term = "";
            matrix = new Double[size][size];
            Iterator<String> iterator = new DepthFirstIterator<>(graph);
            Set<DefaultEdge> edges = null;

            values = new HashMap<String, Double[]>(size);
            indexOfTerm = new HashMap<String, Integer>(size);

            // fill with default 0
            while (iterator.hasNext()) {
                term = iterator.next();
                Double[] row = new Double[size];
                for (int i = 0; i < row.length; i++) {
                    row[i] = initializeValue;
                }
                values.put(term, row);
            }
            // set index
            for (String key : values.keySet()) {
                indexOfTerm.put(key, index++);
            }

            index = 0;
            // calculate out edges
            iterator = new DepthFirstIterator<>(graph);
            while (iterator.hasNext()) {
                term = iterator.next();
                edges = graph.outgoingEdgesOf(term);
                Double[] tempArray = values.get(term);
                for (DefaultEdge edge : edges) {
                    // sinnlos
                    int tempIndex = indexOfTerm.get(getTarget(edge));
                    tempArray[tempIndex] = (1 / (double)edges.size());
                }
            }

            // set simple matrix
            index = 0;
            for (String key : values.keySet()) {
                matrix[indexOfTerm.get(key)] = values.get(key);
            }

        }

    }

    private static boolean isInitialzied = false;
    /** Klassenspezifischer, aktueller Logger (Server: org.apache.log4j.Logger; Client: java.util.logging.Logger) */
    private static transient final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static POSTaggerME posTagger = null;

    private static SentenceDetectorME sentenceDetector = null;

    private static Set<String> stopwords = new HashSet<String>();

    /** Aktuelle Versionsinformation */
    private static final String VERSION = "$Revision: 1.00 $";

    private static List<String> getPosTags() {

        // https://www.cis.lmu.de/~schmid/tools/TreeTagger/data/stts_guide.pdf
        List<String> tagFilters = new ArrayList<String>();
        // english
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
        // tagFilters.add("MD");
        // tagFilters.add("NN");
        // tagFilters.add("NE");
        // tagFilters.add("NNS");
        // tagFilters.add("NNP");
        // tagFilters.add("NNPS");
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

        // german
        tagFilters.add("ADJA");
        tagFilters.add("ADJD");
        tagFilters.add("ADV");
        // tagFilters.add("APPR");
        // tagFilters.add("APPRART");
        // tagFilters.add("APPO");
        // tagFilters.add("APZR");
        // tagFilters.add("ART");
        tagFilters.add("CARD");
        // tagFilters.add("FM");
        // tagFilters.add("ITJ");
        // tagFilters.add("KOUI");
        // tagFilters.add("KOUS");
        // tagFilters.add("KON");
        // tagFilters.add("KOKOM");
        tagFilters.add("NN");
        tagFilters.add("NE");
        tagFilters.add("PDS");
        tagFilters.add("PDAT");
        tagFilters.add("PIS");
        tagFilters.add("PIAT");
        tagFilters.add("PIDAT");
        // tagFilters.add("PPER");
        // tagFilters.add("PPOSAT");
        // tagFilters.add("PRELS");
        // tagFilters.add("PRELAT");
        // tagFilters.add("PRF");
        // tagFilters.add("PWS");
        // tagFilters.add("PWAT");
        // tagFilters.add("PWAV");
        // tagFilters.add("PAV");
        // tagFilters.add("PTKZU");
        // tagFilters.add("PTKNEG");
        // tagFilters.add("PTKVZ");
        // tagFilters.add("PTKANT");
        // tagFilters.add("PTKA");
        // tagFilters.add("TRUNC");
        tagFilters.add("VVFIN");
        tagFilters.add("VVIMP");
        tagFilters.add("VVINF");
        tagFilters.add("VVIZU");
        tagFilters.add("VVPP");
        tagFilters.add("VAFIN");
        tagFilters.add("VAIMP");
        tagFilters.add("VAINF");
        tagFilters.add("VAPP");
        tagFilters.add("VMFIN");
        tagFilters.add("VMINF");
        tagFilters.add("VMPP");
        tagFilters.add("XY");

        return tagFilters;

    }

    private Graph<String, DefaultEdge> graph = null;
    private TextRankMatrix textRankMatrix = null;

    /**
     * Konstruktor.
     *
     * @throws IOException
     */
    public TextRankAnalyser() throws IOException {
        if (!isInitialzied) {
            initialize();
        }
    }

    /**
     * this method build a graph by tankrank algorithm terms are Vertices
     *
     * @param senetences
     * @param wordWindowsSize
     * @return new reference of {@code Graph<String, DefaultEdge>}
     */
    private Graph<String, DefaultEdge> buildGraph(List<List<String>> senetences, int wordWindowsSize) {
        // Graph<String, DefaultEdge> result = new DirectedMultigraph<>(DefaultEdge.class);
        Graph<String, DefaultEdge> result = new SimpleDirectedGraph<>(DefaultEdge.class);
        Set<String> terms = new HashSet<>();
        String[] wordwindow = new String[wordWindowsSize]; // holds vertices
        String[] wordPair = new String[2];
        String vertex = "";
        boolean addVertex = false;
        int slidingWordIndex = 0;

        for (List<String> sentence : senetences) {
            for (int wordIndex = 0 + slidingWordIndex; wordIndex <= (sentence.size() - wordWindowsSize); ++wordIndex) {

                // assign wordWindow and add vertices
                for (int windowIndex = 0; windowIndex < wordWindowsSize; ++windowIndex) {
                    wordwindow[windowIndex] = sentence.get(wordIndex + windowIndex);
                    vertex = sentence.get(wordIndex + windowIndex);
                    result.addVertex(vertex);
                }
                // create word pairs and link theme (add edge)
                for (int windowIndex = 0; windowIndex < wordWindowsSize; ++windowIndex) {
                    for (int slidingWindowIndex = windowIndex + 1; slidingWindowIndex < wordWindowsSize; ++slidingWindowIndex) {
                        // avoid graph circles, sometimes heading and sentence start with same term and where combine,
                        if (!wordwindow[windowIndex].equals(wordwindow[slidingWindowIndex])) {
                            result.addEdge(wordwindow[windowIndex], wordwindow[slidingWindowIndex]);
                        }
                    }
                }
                slidingWordIndex++;
            }
            slidingWordIndex = 0;
        }

        logger.info(MessageFormat.format("graph completed contains {0} nodes.", result.vertexSet().size()));
        return result;
    }

    /**
     * this method calculate textrank score
     *
     * @param d
     * @param iteration
     * @return new reference of {@code Map<String, Double>} (term : score)
     */
    private Map<String, Double> calculateScore(Double d, int iteration) {
        this.textRankMatrix = new TextRankMatrix(graph);
        Map<String, Double> results = new HashMap<String, Double>(textRankMatrix.getValues().size());
        Double[] columnVector = new Double[textRankMatrix.getValues().size()];
        Double[] rowVector = null;
        Double value = 0.0d;
        int index = 0;
        for (int i = 0; i < columnVector.length; i++) {
            columnVector[i] = 1.0d;
        }

        for (int currentInteration = 0; currentInteration < iteration; currentInteration++) {
            index = 0;
            for (String key : textRankMatrix.values.keySet()) {
                rowVector = textRankMatrix.values.get(key);
                value = scalarProduct(rowVector, columnVector);
                columnVector[index++] = (1 - d) + d * (value);

            }
            logger.info(MessageFormat.format("iteration {0} of {1}.", currentInteration, iteration));
        }
        index = 0;
        for (String key : textRankMatrix.values.keySet()) {
            results.put(key, columnVector[index++]);
        }
        logger.info("score calculation completed.");
        return results;
    }

    /**
     * @param bodyContentHandler
     * @param wordWindowSize by default 4
     * @param iteration by default 30
     * @throws IOException
     */
    public Map<String, Double> calculateTextRank(BodyContentHandler bodyContentHandler, @Nullable Integer wordWindowSize, @Nullable Integer iteration)
            throws IOException {

        int currentwordWindowsSize = (wordWindowSize == null) ? wordWindowSize : 4;
        int currentIeteration = (iteration == null) ? iteration : 30;
        double d = 0.85d;

        List<List<String>> textCorpus = transformText(bodyContentHandler, currentwordWindowsSize);
        this.graph = buildGraph(textCorpus, currentwordWindowsSize);
        Map<String, Double> scores = calculateScore(d, currentIeteration);
        Map<String, Double> result = sortbyValue(scores);

        return result;
    }

    /**
     * this method keeps terms they tagged with necessary postag and included in pos-list
     *
     * @param termsWithPosTags
     * @param posTags {@link TextRankAnalyser#getPosTags()}
     * @return new refernce of {@code List<String>}
     */
    private List<String> filterByPosTags(List<Pair<String, String>> termsWithPosTags, List<String> posTags) {
        List<String> results = new ArrayList<String>();

        for (Pair<String, String> pair : termsWithPosTags) {
            for (String posFilter : posTags) {
                if (posFilter.equals(pair.getRight())) {
                    results.add(pair.getLeft());
                }
            }
        }
        logger.info("filter by pos tags completed. (terms keeping)");
        return results;
    }

    /**
     * This method removes terms they are in the stopword-list.
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
        logger.info("filter by stopwords completed. (terms removal)");
        return results;

    }

    /**
     * this method split textcorpus in sentences and return them.
     *
     * @param bodyContentHandler
     * @return new reference of {@code List<String>}
     */
    private List<String> getSentences(BodyContentHandler bodyContentHandler) {

        List<String> results = new ArrayList<String>();
        String[] sentences = sentenceDetector.sentDetect(bodyContentHandler.toString());
        String temp = "";
        for (String sentence : sentences) {
            temp = sentence.replaceAll("-\n", "");
            temp = sentence.replaceAll("\n", "");
            results.add(temp);
        }
        return results;

    }

    /**
     * This method splits an sentence in each term <br>
     * and adds pos tag and return them.
     *
     * @param sentence
     * @return new reference of {@code List<Pair<String, String>>} (term : postag)
     * @throws IOException
     */
    private List<Pair<String, String>> getTokensAndPosTags(String sentence) throws IOException {

        List<Pair<String, String>> results = new ArrayList<Pair<String, String>>();
        List<String> terms = new ArrayList<String>();
        Analyzer analyzer;

        analyzer = CustomAnalyzer.builder()
                .withTokenizer("Letter")
                .addTokenFilter("LowerCase")
                // .addTokenFilter("hyphenatedwords")
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
        logger.info("tokenize and pos tagging completed.");
        return results;
    }

    /**
     * this method, return a graph as a formatted string
     * <p>
     * node: ... <br>
     * in: [(...)] <br>
     * out:[(...)]
     *
     * @param graph
     * @return {@code String}
     */
    public String graphToString() {
        if (this.graph == null) {
            return "";
        }

        StringBuilder stringBuilder = new StringBuilder();
        Formatter formatter = new Formatter(stringBuilder, Locale.GERMAN);
        Iterator<String> iterator = new DepthFirstIterator<>(graph);

        while (iterator.hasNext()) {
            String term = iterator.next();
            formatter.format("node: %1$15s \n", term);
            formatter.format("  in: %1s", graph.incomingEdgesOf(term));
            formatter.format(" out: %1s", graph.outgoingEdgesOf(term));
        }
        return stringBuilder.toString();

    }

    /**
     * this method initialize nlp components
     *
     * @throws IOException
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
     * this method, return a matrix as a formatted string <br>
     * {@code if (N > 15) {return } return "matrix to large!"} <br>
     * is for test
     *
     * @param textRankMatrix
     * @return matrix
     */
    public String matrixToString() {

        if (textRankMatrix == null) {
            return "";
        }
        int length = 13;
        if (textRankMatrix.values.keySet().size() > 15) {
            return "matrix to large!";
        }
        StringBuilder stringBuilder = new StringBuilder();
        Formatter field = new Formatter(stringBuilder, Locale.GERMAN);
        field.format(" %1$13s |", "");
        String temp = "";
        // heading caption
        for (String key : textRankMatrix.values.keySet()) {
            temp = (key.length() > length) ? key.substring(0, length) : key;
            field.format(" %1$13s |", temp);
        }
        stringBuilder.append("\n");
        // rows
        for (String key : textRankMatrix.values.keySet()) {
            temp = (key.length() > length) ? key.substring(0, length) : key;
            Double[] array = textRankMatrix.values.get(key);
            field.format(" %1$13s |", temp);
            for (int i = 0; i < array.length; i++) {
                field.format(" %1$.11f |", array[i]);
            }
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }

    /**
     * this method calculate dot / scalar- product
     *
     * @param rowVector
     * @param columnVector
     * @return {@code Double}
     */
    private Double scalarProduct(final Double[] rowVector, final Double[] columnVector) {
        Double result = 0.0d;
        for (int row = 0; row < rowVector.length; row++) {
            result += rowVector[row] * columnVector[row];
        }
        return result;

    }

    /**
     * this method sort a map descending by value
     *
     * @param map
     * @return new reference of {@code Map<String, Double>}
     */
    private Map<String, Double> sortbyValue(Map<String, Double> map) {

        Map<String, Double> sortedMap = map.entrySet()
                .stream()
                .sorted(Comparator.comparingDouble(v -> -v.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> {
                    throw new AssertionError();
                }, LinkedHashMap::new));
        return sortedMap;
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

    /**
     * this method transform a text in a List of sentences, each sentences contains a list of terms. <br>
     * {@code if (filteredSentences.size() >= wordWindowsSize) {results.add(filteredSentences);} }
     * <p>
     * filtered by pos tags {@link TextRankAnalyser#getPosTags()}
     * <p>
     * filtered by stopwordlist {@link TextRankAnalyser#stopwords}
     *
     * @param bodyContentHandler
     * @param wordWindowsSize
     * @return new reference of {@code List<List<String>>} (sentences and they terms)
     * @throws IOException
     */
    private List<List<String>> transformText(BodyContentHandler bodyContentHandler, int wordWindowsSize) throws IOException {

        List<List<String>> results = new ArrayList<List<String>>();
        List<String> sentences = getSentences(bodyContentHandler);
        List<String> filteredSentences = null;

        for (String sentence : sentences) {
            List<Pair<String, String>> pairs = getTokensAndPosTags(sentence);
            filteredSentences = filterByPosTags(pairs, getPosTags());
            filteredSentences = filterByStopWords(filteredSentences);
            if (filteredSentences.size() >= wordWindowsSize) {
                results.add(filteredSentences);
            }

        }
        return results;

    }

}
