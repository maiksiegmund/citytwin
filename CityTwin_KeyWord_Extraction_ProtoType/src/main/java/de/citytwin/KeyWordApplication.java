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

import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyWordApplication {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		runtf();

	}

	public static void runtf() {

		try {
			StringBuilder stringBuilder = new StringBuilder();
			DocumentConverter documentConverter = new DocumentConverter();
			TextAnalyser textAnalyser = new TextAnalyser(true, true);
			Map<String, Double> result;

			File file = new File("D:\\vms\\sharedFolder\\festsetzungbegruendung-xvii-50aa.pdf");
			//File file = new File("D:\\vms\\sharedFolder\\auszug.txt");
			Formatter formatter = new Formatter(stringBuilder, Locale.GERMAN);
			BodyContentHandler bodyContentHandler = documentConverter
					.documentToText(file);

			List<String> tagFilters = new ArrayList<String>(); 
			
//			tagFilters.add("CC");
//			tagFilters.add("CD");
//			tagFilters.add("DT");
//			tagFilters.add("EX");
//			tagFilters.add("FW");
//			tagFilters.add("IN");
//			tagFilters.add("JJ");
//			tagFilters.add("JJR");
//			tagFilters.add("JJS");
//			tagFilters.add("LS");
//			tagFilters.add("MD");
			tagFilters.add("NN");
			tagFilters.add("NNS");
			tagFilters.add("NNP");
			tagFilters.add("NNPS");
//			tagFilters.add("PDT");
//			tagFilters.add("POS");
//			tagFilters.add("PRP");
//			tagFilters.add("PRP$");
//			tagFilters.add("RB");
//			tagFilters.add("RBR");
//			tagFilters.add("RBS");
//			tagFilters.add("RP");
//			tagFilters.add("SYM");
//			tagFilters.add("TO");
//			tagFilters.add("UH");
//			tagFilters.add("VB");
//			tagFilters.add("VBD");
//			tagFilters.add("VBG");
//			tagFilters.add("VBN");
//			tagFilters.add("VBP");
//			tagFilters.add("VBZ");
//			tagFilters.add("WDT");
//			tagFilters.add("WP");
//			tagFilters.add("WP$");
//			tagFilters.add("WRB");
				
			
				
			
			
			result = textAnalyser.calculateTfIDF(bodyContentHandler, tagFilters , TextAnalyser.NormalizationType.LOG);

//			result = textAnalyser.calculateTfIDF(bodyContentHandler, 8);

//			result = textAnalyser.calculateTfIDF(
//					documentConverter.documentToText(new File("D:\\Keyword extraction\\testdata\\60words.txt")));

			for (String key : result.keySet()) {
				formatter.format("%1$25s --> %2$.10f", key, result.get(key).doubleValue());
				stringBuilder.append("\n");

//					System.out.print(MessageFormat.format("spliter: {0} --> word: {1} --> count: {2} ", spliter, word,
//							tfresults.get(spliter).get(word)));
//					System.out.print("\n");
			}
			
			BufferedWriter writer = new BufferedWriter(
					new BufferedWriter(new FileWriter("D:\\Keyword extraction\\tfidf\\tfidf_" + file.getName() + ".txt" , false)));
			writer.write(stringBuilder.toString());
			writer.close();

		} catch (Exception e) {
			logger.error(e.getMessage());

		}
		return;
	}

	public static void test() {
		DocumentConverter documentConverter = new DocumentConverter();
		TextAnalyser textAnalyser;
		try {
			textAnalyser = new TextAnalyser(true, true);
			textAnalyser.testStem(documentConverter
					.documentToText(new File("D:\\vms\\sharedFolder\\festsetzungbegruendung-xvii-50aa.pdf")));
		} catch (IOException exception) {
			// TODO Auto-generated catch block
			exception.printStackTrace();
		}
		Map<String, Map<String, Double>> tfresults;

	}

}
