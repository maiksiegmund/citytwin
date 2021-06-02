package de.citytwin;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

public class TextCleaner {
	private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static SentenceDetectorME sentenceDetector;
	private static BodyContentHandler bodyContentHandler;
	// 2.4 Planerische Ausgangssituation ............................................................................ 14
	private static String tabelOfContentNumeralPattern = "^[\\d+\\.]+[\\s\\w]*\\.{3,}\\s*\\d*";
	// example II. Planinhalt..................................................................................
	private static String tabelOfContentRomanPattern = "^[I|V|X]+\\.*\\s*[\\w ]*\\.{3,}\\s*\\d*";

	public TextCleaner(BodyContentHandler bodyContentHandler) throws IOException {

		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("de-sent.bin");
		SentenceModel sentenceModel = new SentenceModel(inputStream);
		sentenceDetector = new SentenceDetectorME(sentenceModel);
		inputStream.close();
		this.bodyContentHandler = bodyContentHandler;

	}

	public List<String> getSentences() throws IOException {
		List<String> results = spliteBodyContentToSencences();

		results = removePattern(results, tabelOfContentNumeralPattern); // hyponated words
		results = removePattern(results, tabelOfContentRomanPattern); // hyponated words
		results = removePattern(results, "\n"); // new lines

		return results;

	}

	private List<String> spliteBodyContentToSencences() throws IOException {

		List<String> results = new ArrayList<>();

		String[] sentences = sentenceDetector.sentDetect(bodyContentHandler.toString());
		for (String sentence : sentences) {
			results.add(sentence);
		}
		logger.info(MessageFormat.format("textcorpus contains {0} sentences.", results.size()));
		return results;
	}

	private List<String> removePattern(final List<String> senteneces, String pattern) {
		List<String> results = new ArrayList<String>();
		String replaced = "";

		for (String sentence : senteneces) {
			replaced = sentence;
			replaced = replaced.replace("Ü", "U");
			replaced = replaced.replace("Ö", "O");
			replaced = replaced.replace("Ä", "A");
			replaced = replaced.replace("ü", "u");
			replaced = replaced.replace("ö", "o");
			replaced = replaced.replace("ä", "a");
			replaced = replaced.replace("ß", "ss");

			replaced = replaced.replaceAll(pattern, "");
			results.add(replaced);
		}
		return results;
	}

	private static String getRomanNumeralsAsAlternation(int n) {

		if (n < 0)
			throw new IllegalArgumentException("n must be largern than 0");

		String[] romanNumerals = { "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII",
				"XIV", "XV", "XVI", "XVIII", "XIX", "XX", "XXI", "XXII", "XXIII", "XXIV", "XXV", "XXVI", "XXVII",
				"XXVIII", "XXIX", "XXX", "XXXI", "XXXII", "XXXIII", "XXXIV", "XXXV", "XXXVI", "XXXVII", "XXXVIII",
				"XXXIX", "XL", "XLI", "XLII", "XLIII", "XLIV", "XLV", "XLVI", "XLVII", "XLVIII", "XLIX", "L", "LI",
				"LII", "LIII", "LIV", "LV", "LVI", "LVII", "LVIII", "LIX", "LX", "LXI", "LXII", "LXIII", "LXIV", "LXV",
				"LXVI", "LXVII", "LXVIII", "LXIX", "LXX", "LXXI", "LXXII", "LXXIII", "LXXIV", "LXXV", "LXXVI", "LXXVII",
				"LXXVIII", "LXXIX", "LXXX", "LXXXI", "LXXXII", "LXXXIII", "LXXXIV", "LXXXV", "LXXXVI", "LXXXVII",
				"LXXXVIII", "LXXXIX", "XC", "XCI", "XCII", "XCIII", "XCIV", "XCV", "XCVI", "XCVII", "XCVIII", "XCIX",
				"C" };
		StringBuilder stringBuilder = new StringBuilder();
		String result = "";

		for (int index = 0; index < n && index < romanNumerals.length; ++index) {
			stringBuilder.append(romanNumerals[index] + "." + "|");
		}
		result = stringBuilder.toString();
		int lastIndexOfPipeSign = result.lastIndexOf("|");
		return result.substring(lastIndexOfPipeSign);
	}

}
