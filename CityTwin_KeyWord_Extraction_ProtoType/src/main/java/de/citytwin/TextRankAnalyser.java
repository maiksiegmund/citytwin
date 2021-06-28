package de.citytwin;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.tika.sax.BodyContentHandler;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.DepthFirstIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ma6284si, FH Erfurt,
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class TextRankAnalyser {

    /**
     * This inner class represent textrankmatrix only use here. used as struct ... contains a graph and there adjazenz matrix
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
         * @return {@code Double[][]} simple adjazenz matrix
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
         * @return {@code Map<String, Double[]> } adjazenz matrix
         */
        public Map<String, Double[]> getValues() {
            return values;
        }

        /**
         * this method initialize the class fields <br>
         * {@link TextRankMatrix#values} (adjazenzmatrix) {@code Map<String, Double[]> } (term : rowVector ) <br>
         * {@link TextRankMatrix#matrix} simple adjazenz matrix {@code Double[][]} <br>
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

    }

    private static transient final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String VERSION = "$Revision: 1.00 $";

    private Graph<String, DefaultEdge> graph = null;
    private TextRankMatrix textRankMatrix = null;
    private GermanTextProcessing textProcessing;
    private boolean isOpenNLP = false;

    /**
     * Konstruktor.
     *
     * @throws IOException
     */
    public TextRankAnalyser() throws IOException {

        initialize();

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
        String[] wordwindow = new String[wordWindowsSize]; // holds vertices
        String vertex = "";
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
                        // avoid graph circles, sometimes heading and sentence start with same term and will be combine,
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
                value = textRankMatrix.scalarProduct(rowVector, columnVector);
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
        formatter.close();
        return stringBuilder.toString();

    }

    /**
     * this method initialize nlp components
     *
     * @throws IOException
     */
    private void initialize() throws IOException {

        textProcessing = new GermanTextProcessing();
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
        field.close();
        return stringBuilder.toString();
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
        List<String> sentences = textProcessing.tokenizeBodyContentToSencences(bodyContentHandler);
        List<String> filteredSentences = null;

        for (String sentence : sentences) {
            List<String> terms = (isOpenNLP) ? textProcessing.tokenizeOpenNLP(sentence) : textProcessing.tokenizeLucene(sentence);
            List<Pair<String, String>> pairs = textProcessing.getPOSTags(terms);
            filteredSentences = textProcessing.filterByPosTags(pairs);
            filteredSentences = textProcessing.filterByStopWords(filteredSentences);
            if (filteredSentences.size() >= wordWindowsSize) {
                results.add(filteredSentences);
            }

        }
        return results;

    }

    /**
     * use lucene tokenizer
     *
     * @return new reference of {@link TextRankAnalyser}
     */
    public TextRankAnalyser withLucene() {
        this.isOpenNLP = false;
        return this;
    }

    /**
     * use opennlp tokenizer
     *
     * @return new reference of {@link TextRankAnalyser}
     */
    public TextRankAnalyser withOpenNLP() {
        this.isOpenNLP = true;
        return this;
    }

}
