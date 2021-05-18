package de.citytwin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.util.Map;

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
			TextAnalyser textAnalyser = new TextAnalyser();
			Map<String, Map<String, Double>> tfresults;

			tfresults = textAnalyser.doTfIDF(
					documentConverter.documentToText(new File("D:\\vms\\sharedFolder\\BerlinStrategie_de_PDF.txt")));

			for (String spliter : tfresults.keySet()) {
				for (String word : tfresults.get(spliter).keySet()) {

					stringBuilder.append(
							MessageFormat.format("{0},{1}", word, Double.valueOf(tfresults.get(spliter).get(word))));
					stringBuilder.append("\n");

//					System.out.print(MessageFormat.format("spliter: {0} --> word: {1} --> count: {2} ", spliter, word,
//							tfresults.get(spliter).get(word)));
//					System.out.print("\n");
				}

				BufferedWriter writer = new BufferedWriter(new BufferedWriter(
						new FileWriter("D:\\Keyword extraction\\tfidf\\" + spliter + ".txt", false)));
				writer.write(stringBuilder.toString());
				writer.close();

			}
		} catch (Exception e) {

		}
		return;
	}

}
