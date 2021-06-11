package de.citytwin;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.tika.sax.BodyContentHandler;
import org.javatuples.Quartet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.stemmer.snowball.SnowballStemmer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

/**
 * @author ma6284si
 *         <p>
 *         this class provide tf idf calculation of a german textcorpus
 */
public class TFIDFTextAnalyser {

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
        // term, count, calculation, , postag, sentenceindex
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

    /**
     * This inner enum to choose which normalization is use ...
     * <p>
     *
     * @see <a href=https://en.wikipedia.org/wiki/Tf%E2%80%93idf> tf idf calculation on wikipedia</a>
     */
    public static enum NormalizationType {
        DOUBLE, LOG, NONE
    }

    private static boolean isInitialzied = false;
    private static boolean isOpenNLP = false;
    private static POSTaggerME posTagger;
    private static SentenceDetectorME sentenceDetector;
    private static Set<String> stopwords = new HashSet<String>();
    private static Tokenizer tokenizer;
    private static boolean withStopWordFilter = false;

    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private int minLenght = 2;
    private int maxNewLines = 10;
    private String cleaningPattern = "[^a-zA-ZäÄüÜöÖß-]";

    private String tokenizerName = "whitespace";

    private boolean withStemming = false;

    /**
     * constructor with <b> VERY SIMPLE</b> fluent pattern, no plausibility check
     * <p>
     * example
     * <p>
     * {@code new TFIDFTextAnalyser.withLucene().withStemming().withStopwordFilter();}
     * <p>
     *
     * @throws IOException
     */
    public TFIDFTextAnalyser() throws IOException {

        if (!isInitialzied)
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
     * This method calculate term frequency of a text. used the stopwords
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
     * This method calculate term frequency and inverse document frequency <br>
     *
     * @see <a href=https://en.wikipedia.org/wiki/Tf%E2%80%93idf> tf idf calculation on wikipedia</a>
     * @param bodyContentHandler {@link BodyContentHandler}
     * @param tagFilters {@code List<String> } @Nullable
     * @param type {@link TFIDFTextAnalyser#NormalizationType}
     * @return new reference of {@link Map}
     * @throws IOException
     */
    public Map<String, Quartet<Integer, Double, String, Set<Integer>>> calculateTFIDF(
            final BodyContentHandler bodyContentHandler, final List<String> tagFilters,
            TFIDFTextAnalyser.NormalizationType type) throws IOException {

        DocumentCount result = new DocumentCount();

        DocumentCount rawCount = prepareText(bodyContentHandler);
        rawCount = (withStemming) ? getstemmedRawCount(rawCount) : getRawCount(rawCount);
        DocumentCount tf = null;
        tf = calculateTF(rawCount);
        switch(type) {
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
        if (tagFilters == null || tagFilters.size() == 0) {
            return sortbyValue(result.terms, true);
        }

        Map<String, Quartet<Integer, Double, String, Set<Integer>>> filterd = new HashMap<>();

        for (String tagFilter : tagFilters) {
            for (String term : result.terms.keySet()) {
                String wordPosTag = result.terms.get(term).getValue2();
                if (wordPosTag.equals(tagFilter)) {
                    filterd.put(term, result.terms.get(term));

                }
            }
        }

        return sortbyValue(filterd, true);

    }

    /**
     * This method calculate tfidf. Equation = tfidf(t,d,D) = tf(t,d) * idf(t,D)
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

    /**
     * This method normalized a term freuency. <br>
     * equation = k +( 1 - k) f(t,d) / (max(f(t,d)))
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
        int tagIndex = 0;

        for (Integer index : documentCount.sentences.keySet()) {
            List<String> terms = documentCount.sentences.get(index);
            String[] stringArray = new String[terms.size()];
            stringArray = terms.toArray(stringArray);
            String[] tags = posTagger.tag(stringArray);

            for (String term : terms) {
                result.countWords++;
                quartet = Quartet.with(1, 0.0, tags[tagIndex], new HashSet<Integer>());
                tagIndex++;
                if (!result.terms.containsKey(term)) {
                    result.terms.put(term, quartet);
                    quartet.getValue3().add(sentenceIndex);
                    continue;
                }
                quartet = result.terms.get(term);
                quartet = quartet.setAt0(quartet.getValue0() + 1);
                quartet.getValue3().add(sentenceIndex);
                result.terms.put(term, quartet);
            }
            sentenceIndex++;
            tagIndex = 0;
        }
        result.sentences = documentCount.sentences;
        result.isNormalized = documentCount.isNormalized;
        logger.info(MessageFormat.format("document contains {0} terms.", result.terms.size()));
        logger.info(MessageFormat.format("document contains {0} sentences.", result.sentences.size()));
        logger.info(MessageFormat.format("document contains {0} words.", result.countWords));

        return result;
    }

    /**
     * This method stem all terms in {@link DocumentCount#sentences}.
     * <p>
     * stemmed by {@link opennlp.tools.stemmer.snowball.SnowballStemmer} <br>
     *
     * @param documentCount
     * @return new reference of {@link DocumentCount}
     */
    private DocumentCount getstemmedRawCount(final DocumentCount documentCount) throws IOException {

        DocumentCount result = new DocumentCount();
        SnowballStemmer snowballStemmerOpenNLP = new SnowballStemmer(SnowballStemmer.ALGORITHM.GERMAN);
        String stemmed = "";

        for (Integer index : documentCount.sentences.keySet()) {
            List<String> terms = documentCount.sentences.get(index);
            List<String> stemmedterms = new ArrayList<String>(terms.size());
            for (String term : terms) {
                stemmed = snowballStemmerOpenNLP.stem(term).toString();
                stemmedterms.add(stemmed);
            }
            result.sentences.put(index, stemmedterms);
        }

        result.isNormalized = documentCount.isNormalized;
        result.countWords = documentCount.countWords;
        result.terms = result.terms;

        return getRawCount(result);
    }

    /**
     * This method initialize the opennlp components and the stopword list
     * <p>
     * {@link TFIDFTextAnalyser#posTagger}
     * <p>
     * {@link TFIDFTextAnalyser#sentenceDetector}
     * <p>
     * {@link TFIDFTextAnalyser#tokenizer}
     * <p>
     * {@link TFIDFTextAnalyser#stopwords}
     * <p>
     *
     * @return new reference of {@link DocumentCount}
     * @throws IOException
     */
    private void initialize() throws IOException {

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("de-sent.bin");
        SentenceModel sentenceModel = new SentenceModel(inputStream);
        sentenceDetector = new SentenceDetectorME(sentenceModel);
        inputStream.close();

        // inputStream = getClass().getClassLoader().getResourceAsStream("de-pos-maxent.bin");
        inputStream = getClass().getClassLoader().getResourceAsStream("de-pos-perceptron.bin");
        posTagger = new POSTaggerME(new POSModel(inputStream));
        inputStream.close();

        inputStream = getClass().getClassLoader().getResourceAsStream("de-token.bin");
        TokenizerModel tokenizerModel = new TokenizerModel(inputStream);
        tokenizer = new TokenizerME(tokenizerModel);

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
     * This method prepare the raw text into sentences <br>
     * split by apache opennlp or apache lucene
     * <p>
     * and use a stopword filter
     *
     * @param bodyContentHandler {@link BodyContentHandler}
     * @return new reference of {@link DocumentCount}
     */
    private DocumentCount prepareText(BodyContentHandler bodyContentHandler) throws IOException {

        DocumentCount result = new DocumentCount();
        int count = 0;
        List<String> sentences = spliteBodyContentToSencences(bodyContentHandler);
        List<String> terms = null;
        for (int index = 0; index < sentences.size(); ++index) {
            terms = (isOpenNLP) ? splitSentenceOpenNLP(sentences.get(index))
                    : splitSentenceLucene(sentences.get(index));
            terms = (withStopWordFilter) ? filterByStopWords(terms) : terms;
            result.sentences.put(index, terms);
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
     * This method split textcorpus in each senteces by opennlp
     * <p>
     * remove newline pattern {@code "-\n"} and
     * <p>
     * newline threshold is {@link TFIDFTextAnalyser#maxNewLines}
     * <p>
     *
     * @param bodyContentHandler
     * @return {@code List<String>}
     * @throws IOException
     */
    private List<String> spliteBodyContentToSencences(BodyContentHandler bodyContentHandler) throws IOException {

        List<String> results = new ArrayList<>();
        String temp = "";
        String[] sentences = sentenceDetector.sentDetect(bodyContentHandler.toString());
        // remove hyponated tags -\n
        for (String sentence : sentences) {
            // table of content contains many newlines, simple approach the remove them
            if (countNewLines(sentence) < maxNewLines) {
                temp = sentence.replaceAll("-\n", "");
                results.add(temp);
            }

        }
        logger.info(MessageFormat.format("textcorpus contains {0} sentences.", results.size()));
        return results;
    }

    /**
     * This count new lines in a sentence,
     * <p>
     *
     * @param sentence
     * @return {@code List<String>}
     * @throws IOException
     */
    private int countNewLines(String sentence) {
        char[] chars = sentence.toCharArray();
        int count = 0;
        for (char ch : chars) {
            if (ch == '\n')
                count++;
        }
        return count;
    }

    /**
     * This method split sentences in each terms
     *
     * @param sentence
     * @return {@code List<String>}
     * @throws IOException
     */
    private List<String> splitSentenceLucene(final String sentence) throws IOException {

        String temp = "";
        List<String> results = new ArrayList<String>();
        Analyzer analyzer;

        analyzer = CustomAnalyzer.builder().withTokenizer(tokenizerName).build();
        TokenStream stream = analyzer.tokenStream(null, new StringReader(sentence));
        CharTermAttribute attr = stream.addAttribute(CharTermAttribute.class);
        stream.reset();
        while (stream.incrementToken()) {
            temp = attr.toString().trim().replaceAll(cleaningPattern, "");
            if (temp.length() > minLenght) {
                results.add(temp);
            }
        }
        // logger.info(MessageFormat.format("sentence contains {0} words.",
        // results.size()));
        analyzer.close();
        stream.close();
        return results;
    }

    /**
     * This method split a sentence in each term.
     *
     * @param sentence
     * @return {@code List<String>}
     */
    private List<String> splitSentenceOpenNLP(final String sentence) {

        String temp = "";
        List<String> results = new ArrayList<String>();
        String[] sentences = tokenizer.tokenize(sentence);
        for (String word : sentences) {
            temp = word.trim().replaceAll(cleaningPattern, "");
            if (temp.length() > minLenght) {
                results.add(temp);
            }

        }
        // logger.info(MessageFormat.format("sentence contains {0} words",
        // results.size()));
        return results;
    }

    /**
     * use lucene tokenizer
     *
     * @return new reference of {@link TFIDFTextAnalyser}
     */
    public TFIDFTextAnalyser withLucene() {
        TFIDFTextAnalyser.isOpenNLP = false;
        return this;
    }

    /**
     * use opennlp tokenizer
     *
     * @return new reference of {@link TFIDFTextAnalyser}
     */
    public TFIDFTextAnalyser withOpenNLP() {
        TFIDFTextAnalyser.isOpenNLP = true;
        return this;
    }

    /**
     * use snowball stemmer for each term in textcorpus
     *
     * @return new reference of {@link TFIDFTextAnalyser}
     */
    public TFIDFTextAnalyser withStemming() {
        this.withStemming = true;
        return this;
    }

    /**
     * remove occurre stopwords from textcorpus
     *
     * @return new reference of {@link TFIDFTextAnalyser}
     */
    public TFIDFTextAnalyser withStopwordFilter() {
        TFIDFTextAnalyser.withStopWordFilter = true;
        return this;
    }

}
