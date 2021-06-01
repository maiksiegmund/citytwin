package de.citytwin;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
 *
 */
public class TextAnalyser {

	/**
	 * This inner class represent DocumentCount only use here. used as struct ...
	 * <br>
	 * Quartet {@code Map<String, Quartet<Integer, Double, String, Set<Integer>>>}
	 * <p>
	 * {@link DocumentCount#terms} <br>
	 * {@link DocumentCount#sentences} subdocuments is d_i, D is single Document e.g
	 * a word, pdf, txt file <br>
	 * {@link DocumentCount#countWords} is |D| <br>
	 * {@link DocumentCount#isNormalized}
	 */
	class DocumentCount {

		// term, count, calculation, , postag, sentenceindex
		public Map<String, Quartet<Integer, Double, String, Set<Integer>>> terms;

		public List<String> sentences;
		public int countWords;
		public boolean isNormalized;

		public DocumentCount() {
			terms = new HashMap<String, Quartet<Integer, Double, String, Set<Integer>>>();
			sentences = new ArrayList<String>();
			countWords = 0;
			isNormalized = false;
		}

		public DocumentCount(DocumentCount documentCount) {
			terms = new HashMap<String, Quartet<Integer, Double, String, Set<Integer>>>(documentCount.terms);
			sentences = new ArrayList<String>(documentCount.sentences);
			countWords = documentCount.countWords;
			isNormalized = documentCount.isNormalized;

		}
	}

	public static enum NormalizationType {
		NONE, DOUBLE, LOG
	}

	private static SentenceDetectorME sentenceDetector;
	private static Tokenizer tokenizer;
	private static POSTaggerME posTagger;
	private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private boolean isOpenNLP = false;
	private boolean withStemming = false;
	private String tokenizerName = "whitespace";

	private int minLenght = 0;

	private String replacePattern = "[^a-zA-ZäÄüÜöÖß-]";

	/**
	 * constructor loads the pre trained models <br>
	 * de-sent.bin, de-token.bin and de-pos-maxent.bin
	 * <p>
	 * 
	 * @param isOpenNL     {@code true} or {@code false}
	 * @param withStemming {@code true} or {@code false}
	 * @throws IOException
	 */
	public TextAnalyser(boolean isOpenNL, boolean withStemming) throws IOException {

		this.isOpenNLP = isOpenNL;
		this.withStemming = withStemming;

		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("de-sent.bin");
		SentenceModel sentenceModel = new SentenceModel(inputStream);
		sentenceDetector = new SentenceDetectorME(sentenceModel);
		inputStream.close();

		inputStream = getClass().getClassLoader().getResourceAsStream("de-pos-maxent.bin");
		posTagger = new POSTaggerME(new POSModel(inputStream));
		inputStream.close();

		if (this.isOpenNLP) {
			inputStream = getClass().getClassLoader().getResourceAsStream("de-token.bin");
			TokenizerModel tokenizerModel = new TokenizerModel(inputStream);
			tokenizer = new TokenizerME(tokenizerModel);
		}

	}

	/**
	 * This method calculate smoot inverse document frequency.
	 * 
	 * @param documentCount
	 * @return new reference of {@link DocumentCount}
	 * @throws IOException
	 */
	private DocumentCount calculateSmootInverseDocumentFrequency(final DocumentCount documentCount) throws IOException {

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
			value = Math.log10(quartet.getValue1() / (1.0 + quartet.getValue3().size())) + 1.0;
			quartet = quartet.setAt1(value);
			result.terms.put(term, quartet);

		}
		logger.info("calculate smoot inverse document frequency completed.");
		return result;
	}

	/**
	 * This method calculate a term freuency. tf(term) = rawcount / countWords <br>
	 * 
	 * @see <a href=https://en.wikipedia.org/wiki/Tf%E2%80%93idf> tf idf calculation
	 *      on wikipedia</a>
	 *
	 * @param DocumentCount
	 * @return new reference of {@link DocumentCount}
	 */
	private DocumentCount calculateTermFrequency(final DocumentCount documentCount) {

		DocumentCount result = new DocumentCount();
		result.sentences = documentCount.sentences;
		result.countWords = documentCount.countWords;

		Quartet<Integer, Double, String, Set<Integer>> quartet = null;

		for (String term : documentCount.terms.keySet()) {
			quartet = documentCount.terms.get(term);
			quartet = quartet.setAt1((double) quartet.getValue0() / (double) documentCount.countWords);
			result.terms.put(term, quartet);
		}
		result.isNormalized = false;
		logger.info("calculate term frequency finished.");
		return result;
	}

	/**
	 * This method calculate term frequency and inverse document frequency by lucene
	 * or opennlp.
	 * 
	 * @param bodyContentHandler {@link BodyContentHandler}
	 * @param tagFilters,        {@link List<String>}
	 * @param type               {@link TextAnalyser#NormalizationType}
	 * @return new reference of {@link DocumentCount}
	 * @throws IOException
	 */
	public Map<String, Quartet<Integer, Double, String, Set<Integer>>> calculateTfIDF(
			final BodyContentHandler bodyContentHandler, final List<String> tagFilters,
			TextAnalyser.NormalizationType type) throws IOException {

		DocumentCount result = new DocumentCount();

		try {
			DocumentCount rawCount = new DocumentCount();

			rawCount = getRawCount(bodyContentHandler);
			rawCount = (withStemming) ? stem(rawCount) : rawCount;
			DocumentCount tf = calculateTermFrequency(rawCount);
			switch (type) {
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
			DocumentCount idf = calculateSmootInverseDocumentFrequency(tf);
			result = calculateTfIDF(tf, idf);
			if (tagFilters.size() == 0) {
				return sortbyValue(result.terms, false);
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
			return sortbyValue(filterd, false);

		} catch (IOException exception) {
			logger.error(exception.getMessage());
			throw exception;
		}
	}

	/**
	 * This method calculate tfidf. Equation = tfidf(t,d,D) = tf(t,d) * idf(t,D)
	 * 
	 * @see <a href=https://en.wikipedia.org/wiki/Tf%E2%80%93idf> tf idf calculation
	 *      on wikipedia</a>
	 * 
	 * @param tf  {@link DocumentCount}
	 * @param idf {@link DocumentCount}
	 * @return new reference of {@link DocumentCount}
	 */
	private DocumentCount calculateTfIDF(DocumentCount tf, DocumentCount idf) {
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
	 * This method count a term in a sentence. splits by splited by <br>
	 * {@link TextAnalyser#splitSentenceOpenNLP(String)} or <br>
	 * {@link extAnalyser#splitSentenceLucene}
	 * 
	 * @param term     {@code String}
	 * @param sentence {@code String}
	 * @return {@code int}
	 * @throws IOException
	 */
	private int countTermInSentences(final String term, final String sentence) throws IOException {
		int result = 0;
		SnowballStemmer snowballStemmerOpenNLP = new SnowballStemmer(SnowballStemmer.ALGORITHM.GERMAN);
		String temp = "";
		List<String> words = (isOpenNLP) ? splitSentenceOpenNLP(sentence) : splitSentenceLucene(sentence);
		for (String word : words) {
			temp = (withStemming) ? snowballStemmerOpenNLP.stem(word).toString() : word;
			result = (temp.equals(term)) ? result + 1 : result;
		}
		logger.info(MessageFormat.format("{0} founded {1} times in {2}.", term, result, sentence));
		return result;
	}

	public void doDocumentToVec(String text) {

	}

	public void doRake(String text) {

	}

	/**
	 * This method normalized a term freuency. <br>
	 * equation = k +( 1 - k) f(t,d) / (max(f(t,d)))
	 * 
	 * @see <a href=https://en.wikipedia.org/wiki/Tf%E2%80%93idf> tf idf calculation
	 *      on wikipedia</a>
	 *
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
			value = k + (1.0 - k) * ((double) quartet.getValue0() / (double) max.getValue0());
			quartet = quartet.setAt1(value);
			result.terms.put(term, quartet);
		}
		result.isNormalized = true;
		logger.info("double normalization completed.");
		return result;
	}

	public void doWordToVec(String text) {

	}

	/**
	 * This method split a document in sentences and in words and count theme.
	 *
	 * @param bodyContentHandler {@link BodyContentHandler}
	 * @return new reference of {@link DocumentCount}
	 */
	private DocumentCount getRawCount(BodyContentHandler bodyContentHandler) throws IOException {

		int sentenceIndex = 0;
		DocumentCount result = new DocumentCount();
		result.sentences = getSentences(bodyContentHandler);

		for (String sentence : result.sentences) {
			List<String> words = (isOpenNLP) ? splitSentenceOpenNLP(sentence) : splitSentenceLucene(sentence);
			String[] stringArray = new String[words.size()];
			stringArray = words.toArray(stringArray);
			String[] tags = posTagger.tag(stringArray);
			int tagIndex = 0;

			for (String word : words) {
				result.countWords++;

				Set<Integer> sentenceIndies = new HashSet<>();
				Quartet<Integer, Double, String, Set<Integer>> quartet = Quartet.with(1, 0.0, tags[tagIndex],
						sentenceIndies);

				if (!result.terms.containsKey(word)) {
					result.terms.put(word, quartet);
					quartet.getValue3().add(sentenceIndex);
					continue;
				}
				quartet = result.terms.get(word);
				quartet = quartet.setAt0(quartet.getValue0() + 1);
				quartet.getValue3().add(sentenceIndex);
				result.terms.put(word, quartet);
				tagIndex++;
			}

			sentenceIndex++;
		}
		logger.info(MessageFormat.format("document contains {0} terms.", result.terms.size()));
		logger.info(MessageFormat.format("document contains {0} sentences.", result.sentences.size()));
		logger.info(MessageFormat.format("document contains {0} words.", result.countWords));
		return result;
	}

	/**
	 * This method split a text in sentences. Based on german sentences model.
	 *
	 * @param bodyContentHandler {@link BodyContentHandler}
	 * @return {@code List<String>}
	 * @throws IOException
	 */
	private List<String> getSentences(BodyContentHandler bodyContentHandler) throws IOException {

		List<String> results = new ArrayList<>();

		String[] sentences = sentenceDetector.sentDetect(bodyContentHandler.toString());
		for (String sentence : sentences) {
			results.add(sentence);
		}
		logger.info(MessageFormat.format("textcorpus contains {0} sentences.", results.size()));
		return results;

	}

	public boolean isOpenNLP() {
		return isOpenNLP;
	}

	/**
	 * This method normalized a term freuency.<br>
	 * equation = log(1+f(t,d))
	 * 
	 * @see <a href=https://en.wikipedia.org/wiki/Tf%E2%80%93idf> tf idf calculation
	 *      on wikipedia</a>
	 *
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
			quartet = quartet.setAt1(Math.log10(1.0 + (double) quartet.getValue0()));
			result.terms.put(term, quartet);
		}
		result.isNormalized = true;
		logger.info("log normalization completed.");
		return result;
	}

	/**
	 * This method count a term in a sentence.
	 * <p>
	 * 
	 * {@link TextAnalyser#splitSentenceOpenNLP(String sentence)} or <br>
	 * {@link TextAnalyser#splitSentenceLucene(String sentence)}
	 * 
	 * @param term
	 * @param sentence
	 * @return int
	 * @throws IOException
	 */
	private int senetencContainsTerm(final String term, final String sentence) throws IOException {

		SnowballStemmer snowballStemmerOpenNLP = new SnowballStemmer(SnowballStemmer.ALGORITHM.GERMAN);
		String temp = "";
		List<String> words = (isOpenNLP) ? splitSentenceOpenNLP(sentence) : splitSentenceLucene(sentence);
		for (String word : words) {
			temp = (withStemming) ? snowballStemmerOpenNLP.stem(word).toString() : word;
			if (term.equals(temp)) {
				return 1;
			}

		}
		return 0;
	}

	/**
	 * This method sort a map by value.
	 * <p>
	 * Quartet {@code Map<String, Quartet<Integer, Double, String, Set<Integer>>>}
	 * <p>
	 * 
	 * @param map
	 * @param isDescending {@code true} or {@code false}
	 * @return new reference of
	 *         {@code Map<String, Quartet<Integer, Double, String, Set<Integer>>>}
	 */
	private Map<String, Quartet<Integer, Double, String, Set<Integer>>> sortbyValue(
			final Map<String, Quartet<Integer, Double, String, Set<Integer>>> map, boolean isDescending) {

		double negation = (isDescending) ? -1.0 : 1.0;

		Map<String, Quartet<Integer, Double, String, Set<Integer>>> sortedMap = map.entrySet().stream()
				.sorted(Comparator.comparingDouble(value -> negation * value.getValue().getValue1()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> {
					throw new AssertionError();
				}, LinkedHashMap::new));
		return sortedMap;

	}

	/**
	 * This method split sentences in each terms
	 * <p>
	 * use pattern in {@link TextAnalyser#replacePattern}
	 *
	 * @param sentence
	 * @return {@code List<String>}
	 * @throws IOException
	 */
	private List<String> splitSentenceLucene(final String sentence) throws IOException {

		String temp = "";
		List<String> results = new ArrayList<String>();
		Analyzer analyzer;

		analyzer = CustomAnalyzer.builder().withTokenizer(tokenizerName).addTokenFilter("hyphenatedwords").build();
		TokenStream stream = analyzer.tokenStream(null, new StringReader(sentence));
		CharTermAttribute attr = stream.addAttribute(CharTermAttribute.class);
		stream.reset();
		while (stream.incrementToken()) {
			temp = attr.toString().trim().replaceAll(replacePattern, "");
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
	 * <p>
	 * use pattern in {@link TextAnalyser#replacePattern}
	 *
	 * @param sentence
	 * @return {@code List<String>}
	 */
	private List<String> splitSentenceOpenNLP(final String sentence) {

		String temp = "";
		List<String> results = new ArrayList<String>();
		String[] sentences = tokenizer.tokenize(sentence);
		for (String word : sentences) {
			temp = word.trim().replaceAll(replacePattern, "");
			if (temp.length() > minLenght) {
				results.add(temp);
			}

		}
		// logger.info(MessageFormat.format("sentence contains {0} words",
		// results.size()));
		return results;
	}

	/**
	 * This method stem all words in documentCount and merge the count of theme.
	 * <br>
	 * stemmed by {@link opennlp.tools.stemmer.snowball.SnowballStemmer}
	 * 
	 * @param documentCount
	 * @return new reference of {@link DocumentCount}
	 */
	private DocumentCount stem(final DocumentCount documentCount) {
		String stemmed = "";
		DocumentCount result = new DocumentCount();
		result.sentences = documentCount.sentences;
		int calculateWordCount = 0;

		Set<Integer> sentenceIndies = new HashSet<>();
		Quartet<Integer, Double, String, Set<Integer>> quartet = Quartet.with(0, 0.0, "", sentenceIndies);

		SnowballStemmer snowballStemmerOpenNLP = new SnowballStemmer(SnowballStemmer.ALGORITHM.GERMAN);
		for (String key : documentCount.terms.keySet()) {
			quartet = documentCount.terms.get(key);
			calculateWordCount += quartet.getValue0();
			stemmed = snowballStemmerOpenNLP.stem(key).toString();
			if (!result.terms.containsKey(stemmed)) {
				quartet = quartet.setAt0(calculateWordCount);
				result.terms.put(stemmed, quartet);
				continue;
			}
			result.terms.put(stemmed, quartet);
		}

		if (documentCount.countWords != calculateWordCount) {
			logger.warn(MessageFormat.format("orgin wordcount: {0} new count {1}", documentCount.sentences.size(),
					calculateWordCount));
		}
		result.countWords = documentCount.countWords;
		return result;
	}

	/**
	 * This method tests different stemmers (german) and store the result in a file
	 *
	 * @param bodyContentHandler contains german text
	 */
	public void testStem(final BodyContentHandler bodyContentHandler) {

		try {
			String stemmedWordCistem = "";
			String stemmedWordOpenNLP = "";

			StringBuilder stringBuilder = new StringBuilder();
			Formatter formatter = new Formatter(stringBuilder, Locale.GERMAN);
			List<String> sentences = getSentences(bodyContentHandler);
			List<String> opennlp = new ArrayList<>();
			List<String> lucene = new ArrayList<>();

			SnowballStemmer snowballStemmerOpenNLP = new SnowballStemmer(SnowballStemmer.ALGORITHM.GERMAN);

			for (String sentence : sentences) {

				opennlp.addAll(splitSentenceOpenNLP(sentence));
				lucene.addAll(splitSentenceLucene(sentence));

			}

			formatter.format("%1$25s --> %2$25s --> %3$25s --> %4$25s \n", "orgin", "Cistem", "snowball", "opennlp");
			for (String word : opennlp) {

				stemmedWordOpenNLP = snowballStemmerOpenNLP.stem(word).toString();
				stemmedWordCistem = Cistem.stem(word);
				formatter.format("%1$25s --> %2$25s --> %3$25s \n", word, stemmedWordCistem, stemmedWordOpenNLP);

			}
			BufferedWriter writer = new BufferedWriter(
					new BufferedWriter(new FileWriter("D:\\Keyword extraction\\tfidf\\OpenNLP_stemming.txt", false)));
			writer.write(stringBuilder.toString());
			writer.flush();
			writer.close();
			stringBuilder.delete(0, stringBuilder.length());

			formatter.format("%1$25s --> %2$25s --> %3$25s \n", "orgin", "Cistem", "opennlp");
			for (String word : lucene) {

				stemmedWordOpenNLP = snowballStemmerOpenNLP.stem(word).toString();
				stemmedWordCistem = Cistem.stem(word);
				formatter.format("%1$25s --> %2$25s --> %3$25s -->\n", word, stemmedWordCistem, stemmedWordOpenNLP);

			}
			writer = new BufferedWriter(
					new BufferedWriter(new FileWriter("D:\\Keyword extraction\\tfidf\\Lucene_stemming.txt", false)));
			writer.write(stringBuilder.toString());
			writer.flush();
			writer.close();
			formatter.close();
			stringBuilder.delete(0, stringBuilder.length());

		} catch (IOException exception) {

			exception.printStackTrace();
		}

	}

	public boolean withStemming() {
		return withStemming;
	}

	public void testWriteSentenesToFile(final String destination, final List<String> sentences) {
		try {
			StringBuilder stringBuilder = new StringBuilder();

			for (String sentence : sentences) {
				stringBuilder.append(sentence);
				stringBuilder
						.append("################################################################################\n");
			}

			BufferedWriter writer = new BufferedWriter(new BufferedWriter(new FileWriter(destination, false)));
			writer.write(stringBuilder.toString());
			writer.close();
		} catch (IOException exception) {
			logger.error(exception.getMessage());
		}
	}

}
