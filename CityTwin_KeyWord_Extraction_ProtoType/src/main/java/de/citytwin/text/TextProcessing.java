/* @filename $RCSfile: GermanTextProcessing.java,v $ */

package de.citytwin.text;

import de.citytwin.config.ApplicationConfiguration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

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
/**
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
        properties.setProperty(ApplicationConfiguration.PATH_2_KEEPWORDS_FILE, "..\\de-keepwords.txt");
        properties.setProperty(ApplicationConfiguration.PATH_2_POSTAGS_FILE, "..\\de-postags.txt");
        properties.setProperty(ApplicationConfiguration.CLEANING_REGEX, "[^\\u2013\\u002D\\wäÄöÖüÜß,-/]");
        properties.setProperty(ApplicationConfiguration.MIN_TERM_LENGTH, "2");
        properties.setProperty(ApplicationConfiguration.MIN_TERM_COUNT, "5");
        properties.setProperty(ApplicationConfiguration.MIN_TABLE_OF_CONTENT, "80");
        return properties;
    }

    private boolean isInitialzied = false;
    private POSTaggerME posTagger = null;
    private SentenceDetectorME sentenceDetector = null;
    private Set<String> stopwords = new HashSet<String>();
    private Set<String> keepwords = new HashSet<String>();
    private Set<String> posTags = new HashSet<String>();
    private Tokenizer tokenizer = null;
    private SnowballStemmer snowballStemmer = null;
    private String path2SentenceDetectorFile = null;
    private String path2PosTaggerFile = null;
    private String path2SentenceTokenizerFile = null;
    private String path2StopwordsFile = null;
    private String path2keepwordsFile = null;
    private String path2PosTagsFile = null;
    private Integer minTermCount = null;
    private Integer minTermLength = null;
    private Integer minTableOfContent = null;

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
        this.keepwords.clear();
        this.keepwords = null;
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

        initializeWordLists(path2StopwordsFile, stopwords);
        initializeWordLists(path2PosTagsFile, posTags);
        initializeWordLists(path2keepwordsFile, keepwords);

        snowballStemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.GERMAN);
        isInitialzied = true;
    }

    /**
     * this method reads a wordlist
     *
     * @param path
     * @param container
     * @throws IOException
     */
    private void initializeWordLists(String path, Set<String> container) throws IOException {
        try(InputStream inputStream = new FileInputStream(path);
                Scanner scanner = new Scanner(inputStream).useDelimiter("\\r\\n");) {
            while (scanner.hasNext()) {
                String word = scanner.next();
                if (!word.trim().startsWith("#")) {
                    container.add(word.trim());
                }
            }
        }
    }

    /**
     * this method calculate the probability whether sentence is table of content <br>
     * (calculate by dots, digits and blanks in sentence)
     *
     * @param sentence
     * @return {@code int} in percent
     */
    public int probabilityOfSentenceTabelofContent(final String sentence) {

        float countDots = this.countChar(sentence, '.');
        float countDigits = this.countDigits(sentence);
        float countBlanks = this.countChar(sentence, ' ');
        float lenghtWithoutTerms = getTermCount(sentence) + countDots + countDigits + countBlanks;
        float accurany = (countDots + countDigits + countBlanks) / lenghtWithoutTerms * 100.0f;
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
            temp = temp.replaceAll("\n", " ");
            temp = temp.replaceAll("- ", "");
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
        path2keepwordsFile = properties.getProperty(ApplicationConfiguration.PATH_2_KEEPWORDS_FILE);
        if (path2keepwordsFile == null) {
            throw new IllegalArgumentException("set property --> " + "ApplicationConfiguration.PATH_2_STOPWORDS_FILE");
        }
        path2PosTagsFile = properties.getProperty(ApplicationConfiguration.PATH_2_POSTAGS_FILE);
        if (path2PosTagsFile == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.PATH_2_POSTAGS_FILE);
        }
        // cleaningPattern = properties.getProperty(ApplicationConfiguration.CLEANING_PATTERN);
        // if (cleaningPattern == null) {
        // throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.CLEANING_PATTERN);
        // }
        String property = properties.getProperty(ApplicationConfiguration.MIN_TERM_LENGTH);
        if (property == null) {
            throw new IllegalArgumentException("set property --> " + "ApplicationConfiguration.MIN_TERM_LENGTH");
        }
        minTermLength = Integer.parseInt(property);
        property = properties.getProperty(ApplicationConfiguration.MIN_TERM_COUNT);
        if (property == null) {
            throw new IllegalArgumentException("set property --> " + "ApplicationConfiguration.MIN_TERM_COUNT");
        }
        minTermCount = Integer.parseInt(property);
        property = properties.getProperty(ApplicationConfiguration.MIN_TABLE_OF_CONTENT);
        if (property == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.MIN_TABLE_OF_CONTENT);
        }
        minTableOfContent = Integer.parseInt(property);

        return true;
    }

    /**
     * this method provide some logic to get a cleaned text corpus
     *
     * @param textCorpus
     * @return
     * @throws IOException
     */
    public List<String> getPreProcessedTextCorpus(String textCorpus) throws IOException {

        String temp = try2RemoveUnimportantLinesOnTextCorpus(textCorpus);
        temp = try2RemoveTableOfContentOnTextCorpus(temp);
        temp = try2RemoveDuplicateLinesOnTextCorpus(temp);
        temp = try2RemoveHyphenOnTextCorpus(temp);
        String[] sentences = sentenceDetector.sentDetect(temp.replaceAll("\r\n", " "));
        return Arrays.asList(sentences);

    }

    /**
     * this method provides some logic to try remove lines contains only digtis, newlines, words contains in keepword-list remain
     *
     * @param textCorpus
     * @return
     */
    private String try2RemoveUnimportantLinesOnTextCorpus(String textCorpus) {

        String[] textParts = textCorpus.split("\n");
        if (textParts.length <= 2) {
            return textCorpus;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (String textPart : textParts) {
            if (textPart.trim().length() == 0 || containsOnlyDigits(textPart)) {
                continue;
            }
            if (getTermCount(textPart.trim()) < this.minTermCount ^ containsInKeepwords(textPart)) {
                continue;
            }
            stringBuilder.append(textPart.trim());
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }

    /**
     * this method check whether sentence contains a keep word
     *
     * @param sentence
     * @return
     */
    private boolean containsInKeepwords(String sentence) {
        return (keepwords.stream().filter(keepword -> sentence.toLowerCase().contains(keepword.toLowerCase())).count() >= 1);
    }

    /**
     * this method provide some logic to try to remove table of content lines and header
     *
     * @param textCorpus
     * @return
     */
    private String try2RemoveTableOfContentOnTextCorpus(String textCorpus) {

        String[] textParts = textCorpus.split("\n");
        if (textParts.length <= 2) {
            return textCorpus;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (String textPart : textParts) {
            if (this.probabilityOfSentenceTabelofContent(textPart) > minTableOfContent) {
                continue;
            }
            stringBuilder.append(textPart);
            stringBuilder.append("\n");

        }
        return stringBuilder.toString();
    }

    /**
     * this method provides some logic to try remove duplicate lines (header, footer)
     *
     * @param textCorpus
     * @return
     */
    private String try2RemoveDuplicateLinesOnTextCorpus(String textCorpus) {

        String[] textParts = textCorpus.split("\n");
        if (textParts.length <= 2) {
            return textCorpus;
        }
        StringBuilder stringBuilder = new StringBuilder();

        List<String> tempTextCorpus = Arrays.stream(textParts).collect(Collectors.toList());
        int counts[] = new int[tempTextCorpus.size()];
        // hashmap not suitable, to avoid hashcode collisions
        // count sentence in text corpus
        // index are the same
        for (int index = 0; index < textParts.length; ++index) {
            String temp = textParts[index];
            counts[index] = (int)tempTextCorpus.stream().filter(sent -> sent.equals(temp)).count();
        }

        for (int index = 0; index < textParts.length; ++index) {
            if (counts[index] > 1 && !containsInKeepwords(textParts[index])) {
                continue;
            }

            stringBuilder.append(textParts[index]);
            stringBuilder.append("\n");
        }

        return stringBuilder.toString();
    }

    private String try2RemoveSimilarityLinesOnTextCorpus(String textCorpus, int firstLines) {

        StringBuilder stringBuilder = new StringBuilder();
        String[] textParts = textCorpus.split("\n");
        int similiatry[] = new int[textParts.length];
        for (int indexLeft = 0; indexLeft < firstLines; ++indexLeft) {
            for (int indexRight = 0; indexRight < textParts.length; ++indexRight) {
                String left = textParts[indexLeft];
                String right = textParts[indexRight];
                int value = (indexLeft != indexRight) ? getSimilarityByContainsWords(left, right) : 0;
                if (value > similiatry[indexLeft]) {
                    similiatry[indexLeft] = value;

                }
                stringBuilder.append(MessageFormat.format("{0} --> {1} = {2}", value, left, right));
                stringBuilder.append("\n");
            }
        }

        return stringBuilder.toString();

    }

    /**
     * this method provides some logic to calculate similarity of two sentences by count the contain word in each other very simple! <br>
     * example for matches<br>
     * {@code "Juli 2004, aktualisiert Mai 200520"; } <br>
     * {@code "Juni 2004, aktualisiert Mai 200514"; } <br>
     * {@code "49 Juli 2004, aktualisiert Mai 2005"; } <br>
     * this
     *
     * @param left
     * @param right
     * @return
     */
    private int getSimilarityByContainsWords(String left, String right) {

        String large = left;
        String small = right;

        if (left.length() < right.length()) {
            large = right;
            small = left;
        }
        String partsLarge[] = large.toLowerCase().split(" ");
        String partsSmall[] = small.toLowerCase().split(" ");
        int countContainParts = 0;
        for (int index = 0; index < partsSmall.length; ++index) {
            if (large.toLowerCase().contains(partsSmall[index])) {
                countContainParts++;
            }
        }
        if (countContainParts == 0)
            return 0;
        return (int)Math.ceil((float)countContainParts / (float)partsLarge.length * 100.0f);
    }

    /**
     * this method provides some logic to remove hyphens of text corpus and concatenate these sentences
     *
     * @param textCorpus
     * @return
     */
    private String try2RemoveHyphenOnTextCorpus(String textCorpus) {
        String[] textParts = textCorpus.split("\n");
        if (textParts.length <= 2) {
            return textCorpus;
        }
        StringBuilder stringBuilder = new StringBuilder();

        List<String> partOfTextCorpus = new ArrayList<String>();

        for (int index = 0; index < textParts.length; ++index) {
            if (endsWithHyphen(textParts[index])) {
                partOfTextCorpus.add(textParts[index]);

            } else {
                String temp = "";
                for (String partOfTextCorpu : partOfTextCorpus) {
                    temp += partOfTextCorpu.trim().substring(0, partOfTextCorpu.trim().length() - 1);
                }
                stringBuilder.append(temp + textParts[index]);
                stringBuilder.append("\n");
                partOfTextCorpus.clear();
            }

        }

        return stringBuilder.toString();
    }

    /**
     * this method count digits in a String ("123" = 3 not equal "123" = 1 )
     *
     * @param sentence
     * @return
     */
    private boolean containsOnlyDigits(String sentence) {
        char[] charcaters = sentence.trim().toCharArray();
        for (char c : charcaters) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * this method checks whether a String ends with a hyphen ...
     *
     * @param sentence
     * @return
     */
    private boolean endsWithHyphen(String sentence) {

        String trimed = sentence.trim();
        if (trimed.length() == 0)
            return false;
        String lastCharacter = trimed.substring(trimed.length() - 1, trimed.length());
        return lastCharacter.matches("[\\u2013\\u002D­]");
    }

    /**
     * this method return a term (simple whitespace tokenizer)
     *
     * @param sentence
     * @return
     */
    private int getTermCount(String sentence) {
        String temp = sentence.trim();
        int count = 0;
        String[] parts = temp.split("[\\s]");
        for (String part : parts) {
            if (part.matches("[a-zA-ZwäÄüÜöÖß()/-\\u2013\\u002D]*") && part.trim().length() > this.minTermLength)
                count++;
        }
        return count;

    }

    /**
     * this method concatenate a List<String> and remove blanks between commas
     *
     * @param sentence
     * @return
     */
    public String concat(List<String> sentence) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String term : sentence) {
            // remove blank between commas
            if (term.toCharArray().length == 1 && stringBuilder.length() > 1) {
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            }
            stringBuilder.append(term);
            stringBuilder.append(" ");

        }
        return stringBuilder.toString().trim();
    }

}
