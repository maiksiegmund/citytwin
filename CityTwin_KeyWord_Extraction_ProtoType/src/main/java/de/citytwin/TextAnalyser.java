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

import org.apache.cxf.common.i18n.Exception;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	 * constructor loads the pre trained models de-sent.bin and de-token.bin
	 * 
	 * @throws IOException
	 */
	public TextAnalyser() throws IOException {

		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("de-sent.bin");
		SentenceModel sentenceModel = new SentenceModel(inputStream);
		sentenceDetector = new SentenceDetectorME(sentenceModel);
		inputStream.close();

		inputStream = getClass().getClassLoader().getResourceAsStream("de-token.bin");
		TokenizerModel tokenizerModel = new TokenizerModel(inputStream);

		tokenizer = new TokenizerME(tokenizerModel);

	}

	private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private String tokenizerName = "whitespace";
	private int minLenght = 0;
	private String replacePattern = "[^a-zA-ZäÄüÜöÖß-]";
	private SentenceDetectorME sentenceDetector;
	private Tokenizer tokenizer;

	/**
	 * This method calculate term frequency and inverse document frequency by lucene
	 * or opennlp.
	 * 
	 * @param handBodyContentHandler (contains the german text)
	 * @return Map<String, Map<String, Integer>> ()
	 */
	public Map<String, Map<String, Double>> calculateTfIDF(final BodyContentHandler bodyContentHandler,
			boolean withStemming) {

		DocumentCount lucene = new DocumentCount();
		DocumentCount openNLP = new DocumentCount();

		Map<String, Map<String, Double>> result = new HashMap<String, Map<String, Double>>();

		try {

			openNLP = getRawCount(bodyContentHandler, true);
			lucene = getRawCount(bodyContentHandler, false);

			openNLP = (withStemming) ? stem(openNLP) : openNLP;
			lucene = (withStemming) ? stem(lucene) : lucene;

			DocumentCount tfOpenNLP = calculateTermFrequency(openNLP);
			DocumentCount tflucene = calculateTermFrequency(lucene);

			DocumentCount idfOpenNLP = calculateSmootInverseDocumentFrequency(openNLP);
			DocumentCount idflucene = calculateSmootInverseDocumentFrequency(lucene);

		} catch (IOException exception) {
			logger.error(exception.getMessage());
		}

//		Map<String, Double> normalizetfLucene = calculateTermFrequency(rawCountlucene);
//		Map<String, Double> normalizetfopenNLP = calculateTermFrequency(rawCountopenNLP);
//
//		results.put("tflucene", normalizetfLucene);
//		results.put("tfopenNLP", normalizetfopenNLP);

		return result;

	}

	public void doRake(String text) {

	}

	public void doWordToVec(String text) {

	}

	public void doDocumentToVec(String text) {

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
			logger.info("detected sentences");
			logger.info(sentence);
			results.add(sentence);
		}
		logger.info(MessageFormat.format("textcorpus contains {0} sentences.", results.size()));
		return results;

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
		logger.info(MessageFormat.format("sentence contains {0} words.", results.size()));
		analyzer.close();
		stream.close();
		return results;
	}

	/**
	 * This method split a sentence in each word.
	 *
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
		logger.info(MessageFormat.format("sentence contains {0} words", results.size()));
		return results;
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
	 * This method split a document in sentences and in words and count theme.
	 *
	 * @param bodyContentHandler contains a german document as plain text
	 * @param isOpenNLP          (either OpenNLP or Lucene Tools)
	 * @return new reference of RawCount
	 */
	private DocumentCount getRawCount(final BodyContentHandler bodyContentHandler, boolean isOpenNLP)
			throws IOException {

		DocumentCount result = new DocumentCount();
		result.sentences = getSentences(bodyContentHandler);

		for (String sentence : result.sentences) {
			logger.info(sentence);
			List<String> words = (isOpenNLP) ? splitSentenceOpenNLP(sentence) : splitSentenceLucene(sentence);
			for (String word : words) {
				result.countWords++;
				if (!result.map.containsKey(word)) {
					result.map.put(word, 1.0);
					continue;
				}
				result.map.put(word, result.map.get(word) + 1.0);
			}
		}
		logger.info(MessageFormat.format("document contains {0} sentences.", result.sentences.size()));
		logger.info(MessageFormat.format("document contains {0} words.", result.countWords));
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
		result.countWords = documentCount.countWords;

		result.map = new HashMap<>(documentCount.map.size());
		for (String key : documentCount.map.keySet()) {
			result.map.put(key, documentCount.map.get(key).doubleValue() / (double) documentCount.countWords);
		}
		result.isNormalized = false;
		logger.info("calculate term frequency finished.");
		return result;
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
		result.sentences = documentCount.sentences;
		result.countWords = documentCount.countWords;

		for (String key : documentCount.map.keySet()) {
			result.map.put(key, Math.log(1.0 + documentCount.map.get(key).doubleValue()));
		}
		result.isNormalized = true;
		logger.info("log normalization completed.");
		return result;
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

	private DocumentCount calculateSmootInverseDocumentFrequency(final DocumentCount documentCount) {
		int termCount = 0;
		double value = 0.0;
		DocumentCount result = new DocumentCount();
		result.sentences = documentCount.sentences;
		result.countWords = documentCount.countWords;
		result.isNormalized = false;

		for (String key : documentCount.map.keySet()) {
			value = documentCount.sentences.size() / documentCount.map.get(key).doubleValue();
			for (String sentence : documentCount.sentences) {
				termCount += countTermInSentences(key, sentence, documentCount.isStemmed);
			}
			value = Math.log(value / (1.0 + (double) termCount)) + 1.0;
			result.map.put(key, value);
		}
		logger.info("calculate smoot inverse document frequency completed.");
		return result;
	}

	/**
	 * This method count a term in a sentence.
	 * 
	 * @param term     example (red)
	 * @param sentence example (The red car stop by red traffic light.)
	 * @return int example return 2
	 */
	private int countTermInSentences(final String term, final String sentence, boolean isStemmed) {
		int result = 0;
		SnowballStemmer snowballStemmerOpenNLP = new SnowballStemmer(SnowballStemmer.ALGORITHM.GERMAN);
		String temp = "";
		for (String word : sentence.split(" ")) {
			temp = (isStemmed) ? snowballStemmerOpenNLP.stem(word).toString() : word;
			result = (temp == term) ? result + 1 : result;

		}
		logger.info(MessageFormat.format("{0} founded {1} times in {2}.", term, result, sentence));
		return result;
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
		double oldValue = 0.0;
		int calculateWordCount = 0;
		SnowballStemmer snowballStemmerOpenNLP = new SnowballStemmer(SnowballStemmer.ALGORITHM.GERMAN);
		for (String key : documentCount.map.keySet()) {
			oldValue = documentCount.map.get(key);
			calculateWordCount += (int) documentCount.map.get(key).intValue();
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
		result.isStemmed = true;
		return result;
	}

	/**
	 * This method calculate tfidf. Equation = tfidf(t,d,D) = tf(t,d) * idf(t,D)
	 * link (https://en.wikipedia.org/wiki/Tf%E2%80%93idf)
	 * @param DocumentCount tf
	 * @param DocumentCount idf
	 * @return new reference of DocumentCount
	 */
	private DocumentCount calculateTfIDF(DocumentCount tf, DocumentCount idf) {
		double value = 0.0;
		DocumentCount result = new DocumentCount();
		result.sentences = tf.sentences;
		result.isNormalized = tf.isNormalized;
		result.isStemmed = tf.isStemmed;
		result.countWords = tf.countWords;

		for (String key : tf.map.keySet()) {
			value = tf.map.get(key).doubleValue() * idf.map.get(key).doubleValue();
			result.map.put(key, value);
		}
		logger.info("caculation tf idf completed");
		return result;
	}

	/**
	 * This inner class represent DocumentCount only use here. used as struct ...
	 * 
	 * field map (word, count) field countSentences field countWords field
	 */
	class DocumentCount {

		public DocumentCount() {
			map = new HashMap<String, Double>();
			sentences = new ArrayList<String>();
			countWords = 0;
			isNormalized = false;
			isStemmed = false;
		}

		public DocumentCount(DocumentCount documentCount) {
			map = new HashMap<String, Double>(documentCount.map);
			sentences = new ArrayList<String>(documentCount.sentences);
			countWords = documentCount.countWords;
			isNormalized = documentCount.isNormalized;
			isStemmed = documentCount.isStemmed;
		}

		public Map<String, Double> map;
		public List<String> sentences;
		public int countWords;
		public boolean isNormalized;
		public boolean isStemmed;
	}

}
