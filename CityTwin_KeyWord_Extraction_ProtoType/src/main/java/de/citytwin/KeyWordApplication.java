package de.citytwin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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

//		runTFIDF();
		getResults();
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

	public static StringBuilder calculationTFIDF(List<File> files, TFIDFTextAnalyser tfidfTextAnalyser,
			List<String> posTags, String description) {
		StringBuilder stringBuilder = new StringBuilder();
		Formatter formatter = new Formatter(stringBuilder, Locale.GERMAN);
		BodyContentHandler bodyContentHandler = null;
		DocumentConverter documentConverter = new DocumentConverter();
		Map<String, Quartet<Integer, Double, String, Set<Integer>>> results = null;
		Quartet<Integer, Double, String, Set<Integer>> quartet = null;

		try {

			for (File file : files) {
				bodyContentHandler = documentConverter.documentToBodyContentHandler(file);
				LocalDateTime startTime = LocalDateTime.now();
				results = tfidfTextAnalyser.calculateTFIDF(bodyContentHandler, posTags,
						TFIDFTextAnalyser.NormalizationType.NONE);
				LocalDateTime endTime = LocalDateTime.now();
				// caption
				formatter.format("#file: %1$34s", file.getName() + "\n");
				formatter.format("#start %1$34s --> %2$10s --> %3$15s --> %4$10s --> %5$s", "term", "count", "TFIDF Score",
						"PosTAG", "SentenceIndex \n");
				for (String key : results.keySet()) {
					quartet = results.get(key);
					String sentenceIndies = "";
					int count = 0;
					for (Integer index : quartet.getValue3()) {
						if (count < 10) {
							sentenceIndies += index.toString() + ",";
						}
						else {
							sentenceIndies += "...";
							break;
						}
						count++;
					}
					formatter.format("%1$35s ### %2$10s ### %3$.15f ### %4$10s ### %5$s", key,
							quartet.getValue0().toString(), quartet.getValue1().doubleValue(), quartet.getValue2(),
							sentenceIndies);
					stringBuilder.append("\n");
				}
				stringBuilder.append("#end duration calculated td idf: "
						+ String.valueOf(startTime.until(endTime, ChronoUnit.MINUTES)) + " min(s) " + description
						+ " \n");
			}
			formatter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return stringBuilder;

	}

	public static void getResults() {
		try {
			String[] descriptions = { 
//					"Lucene ", 
//					"Lucene + Stemming ", 
					"Lucene + Stemming + Stopwordfilter ",
//					"opennlp ", 
//					"opennlp + Stemming ", 
					"opennlp + Stopwordfilter " 
					};
			TFIDFTextAnalyser[] textAnalysers = { 
//					new TFIDFTextAnalyser().withLucene(),
//					new TFIDFTextAnalyser().withLucene().withStemming(),
					new TFIDFTextAnalyser().withLucene().withStemming().withStopwordFilter(),
//					new TFIDFTextAnalyser().withOpenNLP(), 
//					new TFIDFTextAnalyser().withOpenNLP().withStemming(),
					new TFIDFTextAnalyser().withOpenNLP().withStopwordFilter() };
			StringBuilder stringBuilder = new StringBuilder();

			for (int index = 0; index < textAnalysers.length; index++) {
				stringBuilder.append(KeyWordApplication.calculationTFIDF(getFiles(), textAnalysers[index], null,
						descriptions[index]));
			}

			BufferedWriter writer = new BufferedWriter(
					new BufferedWriter(new FileWriter("D:\\Keyword extraction\\tfidf\\tfidf_result.txt", false)));
			writer.write(stringBuilder.toString());
			writer.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

	public static List<File> getFiles() {

		String directory = "D:\\vms\\sharedFolder\\";

		List<File> results = new ArrayList();
		results.add(new File(directory + "festsetzungbegruendung-xvii-50aa.pdf"));
//		results.add(new File(directory + "Angebotsmassnahmen_2017.pdf"));
//		results.add(new File(directory + "beg4b-042.pdf"));
//		results.add(new File(directory + "Bekanntmachungstext.pdf"));
		results.add(new File(directory + "FNP Bericht 2020.pdf"));
//		results.add(new File(directory + "Strategie-Stadtlandschaft-Berlin.pdf"));
		results.add(new File(directory + "biologische_vielfalt_strategie.pdf"));
//		results.add(new File(directory + "modell_baulandentwicklung.docx"));
		results.add(new File(directory + "Charta Stadtgrün.docx"));

		return results;

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
