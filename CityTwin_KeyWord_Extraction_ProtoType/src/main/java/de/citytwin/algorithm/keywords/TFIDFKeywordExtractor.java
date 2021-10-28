package de.citytwin.algorithm.keywords;

import de.citytwin.text.TextProcessing;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.tika.sax.BodyContentHandler;
import org.javatuples.Quartet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class is a keyword extrator by tf idf algorithm <br>
 *
 * @see <a href=https://en.wikipedia.org/wiki/Tf%E2%80%93idf> tf idf wiki</a>
 * @author Maik Siegmund, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class TFIDFKeywordExtractor implements KeywordExtractor, AutoCloseable {

    /**
     * inner class represent DocumentCount only use here. used as struct ... <br>
     * {@code Map<String, Quartet<Integer, Double, String, Set<Integer>>>}
     * <p>
     * {@link DocumentCount#terms} distinct all terms of <b>D</b>
     * <p>
     * {@link DocumentCount#sentences} whole text corpus <b>D</b> splited in sentences <b>d_i</b>
     * <p>
     * {@link DocumentCount#countWords} <b>|D|</b>
     * <p>
     * {@link DocumentCount#isNormalized}
     */
    class DocumentCount {

        public int countWords;

        public boolean isNormalized;
        public Map<Integer, List<String>> sentences;
        // term, count, calculation, , postag, sentenceindex (intern use)
        public Map<String, Quartet<Integer, Double, String, Set<Integer>>> terms;

        /**
         * Konstruktor.
         */
        public DocumentCount() {
            terms = new HashMap<String, Quartet<Integer, Double, String, Set<Integer>>>();
            sentences = new HashMap<Integer, List<String>>();
            countWords = 0;
            isNormalized = false;
        }

        /**
         * Konstruktor.
         *
         * @param documentCount
         */
        public DocumentCount(DocumentCount documentCount) {
            terms = new HashMap<String, Quartet<Integer, Double, String, Set<Integer>>>(documentCount.terms);
            sentences = new HashMap<Integer, List<String>>(documentCount.sentences);
            countWords = documentCount.countWords;
            isNormalized = documentCount.isNormalized;

        }
    }

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * the method return default properties
     *
     * @return
     */
    public static Properties getDefaultProperties() {
        Properties properties = new Properties();
        properties.put("withStopwordFilter", false);
        properties.put("withStemming", false);
        properties.put("normalization", "none");
        return properties;
    }

    Properties properties = null;
    // tf idf normalization factor
    private final Double k = 0.5;

    private TextProcessing textProcessing = null;

    /**
     * Konstruktor.
     *
     * @param properties = {@code TFIDFKeywordExtractor.getDefaultProperties()}
     * @param textProcessing
     * @throws IOException
     */
    public TFIDFKeywordExtractor(Properties properties, TextProcessing textProcessing) throws IOException {
        if (validateProperties(properties)) {
            this.textProcessing = textProcessing;
            this.properties = new Properties();
            this.properties.putAll(properties);
        }
    }

    /**
     * This method calculate smooth inverse document frequency.
     *
     * @see <a href=https://en.wikipedia.org/wiki/Tf%E2%80%93idf> tf idf calculation on wikipedia</a>
     * @param documentCount
     * @return new reference of {@link DocumentCount}
     * @throws IOException
     */
    private DocumentCount calculateIDF(final DocumentCount documentCount) throws IOException {

        double value = 0.0;
        DocumentCount result = new DocumentCount();
        result.sentences = documentCount.sentences;
        result.countWords = documentCount.countWords;
        result.isNormalized = documentCount.isNormalized;

        int currentIndex = 0;

        Quartet<Integer, Double, String, Set<Integer>> quartet = null;

        for (String term : documentCount.terms.keySet()) {
            quartet = documentCount.terms.get(term);
            logger.info(
                    MessageFormat.format("processing {0} of {1} terms. ", ++currentIndex, documentCount.terms.size()));
            value = Math.log10((double)documentCount.sentences.size() / quartet.getValue3().size());
            quartet = quartet.setAt1(value);
            result.terms.put(term, quartet);

        }
        logger.info("calculate smoot inverse document frequency completed.");
        return result;
    }

    /**
     * This method calculate term frequency of a text.
     *
     * @see <a href=https://en.wikipedia.org/wiki/Tf%E2%80%93idf> tf idf calculation on wikipedia</a>
     * @param bodyContentHandler {@link BodyContentHandler}
     * @return new reference of {@link DocumentCount}
     */
    private DocumentCount calculateTF(final DocumentCount documentCount) throws IOException {

        DocumentCount result = new DocumentCount();
        result.sentences = documentCount.sentences;
        result.isNormalized = documentCount.isNormalized;
        result.countWords = documentCount.countWords;

        Map<String, Integer> countOfTermInSentence = new HashMap<String, Integer>();
        Quartet<Integer, Double, String, Set<Integer>> quartet = null;
        for (Integer index : documentCount.sentences.keySet()) {
            List<String> terms = documentCount.sentences.get(index);
            int countTermsInSentence = terms.size();
            // count terms in sentences (d_i) of D
            for (String term : terms) {
                if (!countOfTermInSentence.containsKey(term)) {
                    countOfTermInSentence.put(term, 1);
                    continue;
                }
                countOfTermInSentence.put(term, countOfTermInSentence.get(term) + 1);
            }
            // calculate tf score
            for (String term : terms) {
                quartet = documentCount.terms.get(term);
                quartet = quartet.setAt1((double)countOfTermInSentence.get(term) / (double)countTermsInSentence);
                result.terms.put(term, quartet);
            }
            countOfTermInSentence.clear();
        }
        logger.info("calculate term frequency finished.");
        result.isNormalized = false;
        return result;
    }

    /**
     * This method calculate tfidf. <br>
     * equation <strong> fidf(t,d,D) = tf(t,d) * idf(t,D) </strong>
     *
     * @see <a href=https://en.wikipedia.org/wiki/Tf%E2%80%93idf> tf idf calculation on wikipedia</a>
     * @param tf {@link DocumentCount}
     * @param idf {@link DocumentCount}
     * @return new reference of {@link DocumentCount}
     */
    private DocumentCount calculateTFIDF(final DocumentCount tf, final DocumentCount idf) {
        double value = 0.0;
        DocumentCount result = new DocumentCount();
        result.sentences = tf.sentences;
        result.isNormalized = tf.isNormalized;
        result.countWords = tf.countWords;

        Quartet<Integer, Double, String, Set<Integer>> quartetTf = null;
        Quartet<Integer, Double, String, Set<Integer>> quartetIdf = null;

        for (String term : tf.terms.keySet()) {
            quartetTf = tf.terms.get(term);
            quartetIdf = idf.terms.get(term);
            value = quartetTf.getValue1() * quartetIdf.getValue1();
            quartetTf = quartetTf.setAt1(value);
            result.terms.put(term, quartetTf);
        }
        logger.info("caculation tf idf completed");
        return result;
    }

    @Override
    public void close() throws Exception {
        properties.clear();
        this.properties = null;

    }

    /**
     * This method normalized a term freuency. <br>
     * equation <strong> k +( 1 - k) f(t,d) / (max(f(t,d))) </strong>
     *
     * @see <a href=https://en.wikipedia.org/wiki/Tf%E2%80%93idf> tf idf calculation on wikipedia</a>
     * @param documentCount
     * @param k
     * @return new reference of {@link DocumentCount}
     * @throws IllegalArgumentException
     */
    private DocumentCount doubleNormalizationTermFrequency(final DocumentCount documentCount, final double k) {
        if (documentCount.isNormalized) {
            throw new IllegalArgumentException("document already normalized!");
        }
        if (k < 0.0 && k > 1.0) {
            throw new IllegalArgumentException("k only in range 0.1 - 1.0");
        }
        double value = 0.0;
        DocumentCount result = new DocumentCount();
        result.sentences = documentCount.sentences;
        result.countWords = documentCount.countWords;
        Map<String, Quartet<Integer, Double, String, Set<Integer>>> sorted = sortbyValue(documentCount.terms, true);

        Quartet<Integer, Double, String, Set<Integer>> max = sorted.entrySet().iterator().next().getValue();
        Quartet<Integer, Double, String, Set<Integer>> quartet = null;

        for (String term : documentCount.terms.keySet()) {

            quartet = documentCount.terms.get(term);
            value = k + (1.0 - k) * (quartet.getValue1().doubleValue() / max.getValue1().doubleValue());
            quartet = quartet.setAt1(value);
            result.terms.put(term, quartet);
        }
        result.isNormalized = true;
        logger.info("double normalization completed.");
        return result;
    }

    @Override
    public Map<String, Double> getKeywords(List<List<String>> textcorpus) throws Exception {

        HashMap<String, Double> extractedKeywords = new HashMap<String, Double>();
        Boolean withStopwordFilter = (Boolean)properties.get("withStopwordFilter");
        Boolean withStemming = (Boolean)properties.get("withStemming");
        String normalization = (String)properties.get("normalization");

        DocumentCount tfidf = new DocumentCount();
        DocumentCount rawCount = prepareText(textcorpus, withStopwordFilter);
        rawCount = (withStemming) ? getstemmedRawCount(rawCount) : getRawCount(rawCount);

        DocumentCount tf = null;
        tf = calculateTF(rawCount);
        switch(normalization.toLowerCase()) {
            case "double":
                tf = doubleNormalizationTermFrequency(tf, k);
                break;
            case "log":
                tf = logNormalizationTermFrequency(tf);
                break;
            case "none":
            default:
                break;
        }
        DocumentCount idf = calculateIDF(tf);
        tfidf = calculateTFIDF(tf, idf);

        List<String> temp = textProcessing.getPosTags();

        for (String posTag : temp) {
            for (String term : tfidf.terms.keySet()) {
                String posTagOfTerm = tfidf.terms.get(term).getValue2();
                if (posTagOfTerm.equals(posTag)) {
                    extractedKeywords.put(term, tfidf.terms.get(term).getValue1());
                }
            }
        }
        Map<String, Double> sortedMap = extractedKeywords.entrySet()
                .stream()
                .sorted(Comparator.comparingDouble(v -> -v.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> {
                    throw new AssertionError();
                }, LinkedHashMap::new));
        return sortedMap;

    }

    /**
     * This method calculate the raw count and set the pos tag of each term. <br>
     * {@link opennlp.tools.stemmer.snowball.SnowballStemmer}
     *
     * @param bodyContentHandler {@link BodyContentHandler}
     * @return new reference of {@link DocumentCount}
     */
    private DocumentCount getRawCount(DocumentCount documentCount) throws IOException {

        int sentenceIndex = 0;
        DocumentCount result = new DocumentCount();
        Quartet<Integer, Double, String, Set<Integer>> quartet = null;

        for (Integer index : documentCount.sentences.keySet()) {
            List<Pair<String, String>> terms = textProcessing.getPOSTags(documentCount.sentences.get(index));
            for (Pair<String, String> term : terms) {
                result.countWords++;
                quartet = Quartet.with(1, 0.0, term.getRight(), new HashSet<Integer>());
                if (!result.terms.containsKey(term.getLeft())) {
                    result.terms.put(term.getLeft(), quartet);
                    quartet.getValue3().add(sentenceIndex);
                    continue;
                }
                quartet = result.terms.get(term.getLeft());
                quartet = quartet.setAt0(quartet.getValue0() + 1);
                quartet.getValue3().add(sentenceIndex);
                result.terms.put(term.getLeft(), quartet);
            }
            sentenceIndex++;

        }
        result.sentences = documentCount.sentences;
        result.isNormalized = documentCount.isNormalized;
        logger.info(MessageFormat.format("document contains {0} terms overall.", result.terms.size()));
        logger.info(MessageFormat.format("document contains {0} sentences.", result.sentences.size()));
        logger.info(MessageFormat.format("document contains {0} words distinct.", result.countWords));

        return result;
    }

    /**
     * This method stemm all terms in {@link DocumentCount#sentences}.
     *
     * @param documentCount
     * @return new reference of {@link DocumentCount}
     */
    private DocumentCount getstemmedRawCount(final DocumentCount documentCount) throws IOException {

        DocumentCount result = new DocumentCount();
        List<String> tempStemmedTerms = null;

        for (Integer index : documentCount.sentences.keySet()) {
            List<String> terms = documentCount.sentences.get(index);
            Map<String, String> stemmedTerms = textProcessing.stemm(terms);
            tempStemmedTerms = new ArrayList<String>();
            for (String term : terms) {
                tempStemmedTerms.add(stemmedTerms.get(term));
            }
            result.sentences.put(index, tempStemmedTerms);
        }

        result.isNormalized = documentCount.isNormalized;
        result.countWords = documentCount.countWords;
        result.terms = result.terms;

        return getRawCount(result);
    }

    /**
     * This method normalized a term frequency.<br>
     * equation = log(1+f(t,d))
     *
     * @see <a href=https://en.wikipedia.org/wiki/Tf%E2%80%93idf> tf idf calculation on wikipedia</a>
     * @param documentCount {@link DocumentCount}
     * @return new reference of {@link DocumentCount}
     * @throws IllegalArgumentException
     */
    private DocumentCount logNormalizationTermFrequency(final DocumentCount documentCount) {
        if (documentCount.isNormalized) {
            throw new IllegalArgumentException("document already normalized!");
        }
        DocumentCount result = new DocumentCount();
        result.sentences = documentCount.sentences;
        result.countWords = documentCount.countWords;

        Quartet<Integer, Double, String, Set<Integer>> quartet = null;

        for (String term : documentCount.terms.keySet()) {
            quartet = documentCount.terms.get(term);
            quartet = quartet.setAt1(Math.log10(1.0 + quartet.getValue1().doubleValue()));
            result.terms.put(term, quartet);
        }
        result.isNormalized = true;
        logger.info("log normalization completed.");
        return result;
    }

    /**
     * This method prepare text corpus to tf idf calculation
     *
     * @param textCorpus
     * @param filterByStopWords
     * @return
     * @throws IOException
     */
    private DocumentCount prepareText(final List<List<String>> textCorpus, Boolean filterByStopWords) throws IOException {

        DocumentCount result = new DocumentCount();
        int count = 0;
        int sentenceIndex = 0;
        for (List<String> senctence : textCorpus) {
            List<String> terms = filterByStopWords ? textProcessing.filterByStopWords(senctence) : senctence;
            result.sentences.put(sentenceIndex++, terms);
            count += terms.size();
        }
        result.countWords = count;
        result.isNormalized = false;
        return result;
    }

    /**
     * This method sort a map by {@link Quartet#getValue1()}
     * <p>
     * term, count, calculation, postag, sentence indices
     * <p>
     * Quartet {@code Map<String, Quartet<Integer, Double, String, Set<Integer>>>}
     * <p>
     *
     * @param map {@code Map<String, Quartet<Integer, Double, String, Set<Integer>>>}
     * @param isDescending {@code true} or {@code false}
     * @return new reference of {@code Map<String, Quartet<Integer, Double, String, Set<Integer>>>}
     */
    private Map<String, Quartet<Integer, Double, String, Set<Integer>>> sortbyValue(
            final Map<String, Quartet<Integer, Double, String, Set<Integer>>> map, boolean isDescending) {

        double negation = (isDescending) ? -1.0 : 1.0;

        Map<String, Quartet<Integer, Double, String, Set<Integer>>> sortedMap = map.entrySet()
                .stream()
                .sorted(Comparator.comparingDouble(value -> negation * value.getValue().getValue1().doubleValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> {
                    throw new AssertionError();
                }, LinkedHashMap::new));
        return sortedMap;

    }

    /**
     * this method validate passing properties
     *
     * @param properties
     * @return
     * @throws IOException
     */
    public Boolean validateProperties(Properties properties) throws IOException {
        Boolean withStopwordFilter = (Boolean)properties.get("withStopwordFilter");
        if (withStopwordFilter == null) {
            throw new IOException("set property --> withStopwordFilter as Boolean");
        }
        Boolean withStemming = (Boolean)properties.get("withStemming");
        if (withStemming == null) {
            throw new IOException("set property --> withStemming as Boolean");
        }
        String normalization = (String)properties.get("normalization");
        if (normalization == null) {
            throw new IOException("set property --> normalization as String (none, log, double)");
        }
        return true;
    }
}
