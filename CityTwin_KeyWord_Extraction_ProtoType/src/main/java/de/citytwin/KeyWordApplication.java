package de.citytwin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.util.Formatter;
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
			TextAnalyser textAnalyser = new TextAnalyser(false, false);
			Map<String, Double> result;

			//File file = new File("D:\\vms\\sharedFolder\\festsetzungbegruendung-xvii-50aa.pdf");
			File file = new File("D:\\vms\\sharedFolder\\auszug.txt");
			Formatter formatter = new Formatter(stringBuilder, Locale.GERMAN);
			BodyContentHandler bodyContentHandler = documentConverter
					.documentToText(file);

			result = textAnalyser.calculateTfIDF(bodyContentHandler);

//			result = textAnalyser.calculateTfIDF(bodyContentHandler, 8);

//			result = textAnalyser.calculateTfIDF(
//					documentConverter.documentToText(new File("D:\\Keyword extraction\\testdata\\60words.txt")));

			for (String key : result.keySet()) {
				formatter.format("%1$25s --> %2$.4f", key, result.get(key).doubleValue());
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
