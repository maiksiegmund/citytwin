/* @filename $RCSfile: GermanTextProcessing.java,v $ */

package de.citytwin.text;

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
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class TextProcessing implements AutoCloseable {

    private static transient final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * this method create default properties
     *
     * @return {@code Properties}
     */
    public static Properties getDefaultProperties() {
        Properties properties = new Properties();
        properties.put("path.2.sentence.detector.file", "..\\de-sent.bin");
        properties.put("path.2.pos-tagger.file", "..\\de-posperceptron.bin");
        properties.put("path.2.sentence.tokenizer.file", "..\\de-token.bin");
        properties.put("path.2.stopword.file", "..\\de-stopswords.txt");
        properties.put("path.2.postag.file", "..\\de-postags.txt");
        properties.put("cleaningPattern", "[^\\u2013\\u002D\\wäÄöÖüÜß,-/]");
        properties.put("minTermLenght", 2);
        properties.put("minTermCount", 5);
        properties.put("minTableOfContentThershold", 50);

        return properties;
    }

    private boolean isInitialzied = false;
    private final String VERSION = "$Revision: 1.00 $";
    private POSTaggerME posTagger = null;
    private SentenceDetectorME sentenceDetector = null;
    private Set<String> stopwords = new HashSet<String>();
    private Set<String> posTags = new HashSet<String>();
    private Tokenizer tokenizer = null;
    private SnowballStemmer snowballStemmer = null;

    private Properties properties = null;

    /**
     * parameter Constructor
     *
     * @param properties = {@code TextProcessing.getDefaultProperties()}
     * @throws IOException
     */
    public TextProcessing(Properties properties) throws IOException {

        if (validateProperties(properties)) {
            this.properties = new Properties(properties.size());
            this.properties.putAll(properties);
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
        this.properties.clear();
        this.properties = null;
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
        logger.info("filter by pos tags completed. (terms keeping)");
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
        logger.info("filter by stopwords completed. (terms removal)");
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
        logger.info("pos tagging completed.");
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

        String property = "";
        property = properties.getProperty("path.2.sentence.detector.file");
        try(InputStream inputStream = new FileInputStream(property);) {
            SentenceModel sentenceModel = new SentenceModel(inputStream);
            sentenceDetector = new SentenceDetectorME(sentenceModel);
        }
        property = properties.getProperty("path.2.pos-tagger.file");
        try(InputStream inputStream = new FileInputStream(property);) {
            posTagger = new POSTaggerME(new POSModel(inputStream));
        }
        property = properties.getProperty("path.2.sentence.tokenizer.file");
        try(InputStream inputStream = new FileInputStream(property);) {
            TokenizerModel tokenizerModel = new TokenizerModel(inputStream);
            tokenizer = new TokenizerME(tokenizerModel);
        }
        property = properties.getProperty("path.2.stopword.file");
        try(InputStream inputStream = new FileInputStream(property);
                Scanner scanner = new Scanner(inputStream).useDelimiter("\\r\\n");) {
            while (scanner.hasNext()) {
                String stopword = scanner.next();
                if (!stopword.trim().startsWith("#")) {
                    stopwords.add(stopword);
                }
            }
        }
        property = properties.getProperty("path.2.postag.file");
        try(InputStream inputStream = new FileInputStream(property);
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
    public int probabilityIsSentenceTabelofContent(final String sentence) {

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
        logger.info(MessageFormat.format("textcorpus contains {0} sentences.", results.size()));
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
        logger.info(MessageFormat.format("textcorpus contains {0} sentences.", results.size()));
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
        logger.info(MessageFormat.format("tokenize completed, sentence contains {0} terms", results.size()));
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
        logger.info(MessageFormat.format("tokenize completed, sentence contains {0} terms", results.size()));
        return results;
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
     * this method try to remove unimportant information like headings, table of content and listing
     *
     * @param sentence
     * @param cleaningPattern
     * @param minTermLenght
     * @param minTermCount
     * @param minTableOfContentThershold
     * @return new refernece of {@code List <String>}
     * @throws IOException
     */
    public List<String> try2CleanSentence(String sentence, String cleaningPattern, int minTermLenght, int minTermCount, int minTableOfContentThershold)
            throws IOException {

        List<String> results = new ArrayList<String>();
        List<String> terms = tokenize2Term(sentence, cleaningPattern, minTermLenght);

        if (terms.size() <= minTermCount) {
            return results;

        }
        if (probabilityIsSentenceTabelofContent(sentence) >= minTableOfContentThershold) {
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

    private Boolean validateProperties(Properties properties) throws IOException {
        String property = (String)properties.get("path.2.sentence.detector.file");
        if (property == null) {
            throw new IOException("set property --> path.2.sentence.detector.file as String");
        }
        property = (String)properties.get("path.2.pos-tagger.file");
        if (property == null) {
            throw new IOException("set property --> path.2.pos-tagger.file as String");
        }
        property = (String)properties.get("path.2.sentence.tokenizer.file");
        if (property == null) {
            throw new IOException("set property --> path.2.sentence.tokenizer.file as String");
        }
        property = (String)properties.get("path.2.stopwords.file");
        if (stopwords == null) {
            throw new IOException("set property --> path.2.stopword.file as String");
        }
        property = (String)properties.get("path.2.postag.file");
        if (stopwords == null) {
            throw new IOException("set property --> path.2.postag.file as String");
        }
        property = (String)properties.get("cleaningPattern");
        if (stopwords == null) {
            throw new IOException("set property --> cleaningPattern as String");
        }
        Integer value = (Integer)properties.get("minTermLenght");
        if (value == null) {
            throw new IOException("set property --> minTermLenght as Integer");
        }
        value = (Integer)properties.get("minTermCount");
        if (value == null) {
            throw new IOException("set property --> minTermCount as Integer");
        }
        value = (Integer)properties.get("minTableOfContentThershold");
        if (value == null) {
            throw new IOException("set property --> minTableOfContentThershold as Integer");
        }

        return true;
    }

}
