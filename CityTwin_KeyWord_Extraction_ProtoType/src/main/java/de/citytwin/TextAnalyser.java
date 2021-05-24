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
	 * This method calculate term frequency. used apache opennlp and apache lucene
	 * 
	 * @param handBodyContentHandler (contains the german text)
	 * @return Map<String, Map<String, Integer>>
	 */
	public Map<String, Map<String, Double>> doTfIDF(BodyContentHandler bodyContentHandler) {

		RawCount lucene = new RawCount();
		RawCount openNLP = new RawCount();

		Map<String, Map<String, Double>> results = new HashMap<String, Map<String, Double>>();

		try {

			openNLP = rawCount(bodyContentHandler, true);
			lucene = rawCount(bodyContentHandler, false);

			RawCount openNLPStemmed = stem(openNLP);
			RawCount luceneStemmed = stem(lucene);

			Map<String, Double> normalizetfLuceneStemm = calculateTermFrequency(luceneStemmed);
			Map<String, Double> normalizetfopenNLPStem = calculateTermFrequency(openNLPStemmed);

		} catch (IOException exception) {
			logger.error(exception.getMessage());
		}

		Map<String, Double> normalizetfLucene = calculateTermFrequency(lucene);
		Map<String, Double> normalizetfopenNLP = calculateTermFrequency(openNLP);

		results.put("tflucene", normalizetfLucene);
		results.put("tfopenNLP", normalizetfopenNLP);

		return results;

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
	private List<String> getSentences(BodyContentHandler handBodyContentHandler) throws IOException {

		List<String> results = new ArrayList<String>();
		String[] sentences = sentenceDetector.sentDetect(handBodyContentHandler.toString());
		for (String sentence : sentences) {
			logger.info("detected sentences");
			logger.info(sentence);
			results.add(sentence);
		}

		return results;

	}

	/**
	 * This method tests different stemmers (german) and store the result in a file
	 *
	 * @param bodyContentHandler contains german text
	 */
	public void testStem(BodyContentHandler bodyContentHandler) {

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
	 * This method split a sentence in a list of words.
	 *
	 * @param sentence contains a german sentence
	 * @return List<String> of words in lower case and trimed
	 * @throws IOException
	 */
	private List<String> splitSentenceLucene(String sentence) throws IOException {

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
				logger.info(MessageFormat.format("transform word: {0} to {1}", attr.toString(), temp));
				results.add(temp);
			}
		}
		analyzer.close();
		stream.close();
		return results;
	}

	/**
	 * This method split a sentence in each word.
	 *
	 * @param sentence german text
	 * @return List<String> of words in trimed and replace @field replacePattern
	 */
	private List<String> splitSentenceOpenNLP(String sentence) {

		String temp = "";
		List<String> results = new ArrayList<String>();
		String[] sentences = tokenizer.tokenize(sentence);
		for (String word : sentences) {
			temp = word.trim().replaceAll(replacePattern, "");
			if (temp.length() > minLenght) {
				logger.info(MessageFormat.format("transform word: {0} to {1}", word, temp));
				results.add(temp);
			}

		}
		return results;
	}

	/**
	 * This method sort a map by value.
	 *
	 * @param unsorted     Map<String, Double>
	 * @param isDescending
	 * @return sorted Map<String, Double>
	 */
	private Map<String, Double> sortbyValue(Map<String, Double> map, boolean isDescending) {

		double negation = (isDescending) ? -1.0 : 1.0;

		Map<String, Double> sortedMap = map.entrySet().stream()
				.sorted(Comparator.comparingDouble(value -> negation * value.getValue()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> {
					throw new AssertionError();
				}, LinkedHashMap::new));

		return sortedMap;

	}

	private Map<String, Double> calculateTermFrequency(RawCount rawCount) {

		Map<String, Double> results = sortbyValue(rawCount.map, true);
		double max = results.entrySet().iterator().next().getValue();
		double count = 0.0;

		for (String key : results.keySet()) {
			count = results.get(key);
			results.put(key, count / max);
		}

		return results;
	}

	/**
	 * This method split a document in sentences and in words and count theme.
	 *
	 * @param bodyContentHandler contains a german document as plain text
	 * @param isOpenNLP          (either OpenNLP or Lucene Tools)
	 * @return RawCountResult
	 */
	private RawCount rawCount(BodyContentHandler bodyContentHandler, boolean isOpenNLP) throws IOException {

		RawCount result = new RawCount();

		List<String> sentences = getSentences(bodyContentHandler);

		result.map = new HashMap<String, Double>();
		result.countSentences = sentences.size();
		result.countWords = 0;

		logger.info("text in sentecens completed");

		for (String sentence : sentences) {
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
		return result;
	}

	/**
	 * This method calculate a term freuency. rawcount / countWords
	 * https://en.wikipedia.org/wiki/Tf%E2%80%93idf
	 *
	 * @param RawCount
	 * @return new reference of Map<String, Double>
	 */
	private Map<String, Double> termFrequency(final RawCount rawCount) {

		Map<String, Double> result = new HashMap<>(rawCount.map.size());
		for (String key : rawCount.map.keySet()) {
			result.put(key, rawCount.map.get(key).doubleValue() / (double) rawCount.countWords);
		}
		return result;
	}

	/**
	 * This method normalized a term freuency. equation = log(1+f(t,d))
	 * https://en.wikipedia.org/wiki/Tf%E2%80%93idf
	 *
	 * @param Map<String, Double>
	 * @return new reference of Map<String, Double>
	 */
	private Map<String, Double> logNormalization(final Map<String, Double> map) {
		Map<String, Double> result = new HashMap<>(map.size());
		for (String key : map.keySet()) {
			result.put(key, Math.log(1.0 + map.get(key).doubleValue()));
		}
		return result;
	}

	/**
	 * This method normalized a term freuency. 
	 * equation = k +( 1 - K) f(t,d) /
	 * (max(f(t,d))) 
	 * https://en.wikipedia.org/wiki/Tf%E2%80%93idf
	 *
	 * @param RawCount
	 * @return new reference of Map<String, Double>
	 */
	private Map<String, Double> doubleNormalization(final Map<String, Double> map, double K) {
		Map<String, Double> result = new HashMap<>(map.size());
		Map<String, Double> sorted = sortbyValue(map, true);
		double max = sorted.entrySet().iterator().next().getValue();
		
		for(String Key : map.keySet()) {
			
		}
		
		
		return result;
	}

	/**
	 * This method stem all words in rawCount and merge the count of theme. stemmed
	 * by @see opennlp.tools.stemmer.snowball.SnowballStemmer
	 * 
	 * @param rawCount
	 * @return new reference of RawCount
	 */
	private RawCount stem(final RawCount rawCount) {
		String stemmed = "";
		RawCount result = new RawCount();
		double oldValue = 0.0;
		int calculateWordCount = 0;
		result.map = new HashMap<String, Double>(rawCount.map.size());
		SnowballStemmer snowballStemmerOpenNLP = new SnowballStemmer(SnowballStemmer.ALGORITHM.GERMAN);
		for (String key : rawCount.map.keySet()) {
			oldValue = rawCount.map.get(key);
			calculateWordCount += (int) rawCount.map.get(key).intValue();
			stemmed = snowballStemmerOpenNLP.stem(key).toString();
			if (!result.map.containsKey(stemmed)) {
				result.map.put(stemmed, oldValue);
				continue;
			}
			result.map.put(stemmed, oldValue);
		}

		if (rawCount.countWords != calculateWordCount) {
			logger.warn(MessageFormat.format("orgin wordcount: {0} new count {1}", rawCount.countSentences,
					calculateWordCount));
		}
		result.countSentences = rawCount.countSentences;
		result.countWords = rawCount.countWords;

		return result;
	}

	/**
	 * This inner class represent result rawcount only use here.
	 */
	class RawCount {
		public Map<String, Double> map;
		public int countSentences;
		public int countWords;
	}

}
