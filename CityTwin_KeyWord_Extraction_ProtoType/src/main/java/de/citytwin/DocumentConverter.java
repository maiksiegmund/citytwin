package de.citytwin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.microsoft.OfficeParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import org.apache.tika.parser.pdf.*;

public class DocumentConverter {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * This method convert a file to plain text. used apache tika
     *
     * @param file {@link File}
     * @return {@link BodyContentHandler}
     */
    public BodyContentHandler documentToBodyContentHandler(File file) {

        BodyContentHandler handler = new BodyContentHandler(Integer.MAX_VALUE);
        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();

        try {
            ParseContext parseContext = prepareParserContext(file);
            FileInputStream fileInputStream = new FileInputStream(file);
            InputStream stream = fileInputStream;
            logger.info(MessageFormat.format("parse file: {0}", file.getAbsoluteFile()));
            parser.parse(stream, handler, metadata, parseContext);

        } catch (SAXException exception) {
            logger.error(exception.getMessage(), exception);

        } catch (TikaException exception) {
            logger.error(exception.getMessage(), exception);

        } catch (IOException exception) {
            logger.error(exception.getMessage(), exception);

        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);

        }

        return handler;

    }

    /**
     * This method save bodyContentHandler to txt file
     *
     * @param bodyContentHandler
     * @param destination
     * @return {@link ParseContext}
     */
    public void saveAsTextFile(final BodyContentHandler bodyContentHandler, final String destination) {

        try {
            BufferedWriter writer = new BufferedWriter(new BufferedWriter(new FileWriter(destination, false)));
            writer.write(bodyContentHandler.toString());
            writer.close();
        } catch (IOException exception) {
            logger.error(exception.getMessage());
        }

    }

    /**
     * This method returns a configured parsercontext by file type
     *
     * @param file {@link File}
     * @return {@link ParseContext}
     */
    private ParseContext prepareParserContext(File file) {

        String fileType = file.getName();
        int indexOfDot = fileType.lastIndexOf(".");
        fileType = fileType.substring(indexOfDot);
        ParseContext parseContext = new ParseContext();
        if (fileType.contains("xls")) {
            parseContext.set(OfficeParserConfig.class, getOfficeParserConfig());
        }
        if (fileType.contains("doc")) {
            parseContext.set(OfficeParserConfig.class, getOfficeParserConfig());
        }
        if (fileType.contains("ppt")) {
            parseContext.set(OfficeParserConfig.class, getOfficeParserConfig());
        }
        if (fileType.contains("pdf")) {
            parseContext.set(PDFParserConfig.class, getPfdParserConfig());
        }

        return parseContext;

    }

    /**
     * This method returns a configured OfficeParserConfig
     *
     * @return {@link OfficeParserConfig}
     */
    private OfficeParserConfig getOfficeParserConfig() {

        OfficeParserConfig config = new OfficeParserConfig();
        config.setConcatenatePhoneticRuns(true);
        config.setDateOverrideFormat("dd.mm.yyyy");
        config.setExtractAllAlternativesFromMSG(false);
        config.setIncludeDeletedContent(false);
        config.setIncludeHeadersAndFooters(false);
        config.setIncludeMissingRows(false);
        config.setIncludeMoveFromContent(false);
        config.setIncludeShapeBasedContent(false);
        config.setIncludeSlideMasterContent(false);
        config.setIncludeSlideNotes(false);
        config.setUseSAXDocxExtractor(true);
        config.setUseSAXPptxExtractor(true);

        return config;

    }

    /**
     * This method returns a configured OfficeParserConfig
     *
     * @return {@link OfficeParserConfig}
     */
    private PDFParserConfig getPfdParserConfig() {

        PDFParserConfig config = new PDFParserConfig();
        // AccessChecker accessChecker = new AccessChecker();
        // config.setAccessChecker(null);
        // config.setAverageCharTolerance(null); // default 0.3f;
        config.setCatchIntermediateIOExceptions(true);
        config.setDetectAngles(false);
        config.setEnableAutoSpace(true);
        config.setExtractAcroFormContent(false);
        config.setExtractActions(false);
        config.setExtractAnnotationText(false);
        config.setExtractBookmarksText(false);
        config.setExtractFontNames(false);
        config.setExtractMarkedContent(false);
        config.setExtractUniqueInlineImagesOnly(true);
        config.setIfXFAExtractOnlyXFA(false);
        config.setSetKCMS(false);
        config.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.NO_OCR);
        config.setSortByPosition(true);
        config.setSuppressDuplicateOverlappingText(true);

        return config;

    }

}
