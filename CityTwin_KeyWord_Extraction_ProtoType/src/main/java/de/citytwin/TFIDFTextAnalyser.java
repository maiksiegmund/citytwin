package de.citytwin;

import de.citytwin.textprocessing.GermanTextProcessing;

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
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.tika.sax.BodyContentHandler;
import org.javatuples.Quartet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ma6284si
 *         <p>
 *         this class provide tf idf calculation of a german textcorpus
 */
public class TFIDFTextAnalyser implements AutoCloseable {

    /**
     * This inner class represent DocumentCount only use here. used as struct ... <br>
     * Quartet {@code Map<String, Quartet<Integer, Double, String, Set<Integer>>>}
     * <p>
     * {@link DocumentCount#terms} distinct all terms of <b>D</b>
     * <p>
     * {@link DocumentCount#sentences} whole textcorpus <b>D</b> splited in sentences <b>d_i</b>
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

        public DocumentCount() {
            terms = new HashMap<String, Quartet<Integer, Double, String, Set<Integer>>>();
            sentences = new HashMap<Integer, List<String>>();
            countWords = 0;
            isNormalized = false;
        }

        public DocumentCount(DocumentCount documentCount) {
            terms = new HashMap<String, Quartet<Integer, Double, String, Set<Integer>>>(documentCount.terms);
            sentences = new HashMap<Integer, List<String>>(documentCount.sentences);
            countWords = documentCount.countWords;
            isNormalized = documentCount.isNormalized;

        }
    }

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private boolean withStemming = false;

    private GermanTextProcessing textProcessing;

    /**
     * default constructor
     *
     * @throws IOException
     */
    public TFIDFTextAnalyser() throws IOException {

        initialize();
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
            value = Math.log10((double)documentCount.sentences.size() / (double)quartet.getValue3().size());
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
        // textProcessing.close();

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
            List<Pair<String, String>> stemmedTerms = textProcessing.stemm(terms);
            tempStemmedTerms = new ArrayList<String>();
            for (Pair<String, String> stemmedTerm : stemmedTerms) {
                tempStemmedTerms.add(stemmedTerm.getRight());
            }
            result.sentences.put(index, tempStemmedTerms);
        }

        result.isNormalized = documentCount.isNormalized;
        result.countWords = documentCount.countWords;
        result.terms = result.terms;

        return getRawCount(result);
    }

    /**
     * This method calculate term frequency and inverse document frequency
     *
     * @param bodyContentHandler
     * @return new reference of {@code Map<String, Double>}
     * @throws IOException
     */
    public Map<String, Double> getTermsAndScores(final BodyContentHandler bodyContentHandler) throws IOException {
        Map<String, Quartet<Integer, Double, String, Set<Integer>>> temps = getTermsCountScoreStemmOccurrence(bodyContentHandler);
        Map<String, Double> results = new HashMap<String, Double>(temps.size());
        Quartet<Integer, Double, String, Set<Integer>> quartet = null;
        for (String term : temps.keySet()) {
            quartet = temps.get(term);
            results.put(term, quartet.getValue1());

        }
        return results;
    }

    /**
     * This method calculate term frequency and inverse document frequency and returns a detail result information <br>
     *
     * @see <a href=https://en.wikipedia.org/wiki/Tf%E2%80%93idf> tf idf calculation on wikipedia</a>
     * @param bodyContentHandler {@link BodyContentHandler}
     * @return new reference of {@code  Map<String, Quartet<Integer, Double, String, Set<Integer>>>} <br>
     *         (term : countOfTerm, score, sentenceIndex)
     * @throws IOException
     */
    public Map<String, Quartet<Integer, Double, String, Set<Integer>>> getTermsCountScoreStemmOccurrence(
            final BodyContentHandler bodyContentHandler) throws IOException {

        DocumentCount result = new DocumentCount();

        DocumentCount rawCount = transformText(bodyContentHandler);
        rawCount = (Config.WITH_STEMMING) ? getstemmedRawCount(rawCount) : getRawCount(rawCount);
        DocumentCount tf = null;
        tf = calculateTF(rawCount);
        switch(Config.TF_IDF_NORMALIZATION_TYPE) {
            case DOUBLE:
                tf = doubleNormalizationTermFrequency(tf, 0.5);
                break;
            case LOG:
                tf = logNormalizationTermFrequency(tf);
                break;
            case NONE:
            default:
                break;
        }
        DocumentCount idf = calculateIDF(tf);
        result = calculateTFIDF(tf, idf);

        Map<String, Quartet<Integer, Double, String, Set<Integer>>> filtered = new HashMap<>();

        for (String tagFilter : Config.GERMAN_TEXT_PROCESSING_POSTAGS) {
            for (String term : result.terms.keySet()) {
                String wordPosTag = result.terms.get(term).getValue2();
                if (wordPosTag.equals(tagFilter)) {
                    filtered.put(term, result.terms.get(term));
                }
            }
        }
        return sortbyValue(filtered, true);

    }

    /**
     * This method initialize the text processing components
     *
     * @throws IOException
     */
    private void initialize() throws IOException {

        textProcessing = new GermanTextProcessing();
    }

    public boolean isWithStemming() {
        return withStemming;
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
     * This method transfer BodyContentHandler to DocumentCount
     *
     * @param bodyContentHandler {@link BodyContentHandler}
     * @return new reference of {@link DocumentCount}
     */
    private DocumentCount transformText(final BodyContentHandler bodyContentHandler) throws IOException {

        DocumentCount result = new DocumentCount();
        int count = 0;
        List<String> sentences = textProcessing.tokenizeBodyContentToSencences(bodyContentHandler);
        List<String> terms = null;
        int sentenceIndex = 0;
        for (String senctence : sentences) {
            terms = textProcessing.tryToCleanSentence(senctence);
            terms = (Config.WITH_STOPWORD_FILTER) ? textProcessing.filterByStopWords(terms) : terms;
            result.sentences.put(sentenceIndex++, terms);
            count += terms.size();
        }
        result.countWords = count;
        result.isNormalized = false;
        return result;
    }
}
