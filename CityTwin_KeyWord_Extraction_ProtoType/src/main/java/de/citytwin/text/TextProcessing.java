/* @filename $RCSfile: GermanTextProcessing.java,v $ */

package de.citytwin.text;

import de.citytwin.config.ApplicationConfiguration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
// import org.apache.lucene.analysis.Analyzer;
// import org.apache.lucene.analysis.TokenStream;
// import org.apache.lucene.analysis.custom.CustomAnalyzer;
// import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.tika.sax.BodyContentHandler;
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
 * this class provides text processing methods
 *
 * @author Maik Siegmund, FH Erfurt
 */
public class TextProcessing implements AutoCloseable {

    private static transient final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * this method create default properties
     *
     * @return {@code Properties}
     */
    public static Properties getDefaultProperties() {
        Properties properties = new Properties();
        properties.setProperty(ApplicationConfiguration.PATH_2_SENTENCE_DETECTOR_FILE, "..\\de-sent.bin");
        properties.setProperty(ApplicationConfiguration.PATH_2_POS_TAGGER_FILE, "..\\de-posperceptron.bin");
        properties.setProperty(ApplicationConfiguration.PATH_2_SENTENCE_TOKENIZER_FILE, "..\\de-token.bin");
        properties.setProperty(ApplicationConfiguration.PATH_2_STOPWORDS_FILE, "..\\de-stopswords.txt");
        properties.setProperty(ApplicationConfiguration.PATH_2_POSTAGS_FILE, "..\\de-postags.txt");
        properties.setProperty(ApplicationConfiguration.CLEANING_PATTERN, "[^\\u2013\\u002D\\wäÄöÖüÜß,-/]");
        properties.setProperty(ApplicationConfiguration.MIN_TERM_LENGTH, "2");
        properties.setProperty(ApplicationConfiguration.MIN_TERM_COUNT, "5");
        properties.setProperty(ApplicationConfiguration.MIN_TABLE_OF_CONTENT, "50");
        return properties;
    }

    private boolean isInitialzied = false;
    private POSTaggerME posTagger = null;
    private SentenceDetectorME sentenceDetector = null;
    private Set<String> stopwords = new HashSet<String>();
    private Set<String> posTags = new HashSet<String>();
    private Tokenizer tokenizer = null;
    private SnowballStemmer snowballStemmer = null;
    private String path2SentenceDetectorFile = null;
    private String path2PosTaggerFile = null;
    private String path2SentenceTokenizerFile = null;
    private String path2StopwordsFile = null;
    private String path2PosTagsFile = null;

    /**
     * Constructor.
     *
     * @param properties {@code TextProcessing.getDefaultProperties()}
     * @throws IOException
     */
    public TextProcessing(Properties properties) throws IOException {

        if (validateProperties(properties)) {
            if (!isInitialzied) {
                initialize();
            }
        }

    }

    @Override
    public void close() throws Exception {
        this.posTagger = null;
        this.posTags.clear();
        this.posTags = null;
        this.sentenceDetector = null;
        this.snowballStemmer = null;
        this.stopwords.clear();
        this.stopwords = null;
        this.tokenizer = null;
    }

    /**
     * this method count a char in a sentence.
     *
     * @param sentence
     * @param charToCount
     * @return
     */
    public int countChar(String sentence, char charToCount) {
        char[] chars = sentence.toCharArray();
        int count = 0;
        for (char ch : chars) {
            if (ch == charToCount) {
                count++;
            }
        }
        return count;
    }

    /**
     * this method count digits in a sentence
     *
     * @param sentence
     * @return
     */
    public int countDigits(String sentence) {
        char chars[] = sentence.toCharArray();
        int result = 0;
        for (char ch : chars) {
            if (Character.isDigit(ch)) {
                result++;
            }
        }
        return result;
    }

    /**
     * this method count new lines in a sentence,
     *
     * @param sentence
     * @return {@code int}
     */
    public int countNewLines(String sentence) {
        return countChar(sentence, '\n');
    }

    /**
     * this method keeps terms they tagged by necessary pos-tag and include in pos-list
     *
     * @param termsWithPosTags <br>
     *            {@code List<Pair<String, String>>} <br>
     *            meaning of Pair -> left is word, right is pos-tag
     * @param posTags
     *            <P>
     * @return new reference of {@code List<String>}
     */

    public List<String> filterByPosTags(final List<Pair<String, String>> termsWithPosTags) {
        List<String> results = new ArrayList<String>();
        for (Pair<String, String> pair : termsWithPosTags) {
            if (posTags.contains(pair.getRight())) {
                results.add(pair.getLeft());
            }
        }
        LOGGER.info("filter by pos tags completed. (terms keeping)");
        return results;
    }

    /**
     * this method remove terms they are in the stopword list.
     * <p>
     * {@link TextProcessing#stopwords}
     *
     * @param terms {@code List<String>}
     * @return new reference of {@code List<String>} filtered
     */
    public List<String> filterByStopWords(final List<String> terms) {

        List<String> results = new ArrayList<String>();
        for (String term : terms) {
            if (!stopwords.contains(term.toLowerCase())) {
                results.add(term);
            }
        }
        LOGGER.info("filter by stopwords completed. (terms removal)");
        return results;

    }

    /**
     * this method gets used pos tags
     *
     * @return new reference of {@code List<String>}
     */
    public List<String> getPosTags() {

        List<String> results = new ArrayList<String>();
        results.addAll(posTags);
        return results;
    }

    /**
     * this method return pos tags of a german sentence. <br>
     *
     * @param terms
     * @return new reference of {@code List<Pair<String, String>>} (term : postag)
     */
    public List<Pair<String, String>> getPOSTags(final List<String> terms) {
        List<Pair<String, String>> results = new ArrayList<Pair<String, String>>(terms.size());
        String[] strings = new String[terms.size()];
        strings = terms.toArray(strings);
        String[] tags = posTagger.tag(strings);
        int tagIndex = 0;
        for (String term : terms) {
            Pair<String, String> pair = Pair.of(term, tags[tagIndex++]);
            results.add(pair);
        }
        LOGGER.info("pos tagging completed.");
        return results;
    }

    public List<String> getStopwords() {
        List<String> stopwords = new ArrayList<String>();
        stopwords.addAll(stopwords);
        return stopwords;

    }

    /**
     * this method initialize nlp components, pos tags and stopwords
     *
     * @throws IOException
     */
    private void initialize() throws IOException {

        try(InputStream inputStream = new FileInputStream(path2SentenceDetectorFile);) {
            SentenceModel sentenceModel = new SentenceModel(inputStream);
            sentenceDetector = new SentenceDetectorME(sentenceModel);
        }
        try(InputStream inputStream = new FileInputStream(path2PosTaggerFile);) {
            posTagger = new POSTaggerME(new POSModel(inputStream));
        }
        try(InputStream inputStream = new FileInputStream(path2SentenceTokenizerFile);) {
            TokenizerModel tokenizerModel = new TokenizerModel(inputStream);
            tokenizer = new TokenizerME(tokenizerModel);
        }
        try(InputStream inputStream = new FileInputStream(path2StopwordsFile);
                Scanner scanner = new Scanner(inputStream).useDelimiter("\\r\\n");) {
            while (scanner.hasNext()) {
                String stopword = scanner.next();
                if (!stopword.trim().startsWith("#")) {
                    stopwords.add(stopword);
                }
            }
        }
        try(InputStream inputStream = new FileInputStream(path2PosTagsFile);
                Scanner scanner = new Scanner(inputStream).useDelimiter("\\r\\n");) {
            while (scanner.hasNext()) {
                String posTag = scanner.next();
                if (!posTag.trim().startsWith("#")) {
                    posTags.add(posTag);
                }
            }
        }
        snowballStemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.GERMAN);
        isInitialzied = true;
    }

    /**
     * this method calculate the probability whether sentence is table of content <br>
     * (calculate by dots, digits and blanks in sentence)
     *
     * @param sentence
     * @return {@code int} in percent
     */
    public int probabilityOfSentenceTabelofContent(final String sentence) {

        int countDots = this.countChar(sentence, '.');
        int countDigits = this.countDigits(sentence);
        int countBlanks = this.countChar(sentence, ' ');
        float accurany = (float)countDots / (float)sentence.length() * 100.0f + (float)countDigits / (float)sentence.length() * 100.0f
                + (float)countBlanks / (float)sentence.length() * 100.0f;
        return (int)Math.ceil(accurany);

    }

    /**
     * this method stemmed a list of terms
     *
     * @param terms {@code List<String> terms}
     * @return new reference of {@code List<Pair<String, String>>} (term : stemmed term)
     */
    public Map<String, String> stemm(final List<String> terms) {

        Map<String, String> results = new HashMap<String, String>();
        String stemmed = "";
        for (String term : terms) {
            stemmed = snowballStemmer.stem(term).toString();
            results.put(term, stemmed);
        }
        return results;

    }

    /**
     * this method tokenize bodyContentHandler in each sentences and remove newline pattern {@code "-\n"} and
     * <p>
     * newline threshold is {@link TextProcessing#maxNewLines}
     * <p>
     *
     * @param maxNewLines <br>
     *            threshold
     * @param bodyContentHandler <br>
     *            textcorpus
     * @return {@code List<String>}
     * @throws IOException
     */

    public List<String> tokenize2Sencences(BodyContentHandler bodyContentHandler, int maxNewLines) throws IOException {

        List<String> results = new ArrayList<String>();
        String[] sentences = sentenceDetector.sentDetect(bodyContentHandler.toString());
        String temp = "";
        for (String sentence : sentences) {
            temp = sentence.replaceAll("-\n", "");
            temp = sentence.replaceAll("\n", "");
            if (countNewLines(temp) <= maxNewLines) {
                results.add(temp);
            }
        }
        LOGGER.info(MessageFormat.format("textcorpus contains {0} sentences.", results.size()));
        return results;
    }

    /**
     * This method tokenize article, these contains several sentences, in each sentences
     *
     * @param {@code List<String> articles}
     * @return new reference{@code List<String>}
     */
    public List<String> tokenize2Sencences(final String article) {
        List<String> results = new ArrayList<String>();
        String[] sentences = sentenceDetector.sentDetect(article);
        for (String sentence : sentences) {
            results.add(sentence);
        }
        LOGGER.info(MessageFormat.format("textcorpus contains {0} sentences.", results.size()));
        return results;
    }

    /**
     * This method tokenize a sentence in each term
     *
     * @param sentence
     * @return
     */
    public List<String> tokenize2Term(final String sentence) {

        List<String> results = new ArrayList<String>();
        for (String term : tokenizer.tokenize(sentence)) {
            results.add(term);
        }
        LOGGER.info(MessageFormat.format("tokenize completed, sentence contains {0} terms", results.size()));
        return results;
    }

    /**
     * This method tokenize a sentence in each term
     *
     * @param sentence
     * @param cleaningPattern
     * @param minTermLenght
     * @return {@code List<String>}
     */
    public List<String> tokenize2Term(final String sentence, String cleaningPattern, int minTermLenght) {

        String temp = "";
        List<String> results = new ArrayList<String>();
        for (String term : tokenizer.tokenize(sentence)) {
            temp = term.trim().replaceAll(cleaningPattern, "");
            temp = try2RemoveHypen(temp);
            if (temp.length() >= minTermLenght) {
                results.add(temp);
            }

        }
        LOGGER.info(MessageFormat.format("tokenize completed, sentence contains {0} terms", results.size()));
        return results;
    }

    /**
     * this method try to remove unimportant information like headings, table of content and listing
     *
     * @param sentence
     * @param cleaningPattern
     * @param minTermLength
     * @param minTermCount
     * @param minTableOfContent
     * @return new refernece of {@code List <String>}
     * @throws IOException
     */
    public List<String> try2CleanSentence(String sentence, String cleaningPattern, int minTermLength, int minTermCount, int minTableOfContent)
            throws IOException {

        List<String> results = new ArrayList<String>();
        List<String> terms = tokenize2Term(sentence, cleaningPattern, minTermLength);

        if (terms.size() <= minTermCount) {
            return results;

        }
        if (probabilityOfSentenceTabelofContent(sentence) >= minTableOfContent) {
            return results;
        }
        for (String term : terms) {
            String temp = try2RemoveHypen(term);
            results.add(temp);
        }
        return results;

    }

    /**
     * this method try to remove hypen(-) in a term like <br>
     * frühr-er --> frührer <br>
     * U-Bahn --> U-Bahn <br>
     * Robert-August-Staße --> Robert-August-Staße
     *
     * @param term
     * @return
     */
    public String try2RemoveHypen(String term) {

        String[] parts = term.split("-");
        if (parts.length != 2) {
            return term;
        }

        char[] charArray = parts[1].toCharArray();
        if (Character.isLowerCase(charArray[0])) {
            return parts[0] + parts[1];
        }
        return term;
    }

    /**
     * this method validate passing properties and set them
     *
     * @param properties
     * @return
     * @throws IllegalArgumentException
     */
    private Boolean validateProperties(Properties properties) throws IllegalArgumentException {

        path2SentenceDetectorFile = properties.getProperty(ApplicationConfiguration.PATH_2_SENTENCE_DETECTOR_FILE);
        if (path2SentenceDetectorFile == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.PATH_2_SENTENCE_DETECTOR_FILE);
        }
        path2PosTaggerFile = properties.getProperty(ApplicationConfiguration.PATH_2_POS_TAGGER_FILE);
        if (path2PosTaggerFile == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.PATH_2_POS_TAGGER_FILE);
        }
        path2SentenceTokenizerFile = properties.getProperty(ApplicationConfiguration.PATH_2_SENTENCE_TOKENIZER_FILE);
        if (path2SentenceTokenizerFile == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.PATH_2_SENTENCE_TOKENIZER_FILE);
        }
        path2StopwordsFile = properties.getProperty(ApplicationConfiguration.PATH_2_STOPWORDS_FILE);
        if (path2StopwordsFile == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.PATH_2_STOPWORDS_FILE);
        }
        path2PosTagsFile = properties.getProperty(ApplicationConfiguration.PATH_2_POSTAGS_FILE);
        if (path2PosTagsFile == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.PATH_2_POSTAGS_FILE);
        }
        // cleaningPattern = properties.getProperty(ApplicationConfiguration.CLEANING_PATTERN);
        // if (cleaningPattern == null) {
        // throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.CLEANING_PATTERN);
        // }
        // String property = properties.getProperty(ApplicationConfiguration.MIN_TERM_LENGTH);
        // if (property == null) {
        // throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.MIN_TERM_LENGTH);
        // }
        // minTermLength = Integer.parseInt(property);
        // property = properties.getProperty(ApplicationConfiguration.MIN_TERM_COUNT);
        // if (property == null) {
        // throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.MIN_TERM_COUNT);
        // }
        // minTermCount = Integer.parseInt(property);
        // property = properties.getProperty(ApplicationConfiguration.MIN_TABLE_OF_CONTENT);
        // if (property == null) {
        // throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.MIN_TABLE_OF_CONTENT);
        // }
        // minTableOfContent = Integer.parseInt(property);

        return true;
    }

}
