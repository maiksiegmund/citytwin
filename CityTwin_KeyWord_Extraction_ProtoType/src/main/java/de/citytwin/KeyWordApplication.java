package de.citytwin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.tika.sax.BodyContentHandler;
import org.javatuples.Quartet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyWordApplication {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		//runtf();
		// test();
		testNewLineCount();

	}

	public static void runtf() {

		try {
			StringBuilder stringBuilder = new StringBuilder();
			DocumentConverter documentConverter = new DocumentConverter();
			TextAnalyser textAnalyser = new TextAnalyser(false, false);
			Map<String, Quartet<Integer, Double, String, Set<Integer>>> result;

			File file = new File("D:\\vms\\sharedFolder\\festsetzungbegruendung-xvii-50aa.pdf");
//			 File file = new File("D:\\vms\\sharedFolder\\testdata.txt");
			Formatter formatter = new Formatter(stringBuilder, Locale.GERMAN);
			BodyContentHandler bodyContentHandler = documentConverter.documentToBodyContentHandler(file);
			// documentConverter.saveAsTextFile(bodyContentHandler,
			// "D:\\vms\\sharedFolder\\festsetzungbegruendung-xvii-50aa.txt");

			result = textAnalyser.calculateTFIDF(bodyContentHandler, KeyWordApplication.getTagFilters(),
					TextAnalyser.NormalizationType.LOG);
			Quartet<Integer, Double, String, Set<Integer>> quartet = null;

			formatter.format("%1$25s --> %2$5s --> %3$15s --> %4$10s --> %5$15s", "term", "count", "TFIDF Score",
					"Pos TAG", "Sent Index\n");
			for (String key : result.keySet()) {
				quartet = result.get(key);
				String sentenceIndies = "";
				for (Integer index : quartet.getValue3()) {
					sentenceIndies += index.toString() + ", ";
				}
				formatter.format("%1$25s --> %2$5s --> %3$.13f --> %4$10s --> %5$s", key,
						quartet.getValue0().toString(), quartet.getValue1().doubleValue(), quartet.getValue2(),
						sentenceIndies);
				stringBuilder.append("\n");

//					System.out.print(MessageFormat.format("spliter: {0} --> word: {1} --> count: {2} ", spliter, word,
//							tfresults.get(spliter).get(word)));
//					System.out.print("\n");
			}

			BufferedWriter writer = new BufferedWriter(new BufferedWriter(
					new FileWriter("D:\\Keyword extraction\\tfidf\\tfidf_" + file.getName() + "_lucene.txt", false)));
			writer.write(stringBuilder.toString());
			writer.close();

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());

		}
		return;
	}

	public static void testNewLineCount() {

		DocumentConverter documentConverter = new DocumentConverter();
		File file = new File("D:\\vms\\sharedFolder\\festsetzungbegruendung-xvii-50aa.pdf");
		BodyContentHandler bodyContentHandler = documentConverter.documentToBodyContentHandler(file);

		try {
			Integer count = 0;
			Integer index = 0;
			TextAnalyser textAnalyser = new TextAnalyser(false, false);
			List<String> sentences = textAnalyser.testGetSentences(bodyContentHandler);
			for (String sentence : sentences) {
				count = textAnalyser.CountNewLine(sentence);
				System.out.println("newline count: " + count.toString() + " index " + index.toString());
				if (count < 5)
					System.out.println(sentence);
				index++;	
			}

		} catch (IOException exception) {
			// TODO Auto-generated catch block
			exception.printStackTrace();
		}

	}

	public static void test() {

//		DocumentConverter documentConverter = new DocumentConverter();
//		File file = new File("D:\\vms\\sharedFolder\\festsetzungbegruendung-xvii-50aa.pdf");
//		BodyContentHandler bodyContentHandler = documentConverter.documentToBodyContentHandler(file);
		try {
			TextCleaner textCleaner = new TextCleaner(null);
			textCleaner.test();

			writeSentenesToFile(textCleaner.getSentences(),
					"D:\\vms\\sharedFolder\\festsetzungbegruendung-xvii-50aa.txt");
		} catch (IOException exception) {
			// TODO Auto-generated catch block
			logger.error(exception.getMessage());
		}

	}

	public static List<String> getTagFilters() {

		List<String> tagFilters = new ArrayList<String>();

//		tagFilters.add("CC");
//		tagFilters.add("CD");
//		tagFilters.add("DT");
//		tagFilters.add("EX");
//		tagFilters.add("FW");
//		tagFilters.add("IN");
//		tagFilters.add("JJ");
//		tagFilters.add("JJR");
//		tagFilters.add("JJS");
//		tagFilters.add("LS");
		tagFilters.add("MD");
		tagFilters.add("NN");
		tagFilters.add("NNS");
		tagFilters.add("NNP");
		tagFilters.add("NNPS");
//		tagFilters.add("PDT");
//		tagFilters.add("POS");
//		tagFilters.add("PRP");
//		tagFilters.add("PRP$");
//		tagFilters.add("RB");
//		tagFilters.add("RBR");
//		tagFilters.add("RBS");
//		tagFilters.add("RP");
//		tagFilters.add("SYM");
//		tagFilters.add("TO");
//		tagFilters.add("UH");
//		tagFilters.add("VB");
//		tagFilters.add("VBD");
//		tagFilters.add("VBG");
//		tagFilters.add("VBN");
//		tagFilters.add("VBP");
//		tagFilters.add("VBZ");
//		tagFilters.add("WDT");
//		tagFilters.add("WP");
//		tagFilters.add("WP$");
//		tagFilters.add("WRB");

		return tagFilters;

	}

	public static void writeSentenesToFile(final List<String> sentences, final String destination) {
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
