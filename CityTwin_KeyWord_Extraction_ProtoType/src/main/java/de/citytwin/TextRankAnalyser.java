package de.citytwin;

import java.io.IOException;
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
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.tika.sax.BodyContentHandler;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
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
     * This inner class represent textrankmatrix only use here. used as struct ... contains a graph and this adjazenz matrix
     *
     * @author ma6284si, FH Erfurt
     * @version $Revision: 1.0 $
     * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
     */
    public class TextRankMatrix {

        public final transient Graph<String, DefaultWeightedEdge> graph;
        // hold indexes of terms (in Map<K,V> add sequence does not correspond like stack or list)
        private Map<String, Integer> indexOfTerm;
        // simple adjazenz matrix with out term information
        private Double[][] matrix;
        // adjazenz matrix with term information
        //
        // term_1 (Double, Double, Double ,Double ,Double ,Double, Double)
        // term_2 (Double, Double, Double ,Double ,Double ,Double, Double)
        // term_3 (Double, Double, Double ,Double ,Double ,Double, Double)
        // . (Double, Double, Double ,Double ,Double ,Double, Double)
        // . (Double, Double, Double ,Double ,Double ,Double, Double)
        // . (Double, Double, Double ,Double ,Double ,Double, Double)
        // term_N (Double, Double, Double ,Double ,Double ,Double, Double)
        //
        //
        private Map<String, Double[]> values;
        // normalize matrix
        private boolean withNormalize = false;

        /**
         * Konstruktor.
         *
         * @param graph
         * @param withNormalize
         */
        public TextRankMatrix(Graph<String, DefaultWeightedEdge> graph, @Nullable Boolean withNormalize) {
            super();
            this.graph = graph;
            this.withNormalize = (withNormalize != null) ? withNormalize : false;
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
            double weight = 0.0d;
            int index = 0;
            double initializeValue = 0.0d;
            String term = "";
            matrix = new Double[size][size];
            Iterator<String> iterator = new DepthFirstIterator<>(graph);
            Set<DefaultWeightedEdge> edges = null;

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
            // calculate out going edges
            iterator = new DepthFirstIterator<>(graph);
            while (iterator.hasNext()) {
                term = iterator.next();
                edges = graph.outgoingEdgesOf(term);
                Double[] tempArray = values.get(term);
                for (DefaultWeightedEdge edge : edges) {
                    int tempIndex = indexOfTerm.get(graph.getEdgeTarget(edge));
                    tempArray[tempIndex] = (withNormalize) ? (1 / sumEdgeWeights(edges)) : graph.getEdgeWeight(edge);

                }
            }
            index = 0;
            // set simple adjazenz matrix
            for (String key : values.keySet()) {
                matrix[indexOfTerm.get(key)] = values.get(key);
            }
        }

        /**
         * this method normalize an vector
         *
         * @param {@code Double[]}
         * @return new reference of {@code Double[]}
         */
        private Double[] normVector(final Double[] vector) {
            Double[] results = new Double[vector.length];
            double length = 0.0d;
            for (Double value : vector) {
                length += value * value;
            }
            length = Math.sqrt(length);
            for (int index = 0; index < results.length; index++) {
                results[index] = (1.0 / length) * vector[index];
            }

            return results;
        }

        /**
         * this method calculate dot / scalar- product <br>
         * row and column have to same size, no check only for intern use
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
         * this method sum edge weights (DEFAULT_EDGE_WEIGHT is 1.0d)
         *
         * @param edges
         * @return {@code Double}
         */
        private Double sumEdgeWeights(Set<DefaultWeightedEdge> edges) {
            Double result = 0.0d;
            for (DefaultWeightedEdge defaultWeightedEdge : edges) {
                result += graph.getEdgeWeight(defaultWeightedEdge);
            }
            return result;
        }

    }

    private static transient final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String VERSION = "$Revision: 1.00 $";
    private static final double DEFAULT_EDGE_WEIGHT = 1.0d;

    // weigted graph, set by this.calculateScore(...)
    private Graph<String, DefaultWeightedEdge> graph = null;
    // matrix set by this.calculateScore(...)
    private TextRankMatrix textRankMatrix = null;
    private GermanTextProcessing textProcessing;
    private boolean isOpenNLP = false;
    private boolean withMatrixNormalize = false;
    private boolean withVectorNormalize = false;

    /**
     * Konstruktor.
     *
     * @throws IOException
     */
    public TextRankAnalyser() throws IOException {

        initialize();

    }

    /**
     * this method create a graph of a given text <br>
     * vertices are sentences edge weight is similarity between two sentences <br>
     * O = (n^2)
     *
     * @param bodyContentHandler
     * @return new reference of {@code Graph<String, DefaultWeightedEdge>}
     * @throws IOException
     */
    private Graph<String, DefaultWeightedEdge> buildGraph(BodyContentHandler bodyContentHandler) throws IOException {
        Graph<String, DefaultWeightedEdge> result = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);

        Double similartity = 0.0d;

        List<String> sentencesLeft = textProcessing.tokenizeBodyContentToSencences(bodyContentHandler);
        List<String> sentencesRight = sentencesLeft;

        Map<String, List<String>> splited = new HashMap<String, List<String>>();

        // add vertices and split
        for (String sentence : sentencesLeft) {
            result.addVertex(sentence);
            List<String> splitedTerms = (isOpenNLP) ? textProcessing.tokenizeOpenNLP(sentence) : textProcessing.tokenizeLucene(sentence);
            List<String> filtered = textProcessing.filterByStopWords(splitedTerms);
            splited.put(sentence, filtered);
        }

        for (String sentenceLeft : sentencesLeft) {
            for (String sentenceRight : sentencesRight) {
                if (!sentenceLeft.equals(sentenceRight)) {
                    similartity = getSimilartity(splited.get(sentenceLeft), splited.get(sentenceRight));
                    DefaultWeightedEdge defaultWeightedEdge = new DefaultWeightedEdge();
                    if (result.addEdge(sentenceLeft, sentenceRight, defaultWeightedEdge)) {
                        result.setEdgeWeight(defaultWeightedEdge, similartity);
                    }
                }
            }
        }
        logger.info(MessageFormat.format("graph completed contains {0} nodes.", result.vertexSet().size()));
        return result;

    }

    /**
     * this method build a graph by textrank algorithm terms are Vertices
     *
     * @param senetences {@code List<List<String>> senetences} <br>
     *            ({[term_0], [term_1], [term_2], [term_3]}, {[term_0], [term_1], [term_2], [term_3], [term_N]},{ ... })
     * @param wordWindowsSize <br>
     *            example <br>
     *            wordwindowsSize = 4 and sentence_I ([term_0], [term_1], [term_2], [term_3], [term_4], [term_5], [term_N]) <br>
     *            first iteration take [term_0], [term_1], [term_2], [term_3] of sentence_I <br>
     *            next iteration take [term_1], [term_2], [term_3], [term_4] of sentence_I <br>
     *            last iteration take [term_N-3], [term_N-2], [term_N-1], [term_N] of sentence_I <br>
     *            take in first iteration [term_1], [term_2], [term_3], [term_4]
     * @return new reference of {@code Graph<String, DefaultEdge>}
     */
    private Graph<String, DefaultWeightedEdge> buildGraph(List<List<String>> senetences, int wordWindowsSize) {
        Graph<String, DefaultWeightedEdge> result = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
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
                        // avoid graph loops, sometimes heading and sentence start with same term and will be combine,
                        if (!wordwindow[windowIndex].equals(wordwindow[slidingWindowIndex])) {
                            DefaultWeightedEdge edge = result.addEdge(wordwindow[windowIndex], wordwindow[slidingWindowIndex]);
                            if (edge != null) {
                                result.setEdgeWeight(edge, DEFAULT_EDGE_WEIGHT);
                            }
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
        this.textRankMatrix = new TextRankMatrix(graph, this.withMatrixNormalize);
        Map<String, Double> results = new HashMap<String, Double>(textRankMatrix.getValues().size());
        Double[] columnVector = new Double[textRankMatrix.getValues().size()];
        Double[] tempVector = new Double[textRankMatrix.getValues().size()];
        Double[] rowVector = null;
        Double value = 0.0d;
        int index = 0;
        for (int i = 0; i < columnVector.length; i++) {
            columnVector[i] = 1.0d;
            tempVector[i] = 1.0d;
        }

        for (int currentInteration = 0; currentInteration < iteration; currentInteration++) {
            index = 0;
            for (String key : textRankMatrix.values.keySet()) {
                rowVector = textRankMatrix.values.get(key);
                value = textRankMatrix.scalarProduct(rowVector, columnVector);
                // avoid circle dependency
                tempVector[index++] = (1 - d) + d * (value);
            }
            // avoid number overflowF
            columnVector = (withVectorNormalize) ? textRankMatrix.normVector(tempVector) : tempVector.clone();
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
     * this method get in and outbounds Terms of each term in textRank parameter <br>
     * example maxLinks = 3; <br>
     *
     * @param textRank (contains the keywords founded by textRank)
     * @param maxLinks
     * @return {@code Map<String, Map<String, List<String>>>} (keyword : in : List(...) and out : List(...))
     */
    public Map<String, Map<String, List<String>>> getLinkedTerms(Map<String, Double> textRank, @Nullable Integer maxLinks) {

        Map<String, Map<String, List<String>>> result = new HashMap<String, Map<String, List<String>>>();
        int max = (maxLinks == null) ? 3 : maxLinks;
        List<String> inbounds = null;
        List<String> outbounds = null;
        for (Map.Entry<String, Double> entry : textRank.entrySet()) {
            // seeking vertices
            Set<DefaultWeightedEdge> outEdges = graph.outgoingEdgesOf(entry.getKey());
            Set<DefaultWeightedEdge> inEdges = graph.incomingEdgesOf(entry.getKey());
            int count = 0;
            inbounds = new ArrayList<String>();
            for (DefaultWeightedEdge defaultWeightedEdge : inEdges) {
                if (count++ > max) {
                    break;
                }
                graph.getEdgeSource(defaultWeightedEdge);
                inbounds.add(graph.getEdgeSource(defaultWeightedEdge));
            }
            count = 0;
            outbounds = new ArrayList<String>();
            for (DefaultWeightedEdge defaultWeightedEdge : outEdges) {
                if (count++ > max) {
                    break;
                }
                outbounds.add(graph.getEdgeSource(defaultWeightedEdge));

            }
            Map<String, List<String>> links = new HashMap<String, List<String>>();
            links.put("in", inbounds);
            links.put("out", outbounds);
            result.put(entry.getKey(), links);
        }
        return result;
    }

    /**
     * this method calculate a score for sentences with textRank algorithm
     *
     * @param bodyContentHandler
     * @param iteration
     * @return new reference of {@code Map<String, Double>} (sentences : score)
     * @throws IOException
     */
    public Map<String, Double> getSentencesAndScores(BodyContentHandler bodyContentHandler, @Nullable Integer iteration) throws IOException {

        double d = 0.85d;
        this.withMatrixNormalize = false;
        this.withVectorNormalize = true;
        int currentIeteration = (iteration == null) ? iteration : 30;
        graph = buildGraph(bodyContentHandler);
        Map<String, Double> scores = calculateScore(d, currentIeteration);
        Map<String, Double> result = sortbyValue(scores);
        return result;
    }

    //
    /**
     * this method calculate similartity between two sentences
     *
     * @see <a href=https://web.eecs.umich.edu/~mihalcea/papers/mihalcea.emnlp04.pdf> textRank Paper, chapter 4</a>
     * @param lefts {@code List<String>}
     * @param rights {@code List<String>}
     * @return similartity
     */
    private Double getSimilartity(List<String> lefts, List<String> rights) {

        if (lefts.size() == 0 || rights.size() == 0) {
            return 0.0d;
        }
        double countOfTermInBothList = 0;

        // leftTerms = textProcessing.filterByStopWords(leftTerms);
        // rightTerms = textProcessing.filterByStopWords(rightTerms);
        Set<String> mergedTerms = new HashSet<String>();
        for (String term : lefts) {
            mergedTerms.add(term);
        }
        for (String term : rights) {
            mergedTerms.add(term);
        }
        for (String term : mergedTerms) {
            if (isTermInBothLists(term, lefts, rights)) {
                countOfTermInBothList++;
            }
        }
        if (countOfTermInBothList < 1.0d) {
            return 0.0d;
        }

        double leftSize = lefts.size();
        double rightSize = rights.size();
        leftSize = (leftSize > 1.9d) ? Math.log10(leftSize) : 0.0d;
        rightSize = (rightSize > 1.9d) ? Math.log10(rightSize) : 0.0d;
        return (leftSize + rightSize > 0.0d) ? countOfTermInBothList / (leftSize + rightSize) : 0.0d;

    }

    /**
     * this method calculate a score for terms with textRank algorithm
     *
     * @param bodyContentHandler
     * @param wordWindowSize by default 4
     * @param iteration by default 30
     * @return new reference of {@code Map<String, Double>} (term : score)
     * @throws IOException
     */
    public Map<String, Double> getTermsAndScores(BodyContentHandler bodyContentHandler, @Nullable Integer wordWindowSize, @Nullable Integer iteration)
            throws IOException {

        this.withVectorNormalize = false;
        int currentwordWindowsSize = (wordWindowSize == null) ? wordWindowSize : 4;
        int currentIeteration = (iteration == null) ? iteration : 30;
        double d = 0.85d;
        this.withMatrixNormalize = true;
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

    private boolean isTermInBothLists(String term, List<String> leftTerms, List<String> rightTerms) {

        if (leftTerms.contains(term) && rightTerms.contains(term)) {
            return true;
        }
        return false;
    }

    /**
     * this method, return a matrix as a formatted string <br>
     * {@code if (N > 15) {return "matrix to large!"} <br>
     * is for test @param textRankMatrix @return matrix
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
            List<String> terms = textProcessing.tryToCleanSentence(sentence, isOpenNLP, null);
            List<Pair<String, String>> pairs = textProcessing.getPOSTags(terms);
            filteredSentences = textProcessing.filterByPosTags(pairs);
            filteredSentences = textProcessing.filterByStopWords(filteredSentences);
            if (filteredSentences.size() >= wordWindowsSize) {
                results.add(filteredSentences);
            }

        }
        logger.info(MessageFormat.format("text corpus transformation completed contains {0} sentences ", results.size()));
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
