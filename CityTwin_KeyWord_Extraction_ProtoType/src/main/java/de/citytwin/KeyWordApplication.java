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

		runTFIDF();


	}

	public static void runTFIDF() {

		try {
			StringBuilder stringBuilder = new StringBuilder();
			DocumentConverter documentConverter = new DocumentConverter();
			TFIDFTextAnalyser tFIDFTextAnalyser = new TFIDFTextAnalyser().withOpenNLP();
			Map<String, Quartet<Integer, Double, String, Set<Integer>>> result;

			File file = new File("D:\\vms\\sharedFolder\\festsetzungbegruendung-xvii-50aa.pdf");
//			File file = new File("D:\\vms\\sharedFolder\\testdata.txt");
			Formatter formatter = new Formatter(stringBuilder, Locale.GERMAN);
			BodyContentHandler bodyContentHandler = documentConverter.documentToBodyContentHandler(file);
			// documentConverter.saveAsTextFile(bodyContentHandler,
			// "D:\\vms\\sharedFolder\\festsetzungbegruendung-xvii-50aa.txt");

			result = tFIDFTextAnalyser.calculateTFIDF(bodyContentHandler, null,
					TFIDFTextAnalyser.NormalizationType.DOUBLE);
			Quartet<Integer, Double, String, Set<Integer>> quartet = null;

			formatter.format("%1$35s --> %2$5s --> %3$15s --> %4$10s --> %5$15s", "term", "count", "TFIDF Score",
					"Pos TAG", "Sent Index\n");
			for (String key : result.keySet()) {
				quartet = result.get(key);
				String sentenceIndies = "";
				for (Integer index : quartet.getValue3()) {
					sentenceIndies += index.toString() + ", ";
				}
				formatter.format("%1$35s --> %2$5s --> %3$.13f --> %4$10s --> %5$s", key,
						quartet.getValue0().toString(), quartet.getValue1().doubleValue(), quartet.getValue2(),
						sentenceIndies);
				stringBuilder.append("\n");

//					System.out.print(MessageFormat.format("spliter: {0} --> word: {1} --> count: {2} ", spliter, word,
//							tfresults.get(spliter).get(word)));
//					System.out.print("\n");
			}

			BufferedWriter writer = new BufferedWriter(new BufferedWriter(
					new FileWriter("D:\\Keyword extraction\\tfidf\\tfidf_" + file.getName() + "_opennlp.txt", false)));
			writer.write(stringBuilder.toString());
			writer.close();

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());

		}
		return;
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
