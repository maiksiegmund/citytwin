package de.citytwin;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.microsoft.OfficeParserConfig;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class DocumentConverter implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private BodyContentHandler bodyContentHandler = null;
    private AutoDetectParser autoDetectParser = null;
    private Metadata metadata = null;
    private ParseContext parseContext = null;
    private File parsedFile = null;

    public DocumentConverter() {

    }

    @Override
    public void close() throws Exception {
        autoDetectParser = null;
        bodyContentHandler = null;
        metadata = null;
        parseContext = null;
    }

    /**
     * this method parse an list of json files and store the article texts. file content like <br>
     * {"id":"..." , "revid": "...", "url": "http://..." , "title": "..." , "text": "..."}
     *
     * @param jsonFile
     * @return new reference of {@code List<String>}
     * @throws IOException
     */
    public List<String> getArticleTexts(final List<File> jsonFiles) throws IOException {

        List<String> results = new ArrayList<String>();
        ObjectMapper mapper = new ObjectMapper();
        for (File jsonFile : jsonFiles) {
            logger.info(MessageFormat.format("parse file {0}.", jsonFile.getName()));
            JsonParser parser = mapper.createParser(jsonFile);
            JsonToken token = parser.nextToken();

            while (token != null) {
                // seeking text fieldname
                if ("text".equals(parser.getText())) {
                    // next token is text field value
                    token = parser.nextToken();
                    if (!parser.getText().isBlank()) {
                        results.add(parser.getText());

                    }
                }
                token = parser.nextToken();
            }
        }
        logger.info(MessageFormat.format("json file contains {0} atricles ", results.size()));
        return results;

    }

    /**
     * this method return BodyContentHandler of file
     *
     * @param file
     * @return new reference of {@code BodyContentHandler}
     * @throws SAXException
     * @throws TikaException
     * @throws IOException
     * @throws Exception
     */
    public BodyContentHandler getBodyContentHandler(final File file) throws SAXException, TikaException, IOException, Exception {
        if (parsedFile == null) {
            setTikaComponents(file);
            parsedFile = file;
        }
        if (!parsedFile.equals(file)) {
            setTikaComponents(file);
            parsedFile = file;
        }
        return bodyContentHandler;
    }

    /**
     * this method return title of file
     *
     * @param file
     * @return {@code String}
     * @throws SAXException
     * @throws TikaException
     * @throws IOException
     * @throws Exception
     */
    public String getDocumentTitle(final File file) throws SAXException, TikaException, IOException, Exception {
        if (parsedFile == null) {
            setTikaComponents(file);
            parsedFile = file;
        }
        if (!parsedFile.equals(file)) {
            setTikaComponents(file);
            parsedFile = file;
        }
        return metadata.get("title");

    }

    // TODO Fix Javadoc! Wrong place for this method? (consider moving or deleting)
    /**
     * this method deserialized alkis.json file (include as resource)
     *
     * @param C.
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    /**
     * this method deserialized data transfer objects (include as resource)
     *
     * @param <T> {@code ALKISDTO or OntologyDTO}
     * @param type {@code List<ALKISDTO>}
     * @param path
     * @return new reference of T
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public <T> T getDTOs(final TypeReference<T> type, String path) throws JsonParseException, JsonMappingException, IOException {

        T results = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        InputStream inputStream = new FileInputStream(path);
        results = mapper.readValue(inputStream, type);
        inputStream.close();
        return results;

    }

    /**
     * this method return metadata of file
     *
     * @param file
     * @return new reference of {@code Metadata}
     * @throws SAXException
     * @throws TikaException
     * @throws IOException
     * @throws Exception
     */
    public Metadata getMetaData(final File file) throws SAXException, TikaException, IOException, Exception {
        if (parsedFile == null) {
            setTikaComponents(file);
            parsedFile = file;
        }
        if (!parsedFile.equals(file)) {
            setTikaComponents(file);
            parsedFile = file;
        }
        String filename = metadata.get("name");
        if (filename == null) {
            metadata.add("name", file.getName());
        }
        return metadata;
    }

    /**
     * This method returns a configured OfficeParserConfig
     *
     * @return new reference of {@link OfficeParserConfig}
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
     * @return new reference of {@link OfficeParserConfig}
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

    /**
     * This method returns a configured parsercontext choose by file type
     *
     * @param file {@link File}
     * @return new reference of {@link ParseContext}
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
     * This method save bodyContentHandler to file
     *
     * @param bodyContentHandler
     * @param destination
     * @throwIOException
     */
    public void saveAsTextFile(final BodyContentHandler bodyContentHandler, final String destination) throws IOException {
        BufferedWriter writer = new BufferedWriter(new BufferedWriter(new FileWriter(destination, false)));
        writer.write(bodyContentHandler.toString());
        writer.close();
    }

    /**
     * This method convert a file to plain text. used apache tika
     *
     * @param file {@link File}
     * @return new reference of {@link BodyContentHandler}
     * @throws SAXException, TikaException, IOException, Exception
     */
    private BodyContentHandler setTikaComponents(final File file) throws SAXException, TikaException, IOException, Exception {
        this.bodyContentHandler = new BodyContentHandler(Integer.MAX_VALUE);
        this.autoDetectParser = new AutoDetectParser();
        this.metadata = new Metadata();
        this.parseContext = prepareParserContext(file);
        FileInputStream fileInputStream = new FileInputStream(file);
        InputStream stream = fileInputStream;
        logger.info(MessageFormat.format("parse file: {0}", file.getAbsoluteFile()));
        autoDetectParser.parse(stream, bodyContentHandler, metadata, parseContext);
        stream.close();
        return bodyContentHandler;
    }

}
