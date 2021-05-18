package de.citytwin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class DocumentConverter {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	/**
	 * This method convert a file to plain text. 
	 * used apache tika 
	 * @param file with text content 
	 * @return @see BodyContentHandler
	 */
	public BodyContentHandler documentToText(File file) {
		BodyContentHandler handler = new BodyContentHandler(Integer.MAX_VALUE);
		AutoDetectParser parser = new AutoDetectParser();
		Metadata metadata = new Metadata();

		try {

			FileInputStream fileInputStream = new FileInputStream(file);
			InputStream stream = fileInputStream;
			logger.info(MessageFormat.format("parse file: {0}", file.getAbsoluteFile()));
			parser.parse(stream, handler, metadata);

		} catch (SAXException exception) {
			logger.error(exception.getMessage());

		} catch (TikaException exception) {
			logger.error(exception.getMessage());

		} catch (IOException exception) {
			logger.error(exception.getMessage());

		} catch (Exception exception) {
			logger.error(exception.getMessage());

		}
		return handler;

	}

}
