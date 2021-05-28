package de.citytwin;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

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
 * @author ma6284si
 *
 */
public class TextAnalyser {

	class CountTerm implements Runnable {

		private final String term;
		private final List<String> words;
		private int termCount;

		public CountTerm(String term, final List<String> words) {
			this.term = term;
			this.words = new ArrayList<String>(words);
			this.termCount = 0;
		}

		public int getTermCount() {
			return termCount;
		}

		@Override
		public void run() {

			for (String word : words) {

				termCount = (word.equals(term)) ? termCount + 1 : termCount;

			}

		}
	}

	/**
	 * This inner class represent DocumentCount only use here. used as struct ...
	 * 
	 * @see DocumentCount#map HashMap<String, Double> (term = word), count or
	 *      calculated result
	 * @see DocumentCount#pos HashMap<String, Double> (term = word), part of speech
	 *      tag
	 * @see DocumentCount#sentences List<String> subdocuments is d_i, D is single
	 *      Document e.g a word, pdf, txt file
	 * @see DocumentCount#countWords in |D|
	 * @see DocumentCount#isNormalized
	 */
	class DocumentCount {

		public Map<String, Double> map;

		public Map<String, String> pos;

		public List<String> sentences;
		public int countWords;
		public boolean isNormalized;
		public DocumentCount() {
			map = new HashMap<String, Double>();
			pos = new HashMap<String, String>();
			sentences = new ArrayList<String>();
			countWords = 0;
			isNormalized = false;
		}
		public DocumentCount(DocumentCount documentCount) {
			map = new HashMap<String, Double>(documentCount.map);
			pos = new HashMap<String, String>(documentCount.pos);
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
//	private static POSModel posModel;
	private static POSTaggerME posTagger;
	private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private boolean isOpenNLP = false;
	private boolean withStemming = false;
	private String tokenizerName = "whitespace";

	private int minLenght = 0;

	private String replacePattern = "[^a-zA-ZäÄüÜöÖß-]";

	/**
	 * constructor loads the pre trained models de-sent.bin, de-token.bin and
	 * de-pos-maxent.bin
	 * 
	 * @see TextAnalyser#isOpenNLP
	 * @see TextAnalyser#withStemming
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
	 * @return new reference of DocumentCount
	 * @throws IOException
	 */
	private DocumentCount calculateSmootInverseDocumentFrequency(final DocumentCount documentCount) throws IOException {
		int countSentencesWithTerm = 0;
		double value = 0.0;
		DocumentCount result = new DocumentCount();
		result.pos = documentCount.pos;
		result.sentences = documentCount.sentences;
		result.countWords = documentCount.countWords;
		result.isNormalized = false;

		int currentIndex = 0;

		for (String key : documentCount.map.keySet()) {
			value = documentCount.map.get(key).doubleValue();
			logger.info(
					MessageFormat.format("processing {0}  of {1} terms. ", ++currentIndex, documentCount.map.size()));
			for (String sentence : documentCount.sentences) {
//				termCountInSentences += countTermInSentences(key, sentence);
				countSentencesWithTerm += senetencContainsTerm(key, sentence);
			}
			value = Math.log10(value / (1.0 + countSentencesWithTerm)) + 1.0;
			result.map.put(key, value);
			countSentencesWithTerm = 0;
		}
		logger.info("calculate smoot inverse document frequency completed.");
		return result;
	}

	/**
	 * This method calculate a term freuency. rawcount / countWords
	 * https://en.wikipedia.org/wiki/Tf%E2%80%93idf
	 *
	 * @param DocumentCount
	 * @return new reference of DocumentCount
	 */
	private DocumentCount calculateTermFrequency(final DocumentCount documentCount) {

		DocumentCount result = new DocumentCount();
		result.sentences = documentCount.sentences;
		result.pos = documentCount.pos;
		result.countWords = documentCount.countWords;
		double value = 0.0;

		result.map = new HashMap<>(documentCount.map.size());
		for (String key : documentCount.map.keySet()) {
			value = documentCount.map.get(key).doubleValue() / documentCount.countWords;
			result.map.put(key, value);
		}
		result.isNormalized = false;
		logger.info("calculate term frequency finished.");
		return result;
	}

	public Map<String, Double> calculateTfIDF(final BodyContentHandler bodyContentHandler, int threadCount)
			throws IOException, InterruptedException {

		DocumentCount tfidf = new DocumentCount();

		Map<String, Double> result = new HashMap<String, Double>();

		try {
			int tempCount = 0;

			DocumentCount rawCount = new DocumentCount();
			DocumentCount termCount = new DocumentCount();

			rawCount = getRawCount(bodyContentHandler);
			rawCount = (withStemming) ? stem(rawCount) : rawCount;
			List<String> words;

			for (String sentence : rawCount.sentences) {
				words = (isOpenNLP) ? splitSentenceOpenNLP(sentence) : splitSentenceLucene(sentence);
				for (String word : words) {

					Thread[] threads = new Thread[words.size()];
					CountTerm[] countTerms = new CountTerm[words.size()];

					for (int index = 0; index < words.size(); index++) {
						countTerms[index] = new CountTerm(word, words);
					}

					for (int index = 0; index < words.size(); index++) {
						threads[index] = new Thread(countTerms[index]);
						threads[index].start();
					}

					for (int index = 0; index < words.size(); index++) {
						threads[index].join();
					}

					for (CountTerm countTerm : countTerms) {
						tempCount += countTerm.getTermCount();

					}
					termCount.map.put(word, (double) tempCount);
					tempCount = 0;

				}

			}
			// todo merge
			DocumentCount tf = calculateTermFrequency(rawCount);
			DocumentCount idf = calculateSmootInverseDocumentFrequency(rawCount);

			tfidf = calculateTfIDF(tf, idf);
			return tfidf.map;

		} catch (IOException exception) {
			logger.error(exception.getMessage());
			throw exception;
		}
	}

	/**
	 * This method calculate term frequency and inverse document frequency by lucene
	 * or opennlp.
	 * 
	 * @param bodyContentHandler (contains the german text)
	 * @param tagFilters,        keep and remove the rest
	 * @param type               which normalization (NONE, DOUBLE, LOG)
	 * @return Map<String, Map<String, Integer>>
	 * @throws IOException
	 */
	public Map<String, Double> calculateTfIDF(final BodyContentHandler bodyContentHandler,
			final List<String> tagFilters, TextAnalyser.NormalizationType type) throws IOException {

		DocumentCount result = new DocumentCount();

		try {
			DocumentCount rawCount = new DocumentCount();

			rawCount = getRawCount(bodyContentHandler);
			rawCount = (withStemming) ? stem(rawCount) : rawCount;
			DocumentCount tf;
			switch (type) {
			case DOUBLE:
				tf = doubleNormalizationTermFrequency(rawCount, 0.5);
				break;
			case LOG:
				tf = logNormalizationTermFrequency(rawCount);
				break;
			default:
				tf = calculateTermFrequency(rawCount);
				break;
			}
			DocumentCount idf = calculateSmootInverseDocumentFrequency(rawCount);
			result = calculateTfIDF(tf, idf);
			if (tagFilters.size() == 0) {
				return sortbyValue(result.map, false);
			}
			DocumentCount filtered = new DocumentCount();

			for (String tagFilter : tagFilters) {

				for (String key : result.pos.keySet()) {
					String wordPosTag = result.pos.get(key);
					if (wordPosTag.equals(tagFilter)) {
						filtered.map.put(key, result.map.get(key));
						filtered.pos.put(key, tagFilter);
					}
				}
			}
			return sortbyValue(filtered.map, false);

		} catch (IOException exception) {
			logger.error(exception.getMessage());
			throw exception;
		}
	}

	/**
	 * This method calculate tfidf. Equation = tfidf(t,d,D) = tf(t,d) * idf(t,D)
	 * link (https://en.wikipedia.org/wiki/Tf%E2%80%93idf)
	 * 
	 * @param DocumentCount tf
	 * @param DocumentCount idf
	 * @return new reference of DocumentCount
	 */
	private DocumentCount calculateTfIDF(DocumentCount tf, DocumentCount idf) {
		double value = 0.0;
		DocumentCount result = new DocumentCount();
		result.pos = tf.pos;
		result.sentences = tf.sentences;
		result.isNormalized = tf.isNormalized;
		result.countWords = tf.countWords;

		for (String key : tf.map.keySet()) {
			value = tf.map.get(key).doubleValue() * idf.map.get(key).doubleValue();
			result.map.put(key, value);
		}
		logger.info("caculation tf idf completed");
		return result;
	}

	/**
	 * This method count a term in a sentence. splits by
	 * 
	 * @see TextAnalyser#splitSentenceOpenNLP(String sentence) or
	 * @see TextAnalyser#splitSentenceLucene(String sentence)
	 * @param term     example (red)
	 * @param sentence example (The red car stop by red traffic light.)
	 * @return int example return 2
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
	 * This method normalized a term freuency. equation = k +( 1 - k) f(t,d) /
	 * (max(f(t,d))) https://en.wikipedia.org/wiki/Tf%E2%80%93idf
	 *
	 * @param Map<String, Double> (term, count)
	 * @param k           math term
	 * @return new reference of DocumentCount
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
		result.pos = documentCount.pos;
		result.sentences = documentCount.sentences;
		result.countWords = documentCount.countWords;
		Map<String, Double> sorted = sortbyValue(documentCount.map, true);

		double max = sorted.entrySet().iterator().next().getValue();

		for (String key : documentCount.map.keySet()) {
			value = k + (1.0 - k) * (documentCount.map.get(key).doubleValue() / max);
			result.map.put(key, value);
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
	 * @param bodyContentHandler contains a german document as plain text
	 * @param isOpenNLP          (either OpenNLP or Lucene Tools)
	 * @return new reference of RawCount
	 */
	private DocumentCount getRawCount(final BodyContentHandler bodyContentHandler) throws IOException {

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
				if (!result.map.containsKey(word)) {
					result.map.put(word, 1.0);
					result.pos.put(word, tags[tagIndex]);
					continue;
				}
				result.map.put(word, result.map.get(word) + 1.0);
				tagIndex++;
			}
		}
		logger.info(MessageFormat.format("document contains {0} terms.", result.map.size()));
		logger.info(MessageFormat.format("document contains {0} sentences.", result.sentences.size()));
		logger.info(MessageFormat.format("document contains {0} words.", result.countWords));
		return result;
	}

	/**
	 * This method split a text in sentences. Based on german sentences model.
	 *
	 * @param handBodyContentHandler (contains the german text)
	 * @return List<String> of sentences
	 * @throws IOException
	 */
	private List<String> getSentences(final BodyContentHandler handBodyContentHandler) throws IOException {

		List<String> results = new ArrayList<String>();
		String[] sentences = sentenceDetector.sentDetect(handBodyContentHandler.toString());
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
	 * This method normalized a term freuency. equation = log(1+f(t,d))
	 * https://en.wikipedia.org/wiki/Tf%E2%80%93idf
	 *
	 * @param DocumentCount
	 * @return new reference DocumentCount
	 * @throws IllegalArgumentException
	 */
	private DocumentCount logNormalizationTermFrequency(final DocumentCount documentCount) {
		if (documentCount.isNormalized) {
			throw new IllegalArgumentException("document already normalized!");
		}
		DocumentCount result = new DocumentCount();
		result.pos = documentCount.pos;
		result.sentences = documentCount.sentences;
		result.countWords = documentCount.countWords;

		for (String key : documentCount.map.keySet()) {
			result.map.put(key, Math.log10(1.0 + documentCount.map.get(key).doubleValue()));
		}
		result.isNormalized = true;
		logger.info("log normalization completed.");
		return result;
	}

	/**
	 * This method count a term in a sentence. splits by
	 * 
	 * @see TextAnalyser#splitSentenceOpenNLP(String sentence) or
	 * @see TextAnalyser#splitSentenceLucene(String sentence)
	 * @param term     example (red)
	 * @param sentence example (The red car stop by red traffic light.)
	 * @return int 1 or 0
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
	 *
	 * @param map          Map<String, Double>
	 * @param isDescending
	 * @return new reference of Map<String, Double>
	 */
	private Map<String, Double> sortbyValue(final Map<String, Double> map, boolean isDescending) {

		double negation = (isDescending) ? -1.0 : 1.0;

		Map<String, Double> sortedMap = map.entrySet().stream()
				.sorted(Comparator.comparingDouble(value -> negation * value.getValue()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> {
					throw new AssertionError();
				}, LinkedHashMap::new));

		return sortedMap;

	}

	/**
	 * This method split a sentence in each word.
	 *
	 * @param sentence
	 * @return new reference List<String> contain words (trimed and replaced @field
	 *         replacePattern)
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
	 * This method split a sentence in each word.
	 * 
	 * @see TextAnalyser#isOpenNLP
	 * @param sentence
	 * @return new reference List<String> contain words (trimed and replaced @field
	 *         replacePattern)
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
	 * stemmed by @see opennlp.tools.stemmer.snowball.SnowballStemmer
	 * 
	 * @param documentCount
	 * @return new reference of DocumentCount
	 */
	private DocumentCount stem(final DocumentCount documentCount) {
		String stemmed = "";
		DocumentCount result = new DocumentCount();
		result.pos = documentCount.pos;
		result.sentences = documentCount.sentences;
		double oldValue = 0.0;
		int calculateWordCount = 0;
		SnowballStemmer snowballStemmerOpenNLP = new SnowballStemmer(SnowballStemmer.ALGORITHM.GERMAN);
		for (String key : documentCount.map.keySet()) {
			oldValue = documentCount.map.get(key);
			calculateWordCount += documentCount.map.get(key).intValue();
			stemmed = snowballStemmerOpenNLP.stem(key).toString();
			if (!result.map.containsKey(stemmed)) {
				result.map.put(stemmed, oldValue);
				continue;
			}
			result.map.put(stemmed, oldValue);
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

}
