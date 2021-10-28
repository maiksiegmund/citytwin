package de.citytwin.converter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.citytwin.text.TextProcessing;

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
import java.util.Properties;

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

/**
 * this class provides convert functions
 *
 * @author Maik Siegmund, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class DocumentConverter implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * this method return default properties
     *
     * @return new reference of {@code Properties}
     */
    public static Properties getDefaultProperties() {

        Properties properties = new Properties();
        properties.put("maxNewLines", 5);
        properties.put("cleaningPattern", "[^\\u2013\\u002D\\wäÄöÖüÜß,-/]");
        properties.put("minTermLenght", 2);
        properties.put("minTermCount", 5);
        properties.put("tableOfContendThershold", 50);
        return properties;
    }

    /**
     * this method deserialized json file
     *
     * @param <T>
     * @param type
     * @param path
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public static <T> T getObjects(final TypeReference<T> type, String path) throws JsonParseException, JsonMappingException, IOException {

        T results = null;
        try(InputStream inputStream = new FileInputStream(path);) {
            ObjectMapper mapper = new ObjectMapper();
            // mapper.registerSubtypes(new NamedType(ALKIS.class, "de.citytwin.catalog.ALKIS"));
            // mapper.registerSubtypes(new NamedType(Term.class, "de.citytwin.catalog.Term"));
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            results = mapper.readValue(inputStream, type);
            return results;

        }
    }

    private BodyContentHandler bodyContentHandler = null;
    private AutoDetectParser autoDetectParser = null;
    private Metadata metadata = null;

    private ParseContext parseContext = null;
    private File parsedFile = null;
    private List<List<String>> textCorpus = null;

    private Properties properties = null;

    private TextProcessing textProcessing = null;

    /**
     * Konstruktor.
     *
     * @param properties {@code DocumentConverter.getDefaultProperties()}
     * @param textProcessing
     * @throws IOException
     */
    public DocumentConverter(Properties properties, TextProcessing textProcessing) throws IOException {
        if (validateProperties(properties)) {
            this.properties = new Properties(properties.size());
            this.properties.putAll(properties);
            this.textProcessing = textProcessing;
        }
    }

    @Override
    public void close() throws Exception {
        autoDetectParser = null;
        bodyContentHandler = null;
        metadata = null;
        parseContext = null;
        textCorpus.clear();
        textCorpus = null;
    }

    /**
     * this method return BodyContentHandler of a file
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
     * this method tokenize bodyContentHandler in sentences and each term and remove footers, table of content
     *
     * @param bodyContentHandler {@code BodyContentHandler}
     * @return {@code List<List<String>>}
     * @throws IOException
     */
    public List<List<String>> getCleanedTextCorpus(BodyContentHandler bodyContentHandler) throws IOException {

        textCorpus = new ArrayList<List<String>>();
        Integer maxNewLines = (Integer)properties.get("maxNewLines");
        String cleaningPattern = (String)properties.get("cleaningPattern");
        Integer minTermLenght = (Integer)properties.get("minTermLenght");
        Integer minTermCount = (Integer)properties.get("minTermCount");
        Integer minTableOfContentThershold = (Integer)properties.get("minTableOfContentThershold");
        List<String> sentences = textProcessing.tokenize2Sencences(bodyContentHandler, maxNewLines);
        // tokenize sentence in each term
        for (String sentence : sentences) {
            textCorpus.add(textProcessing.try2CleanSentence(sentence, cleaningPattern, minTermLenght, minTermCount, minTableOfContentThershold));
        }
        return textCorpus;
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
     * this method returns text corpus of a bodyContentHandler.
     *
     * @param bodyContentHandler
     * @return {@code List<List<String>>} <br>
     *         Sentence_0{"Hello" , "my" , "name", "is" , "rumpelstilzchen"} <br>
     *         Sentence_1{"I" , "live" , "in", "dreamland" , "by" , "king" , "zorg"}
     * @throws IOException
     */
    public List<List<String>> getTextCorpus(BodyContentHandler bodyContentHandler) throws IOException {

        textCorpus = new ArrayList<List<String>>();
        Integer maxNewLines = (Integer)properties.get("maxNewLines");
        if (maxNewLines == null) {
            throw new IOException("set property --> maxNewLines as Integer");
        }
        List<String> sentences = textProcessing.tokenize2Sencences(bodyContentHandler, maxNewLines);
        for (String sentence : sentences) {
            textCorpus.add(textProcessing.tokenize2Term(sentence));
        }
        return textCorpus;
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
        try(BufferedWriter writer = new BufferedWriter(new BufferedWriter(new FileWriter(destination, false)))) {
            writer.write(bodyContentHandler.toString());
            writer.close();
        }
    }

    /**
     * This method convert a file to plain text. used apache tika
     *
     * @param file {@link File}
     * @return new reference of {@link BodyContentHandler}
     * @throws SAXException, TikaException, IOException, Exception
     */
    private BodyContentHandler setTikaComponents(final File file) throws SAXException, TikaException, IOException, Exception {
        FileInputStream fileInputStream = null;
        InputStream stream = null;
        try {
            this.bodyContentHandler = new BodyContentHandler(Integer.MAX_VALUE);
            this.autoDetectParser = new AutoDetectParser();
            this.metadata = new Metadata();
            this.parseContext = prepareParserContext(file);
            fileInputStream = new FileInputStream(file);
            stream = fileInputStream;
            logger.info(MessageFormat.format("parse file: {0}", file.getAbsoluteFile()));
            autoDetectParser.parse(stream, bodyContentHandler, metadata, parseContext);
            stream.close();
            fileInputStream.close();
            return bodyContentHandler;
        } finally {
            fileInputStream.close();
            stream.close();
        }

    }

    /**
     * this method validate passing properties
     *
     * @param properties
     * @return
     * @throws IOException
     */
    private Boolean validateProperties(Properties properties) throws IOException {

        Integer value = (Integer)properties.get("maxNewLines");
        if (value == null) {
            throw new IOException("set property --> maxNewLines as Integer");
        }
        String string = (String)properties.get("cleaningPattern");
        if (string == null) {
            throw new IOException("set property --> cleaningPattern as String");
        }
        value = (Integer)properties.get("minTermLenght");
        if (value == null) {
            throw new IOException("set property --> minTermLenght as Integer");
        }
        Integer minTermCount = (Integer)properties.get("minTermCount");
        if (minTermCount == null) {
            throw new IOException("set property --> minTermCount as Integer");
        }
        Integer minTableOfContentThershold = (Integer)properties.get("minTableOfContentThershold");
        if (minTableOfContentThershold == null) {
            throw new IOException("set property --> tableOfContendThershold as Integer");
        }
        return true;
    }

}
