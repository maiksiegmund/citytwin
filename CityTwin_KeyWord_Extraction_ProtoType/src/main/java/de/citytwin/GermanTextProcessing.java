/* @filename $RCSfile: GermanTextProcessing.java,v $ */

package de.citytwin;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
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
 * @author Maik, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class GermanTextProcessing {

    private static boolean isInitialzied = false;

    /** Klassenspezifischer, aktueller Logger (Server: org.apache.log4j.Logger; Client: java.util.logging.Logger) */
    private static transient final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static POSTaggerME posTagger = null;
    private static SentenceDetectorME sentenceDetector = null;
    private static Set<String> stopwords = new HashSet<String>();
    private static Tokenizer tokenizer = null;
    private static SnowballStemmer snowballStemmer = null;
    /** Aktuelle Versionsinformation */
    private static final String VERSION = "$Revision: 1.00 $";

    /**
     * this method return german pos tag List
     *
     * @see <a href=https://www.cis.lmu.de/~schmid/tools/TreeTagger/data/stts_guide.pdf>german pos tags</a>
     * @return new reference of {@code List<String>}
     */
    public static List<String> getPosTagList() {

        List<String> tagFilters = new ArrayList<String>();

        // german
        // tagFilters.add("ADJA");
        // tagFilters.add("ADJD");
        // tagFilters.add("ADV");
        // tagFilters.add("APPR");
        // tagFilters.add("APPRART");
        // tagFilters.add("APPO");
        // tagFilters.add("APZR");
        // tagFilters.add("ART");
        // tagFilters.add("CARD");
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
        // tagFilters.add("VVFIN");
        // tagFilters.add("VVIMP");
        // tagFilters.add("VVINF");
        // tagFilters.add("VVIZU");
        // tagFilters.add("VVPP");
        // tagFilters.add("VAFIN");
        // tagFilters.add("VAIMP");
        // tagFilters.add("VAINF");
        // tagFilters.add("VAPP");
        // tagFilters.add("VMFIN");
        // tagFilters.add("VMINF");
        // tagFilters.add("VMPP");
        // tagFilters.add("XY");

        return tagFilters;

    }

    public static Set<String> getStopwords() {
        return stopwords;
    }

    public static void setStopwords(Set<String> stopwords) {
        GermanTextProcessing.stopwords = stopwords;
    }

    private String cleaningPattern = "[^\\u2013\\u002D\\wäÄöÖüÜß,-/]";
    private int maxNewLines = 10;
    // threshold term lenght
    private int minTermLenght = 2;
    // threshold of term count in a sentences
    private int minTermCount = 5;

    private String tokenizerName = "Letter";

    public GermanTextProcessing() throws IOException {

        if (!isInitialzied) {
            initialize();
        }
    }

    /**
     * This count a specific char in a sentence,
     * <p>
     *
     * @param sentence
     * @return {@code int}
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
     * this mehtod count digits in a sentence
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
     * This count new lines in a sentence,
     * <p>
     *
     * @param sentence
     * @return {@code int}
     */
    public int countNewLines(String sentence) {
        return countChar(sentence, '\n');
    }

    /**
     * this method keeps terms they tagged with necessary pos tag and included in pos-list
     *
     * @param termsWithPosTags
     * @param posTags {@link GermanTextProcessing#getPOSTags(List)}
     * @return new reference of {@code List<String>}
     */
    public List<String> filterByPosTags(List<Pair<String, String>> termsWithPosTags) {
        List<String> results = new ArrayList<String>();

        for (Pair<String, String> pair : termsWithPosTags) {
            for (String posFilter : GermanTextProcessing.getPosTagList()) {
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
     * {@link GermanTextProcessing#stopwords}
     *
     * @param terms {@code List<String>}
     * @return new reference of {@code List<String>} filtered
     */
    public List<String> filterByStopWords(List<String> terms) {

        List<String> results = new ArrayList<String>();
        for (String term : terms) {
            if (!stopwords.contains(term.toLowerCase())) {
                results.add(term);
            }
        }
        logger.info("filter by stopwords completed. (terms removal)");
        return results;

    }

    public String getCleaningPattern() {
        return cleaningPattern;
    }

    public int getMaxNewLines() {
        return maxNewLines;
    }

    public int getMinCharactersLenght() {
        return minTermLenght;
    }

    /**
     * this method return pos tags of a german sentence. <br>
     * <strong> not stemmed or filtered by stopwords </strong>
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

    /**
     * this method return SentenceDetector
     *
     * @return static reference of {@code SentenceDetectorME}
     */
    public SentenceDetectorME getSentenceDetectorME() {
        return sentenceDetector;
    }

    public String getTokenizerName() {
        return tokenizerName;
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

        inputStream = getClass().getClassLoader().getResourceAsStream("de-token.bin");
        TokenizerModel tokenizerModel = new TokenizerModel(inputStream);
        tokenizer = new TokenizerME(tokenizerModel);
        inputStream.close();

        inputStream = getClass().getClassLoader().getResourceAsStream("stopswords_de.txt");
        Scanner scanner = new Scanner(inputStream).useDelimiter("\\r\\n");

        while (scanner.hasNext()) {
            stopwords.add(scanner.next());
        }
        inputStream.close();
        scanner.close();

        snowballStemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.GERMAN);

        isInitialzied = true;

    }

    /**
     * this method calculate the probability (calculate by dots, digits and blanks in sentence)
     *
     * @param sentence
     * @return {@code int} in percent
     */
    public int probabilityIsSentenceTabelofContent(String sentence) {

        int countDots = this.countChar(sentence, '.');
        int countDigits = this.countDigits(sentence);
        int countBlanks = this.countChar(sentence, ' ');
        float accurany = (float)countDots / (float)sentence.length() * 100.0f + (float)countDigits / (float)sentence.length() * 100.0f
                + (float)countBlanks / (float)sentence.length() * 100.0f;
        return (int)Math.ceil(accurany);

    }

    public void setCleaningPattern(String cleaningPattern) {
        this.cleaningPattern = cleaningPattern;
    }

    public void setMaxNewLines(int maxNewLines) {
        this.maxNewLines = maxNewLines;
    }

    public void setMinCharactersLenght(int minCharactersLenght) {
        this.minTermLenght = minCharactersLenght;
    }

    public void setTokenizerName(String tokenizerName) {
        this.tokenizerName = tokenizerName;
    }

    /**
     * this method stemmed a list of terms
     *
     * @param terms {@code List<String> terms}
     * @return new reference of {@code List<Pair<String, String>>} (term : stemmed term)
     */
    public List<Pair<String, String>> stemm(final List<String> terms) {

        List<Pair<String, String>> results = new ArrayList<Pair<String, String>>(terms.size());
        String stemmed = "";
        for (String term : terms) {
            stemmed = snowballStemmer.stem(term).toString();
            Pair<String, String> pair = Pair.of(term, stemmed);
            results.add(pair);
        }
        return results;

    }

    /**
     * This method split articles in each sentences by opennlp an article can contain more as one sentence
     *
     * @param {@code List<String> articles}
     * @return new reference{@code List<String>}
     */
    public List<String> tokenizeArticlesToSencences(final List<String> articles) {
        List<String> results = new ArrayList<String>();
        for (String atricle : articles) {
            String[] sentences = sentenceDetector.sentDetect(atricle);
            for (String sentence : sentences) {
                results.add(sentence);
            }
        }
        logger.info(MessageFormat.format("textcorpus contains {0} sentences.", results.size()));
        return results;
    }

    /**
     * This method split textcorpus in each senteces by opennlp
     * <p>
     * remove newline pattern {@code "-\n"} and
     * <p>
     * newline threshold is {@link GermanTextProcessing#maxNewLines}
     * <p>
     *
     * @param bodyContentHandler
     * @return {@code List<String>}
     * @throws IOException
     */
    public List<String> tokenizeBodyContentToSencences(BodyContentHandler bodyContentHandler) throws IOException {

        List<String> results = new ArrayList<String>();
        String[] sentences = sentenceDetector.sentDetect(bodyContentHandler.toString());
        String temp = "";
        for (String sentence : sentences) {
            temp = sentence.replaceAll("-\n", "");
            temp = sentence.replaceAll("\n", "");
            if (countNewLines(sentence) < maxNewLines) {
                results.add(temp);
            }
        }
        logger.info(MessageFormat.format("textcorpus contains {0} sentences.", results.size()));
        return results;
    }

    /**
     * This method splits an sentence in each term by apache lucene
     *
     * @param sentence
     * @return new reference of {@code List<String>}
     * @throws IOException
     */
    public List<String> tokenizeLucene(final String sentence) throws IOException {

        List<String> results = new ArrayList<String>();
        Analyzer analyzer = CustomAnalyzer.builder()
                .withTokenizer(tokenizerName)
                // .addTokenFilter("LowerCase")
                // .addTokenFilter("hyphenatedwords")
                .build();

        String temp = "";

        TokenStream stream = analyzer.tokenStream(null, new StringReader(sentence));
        CharTermAttribute attr = stream.addAttribute(CharTermAttribute.class);
        stream.reset();
        while (stream.incrementToken()) {
            temp = attr.toString();
            temp = tryToRemoveHypen(temp);
            if (temp.length() >= minTermLenght) {
                results.add(temp);
            }
        }
        analyzer.close();
        stream.close();
        logger.info(MessageFormat.format("tokenize completed, sentence contains {0} terms.", results.size()));
        return results;
    }

    /**
     * This method split a sentence in each term by apache opennlp works better with terms like "Bebauungsplan-Entwurf"
     *
     * @param sentence
     * @return {@code List<String>}
     */
    public List<String> tokenizeOpenNLP(final String sentence) {

        String temp = "";
        List<String> results = new ArrayList<String>();
        for (String term : tokenizer.tokenize(sentence)) {
            temp = term.trim().replaceAll(cleaningPattern, "");
            temp = tryToRemoveHypen(temp);
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
     * @param isOpenNLP
     * @param tableOfContentThreshold (Nullable, default 50)
     * @return new refernece of {@code List <String>}
     * @throws IOException
     */

    public List<String> tryToCleanSentence(String sentence, boolean isOpenNLP, @Nullable Integer tableOfContentThreshold) throws IOException {

        int threshold = (tableOfContentThreshold == null) ? 50 : tableOfContentThreshold;
        List<String> results = new ArrayList<String>();
        List<String> terms = (isOpenNLP) ? tokenizeOpenNLP(sentence) : tokenizeLucene(sentence);

        if (terms.size() <= this.minTermCount) {
            return results;

        }
        if (probabilityIsSentenceTabelofContent(sentence) > threshold) {
            return results;
        }
        for (String term : terms) {
            String temp = tryToRemoveHypen(term);
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
    public String tryToRemoveHypen(String term) {

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

}
