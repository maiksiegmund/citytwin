package de.citytwin;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
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
	private int minLenght = 3;
	private String replacePattern = "[^\\wäÄüÜöÖß]";
	private SentenceDetectorME sentenceDetector;
	private Tokenizer tokenizer;

	/**
	 * This method calculate term frequency. used apache opennlp and apache lucene
	 * 
	 * @param handBodyContentHandler (contains the german text)
	 * @return Map<String, Map<String, Integer>>
	 */
	public Map<String, Map<String, Double>> doTfIDF(BodyContentHandler bodyContentHandler) {

		Map<String, Double> tfLucene = new HashMap<String, Double>();
		Map<String, Double> tfopenNLP = new HashMap<String, Double>();
		Map<String, Map<String, Double>> results = new HashMap<String, Map<String, Double>>();

		try {
			List<String> sentences = getSentences(bodyContentHandler);
			logger.info("text in sentecens completed");
			for (String sentence : sentences) {
				logger.info(sentence);
				for (String word : splitSentenceLucene(sentence)) {
					if (!tfLucene.containsKey(word)) {
						tfLucene.put(word, 1.0);
						continue;
					}
					tfLucene.put(word, tfLucene.get(word) + 1.0);
				}
				for (String word : splitSentenceOpenNLP(sentence)) {
					if (!tfopenNLP.containsKey(word)) {
						tfopenNLP.put(word, 1.0);
						continue;
					}
					tfopenNLP.put(word, tfopenNLP.get(word) + 1.0);
				}

			}

		} catch (IOException exception) {
			logger.error(exception.getMessage());
		}
//
		Map<String, Double> sortedtfLucene = sortbyValue(tfLucene, false);
		Map<String, Double> sortedtfopenNLP = sortbyValue(tfopenNLP, true);

		results.put("tflucene", sortedtfLucene);
		results.put("tfopenNLP", sortedtfopenNLP);
		
		
		
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

		analyzer = CustomAnalyzer.builder().withTokenizer(tokenizerName)
//			      .addTokenFilter("stop")
//				  .addTokenFilter("porterstem")
				.addTokenFilter("hyphenatedwords").build();
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
	 * This method split a sentence in a list of each word.
	 *
	 * @param sentence contains a german sentence
	 * @return List<String> of words in lower case and trimed
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

	private HashMap<String, Double> sortbyValue(final Map<String, Double>map, boolean descending) {

		double negation = (descending == true) ? -1.0 : 1.0;
		
		Map<String, Double> sortedMap = map.entrySet().stream()
				.sorted(Comparator.comparingDouble(value -> (negation*value.getValue())))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> {
					throw new AssertionError();
				}, LinkedHashMap::new));

		return (HashMap<String, Double>) sortedMap;

	}

	private HashMap<String, Double> normalizeFrequency(Map<String, Double> map) {

		return (HashMap<String, Double>) null;
	}

}
