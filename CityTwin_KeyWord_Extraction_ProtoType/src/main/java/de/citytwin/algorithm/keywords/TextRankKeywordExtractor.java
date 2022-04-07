package de.citytwin.algorithm.keywords;

import de.citytwin.config.ApplicationConfiguration;
import de.citytwin.keywords.KeywordExtractor;
import de.citytwin.text.TextProcessing;

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
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.traverse.DepthFirstIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class is a keyword extractor by textRank <br>
 *
 * @see <a href=https://web.eecs.umich.edu/~mihalcea/papers/mihalcea.emnlp04.pdf> textRank Paper</a>
 * @author maik siegmund, FH Erfurt
 */
public class TextRankKeywordExtractor implements KeywordExtractor, AutoCloseable {

    /**
     * This inner class represent textrank matrix contains a graph and this adjazenz matrix
     *
     * @author maik siegmund, FH Erfurt
     */
    public class TextRankMatrix implements AutoCloseable {

        private transient Graph<String, DefaultWeightedEdge> graph;
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
         * constructor.
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

        @Override
        public void close() throws Exception {
            graph = null;
            indexOfTerm.clear();
            indexOfTerm = null;
            matrix = null;

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

    public final static String IN = "in";
    public final static String OUT = "out";

    private static transient final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final double DEFAULT_EDGE_WEIGHT = 1.0d;
    private static final double D = 0.85d;

    /**
     * this method returns default properties
     *
     * @return
     */
    public static Properties getDefaultProperties() {
        Properties properties = new Properties();
        properties.setProperty(ApplicationConfiguration.MIN_TERM_COUNT, "5");
        properties.setProperty(ApplicationConfiguration.WORD_WINDOW_SIZE, "5");
        properties.setProperty(ApplicationConfiguration.ITERATION, "10");
        properties.setProperty(ApplicationConfiguration.WITH_VECTOR_NORMALIZATION, "true");
        return properties;
    }

    private Graph<String, DefaultWeightedEdge> graph = null;
    private TextRankMatrix textRankMatrix = null;
    private TextProcessing textProcessing = null;
    private Integer minTermCount = null;
    private Integer wordWindowSize = null;
    private Integer iteration = null;
    private Boolean withVectorNormalization = null;

    /**
     * constructor.
     *
     * @param properties = {@code TextRankKeywordExtractor.getDefaultProperties()}
     * @param textProcessing
     * @throws IOException
     */
    public TextRankKeywordExtractor(Properties properties, TextProcessing textProcessing) throws IOException {

        if (validateProperties(properties)) {
            this.textProcessing = textProcessing;
        }

    }

    /**
     * this method build a graph by textrank algorithm terms are Vertices
     *
     * @param textCorpus {@code List<List<String>> senetences} <br>
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
    private Graph<String, DefaultWeightedEdge> buildGraph(List<List<String>> textCorpus, int wordWindowsSize) {
        Graph<String, DefaultWeightedEdge> result = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        String[] wordwindow = new String[wordWindowsSize]; // holds vertices
        String vertex = "";
        int slidingWordIndex = 0;

        for (List<String> sentence : textCorpus) {
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
        LOGGER.info(MessageFormat.format("graph completed contains {0} nodes.", result.vertexSet().size()));
        return result;
    }

    /**
     * this method calculate textrank score
     *
     * @param d
     * @return new reference of {@code Map<String, Double>} (term : score)
     */
    private Map<String, Double> calculateScore(Double d, Integer iteration, Boolean withVectorNormalization) {
        this.textRankMatrix = new TextRankMatrix(graph, withVectorNormalization);
        Map<String, Double> results = new HashMap<String, Double>(textRankMatrix.getValues().size());
        Double[] columnVector = new Double[textRankMatrix.getValues().size()];
        Double[] tempVector = new Double[textRankMatrix.getValues().size()];
        Double[] rowVector = null;
        Double value = 0.0d;
        int index = 0;
        // initialize vectors
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
            // avoid number overflow
            columnVector = (withVectorNormalization) ? textRankMatrix.normVector(tempVector) : tempVector.clone();
            LOGGER.info(MessageFormat.format("iteration {0} of {1}.", currentInteration, iteration));
        }
        index = 0;
        for (String key : textRankMatrix.values.keySet()) {
            results.put(key, columnVector[index++]);
        }
        LOGGER.info("score calculation completed.");
        return results;
    }

    @Override
    public void close() throws Exception {
        graph = null;
        textRankMatrix.close();
    }

    @Override
    public Map<String, Double> getKeywords(List<List<String>> textcorpus) throws Exception {

        List<List<String>> preparedTextCorpus = prepareText(textcorpus, minTermCount);
        graph = buildGraph(preparedTextCorpus, wordWindowSize);
        Map<String, Double> scores = calculateScore(D, iteration, withVectorNormalization);

        Map<String, Double> sortedMap = scores.entrySet()
                .stream()
                .sorted(Comparator.comparingDouble(v -> -v.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> {
                    throw new AssertionError();
                }, LinkedHashMap::new));
        LOGGER.info(MessageFormat.format("keywords found:   {0}", sortedMap.size()));
        return sortedMap;
    }

    /**
     * this method get in- and out-bounds terms of each term in graphF <br>
     * example maxLinks = 3; <br>
     *
     * @param keywords
     * @param maxLinks
     * @return {@code Map<String, Map<String, List<String>>>} <br>
     *         (keyword : in : List(...) and out : List(...))
     */
    public Map<String, Map<String, List<String>>> getLinkedTerms(Map<String, Double> keywords, Integer maxLinks) {

        Map<String, Map<String, List<String>>> result = new HashMap<String, Map<String, List<String>>>();
        List<String> inbounds = null;
        List<String> outbounds = null;
        for (Map.Entry<String, Double> entry : keywords.entrySet()) {
            // seeking vertices
            Set<DefaultWeightedEdge> outEdges = graph.outgoingEdgesOf(entry.getKey());
            Set<DefaultWeightedEdge> inEdges = graph.incomingEdgesOf(entry.getKey());
            int count = 0;
            inbounds = new ArrayList<String>();
            for (DefaultWeightedEdge defaultWeightedEdge : inEdges) {
                if (count++ > maxLinks) {
                    break;
                }
                inbounds.add(graph.getEdgeSource(defaultWeightedEdge));
            }
            count = 0;
            outbounds = new ArrayList<String>();
            for (DefaultWeightedEdge defaultWeightedEdge : outEdges) {
                if (count++ > maxLinks) {
                    break;
                }
                outbounds.add(graph.getEdgeTarget(defaultWeightedEdge));

            }
            Map<String, List<String>> links = new HashMap<String, List<String>>();
            links.put(TextRankKeywordExtractor.IN, inbounds);
            links.put(TextRankKeywordExtractor.OUT, outbounds);
            result.put(entry.getKey(), links);
        }
        LOGGER.info("get linked terms completed");
        return result;
    }

    /**
     * this method calculate similarity between two sentences
     *
     * @see <a href=https://web.eecs.umich.edu/~mihalcea/papers/mihalcea.emnlp04.pdf> textRank Paper, chapter 4</a>
     * @param lefts {@code List<String>}
     * @param rights {@code List<String>}
     * @return similarity
     */
    private Double getSimilartity(List<String> lefts, List<String> rights) {

        if (lefts.size() == 0 || rights.size() == 0) {
            return 0.0d;
        }
        double countOfEqualsTerms = 0;

        Set<String> mergedTerms = new HashSet<String>();
        for (String term : lefts) {
            mergedTerms.add(term);
        }
        for (String term : rights) {
            mergedTerms.add(term);
        }
        for (String term : mergedTerms) {
            if (isTermInBoth(term, lefts, rights)) {
                countOfEqualsTerms++;
            }
        }
        if (countOfEqualsTerms < 1.0d) {
            return 0.0d;
        }
        double leftSize = lefts.size();
        double rightSize = rights.size();
        leftSize = (leftSize > 1.9d) ? Math.log10(leftSize) : 0.0d;
        rightSize = (rightSize > 1.9d) ? Math.log10(rightSize) : 0.0d;
        return (leftSize + rightSize > 0.0d) ? countOfEqualsTerms / (leftSize + rightSize) : 0.0d;

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
     * this method checks whether a term is member of both sentences
     *
     * @param term
     * @param leftTerms
     * @param rightTerms
     * @return
     */
    private boolean isTermInBoth(String term, List<String> leftTerms, List<String> rightTerms) {

        return leftTerms.contains(term) && rightTerms.contains(term);
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
     * this method filter text corpus by pos-tags and remove stopwords
     *
     * @param sentences
     * @param minTermCount
     * @return
     */
    private List<List<String>> prepareText(final List<List<String>> sentences, int minTermCount) {

        List<List<String>> results = new ArrayList<List<String>>();
        List<String> filteredSentences = null;

        for (List<String> sentence : sentences) {

            List<Pair<String, String>> posTagged = textProcessing.getPOSTags(sentence);
            filteredSentences = textProcessing.filterByPosTags(posTagged);
            filteredSentences = textProcessing.filterByStopWords(filteredSentences);
            if (filteredSentences.size() >= minTermCount) {
                results.add(filteredSentences);
            }

        }
        LOGGER.info(MessageFormat.format("text corpus transformation completed contains {0} sentences ", results.size()));
        return results;

    }

    /**
     * this method validate passing properties and set them
     *
     * @param properties
     * @return
     * @throws IllegalArgumentException
     */
    public Boolean validateProperties(Properties properties) throws IllegalArgumentException {

        String property = null;
        property = properties.getProperty(ApplicationConfiguration.MIN_TERM_COUNT);
        if (property == null) {
            throw new IllegalArgumentException("set property --> " + "ApplicationConfiguration.MIN_TERM_COUNT");
        }
        minTermCount = Integer.parseInt(property);

        property = properties.getProperty(ApplicationConfiguration.WORD_WINDOW_SIZE);
        if (property == null) {
            throw new IllegalArgumentException("set property --> " + "ApplicationConfiguration.WORD_WINDOW_SIZE");
        }
        wordWindowSize = Integer.parseInt(property);

        property = properties.getProperty(ApplicationConfiguration.ITERATION);
        if (property == null) {
            throw new IllegalArgumentException("set property --> " + "ApplicationConfiguration.ITERATION");
        }
        iteration = Integer.parseInt(property);

        property = properties.getProperty(ApplicationConfiguration.WITH_VECTOR_NORMALIZATION);
        if (property == null) {
            throw new IllegalArgumentException("set property --> " + "ApplicationConfiguration.WITH_VECTOR_NORMALIZATION");
        }
        withVectorNormalization = Boolean.parseBoolean(property);
        return true;
    }
}
