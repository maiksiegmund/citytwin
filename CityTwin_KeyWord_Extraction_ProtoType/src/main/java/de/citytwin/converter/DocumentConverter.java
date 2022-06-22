package de.citytwin.converter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.citytwin.config.ApplicationConfiguration;
import de.citytwin.text.TextProcessing;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

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
 */
public class DocumentConverter implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * this method return default properties
     *
     * @return new reference of {@code Properties}
     */
    public static Properties getDefaultProperties() {

        Properties properties = new Properties();
        properties.setProperty(ApplicationConfiguration.MAX_NEW_LINES, "5");
        properties.setProperty(ApplicationConfiguration.CLEANING_REGEX, "[^\\u2013\\u002D\\wäÄöÖüÜß,-/]");
        properties.setProperty(ApplicationConfiguration.MIN_TERM_LENGTH, "2");
        properties.setProperty(ApplicationConfiguration.MIN_TERM_COUNT, "5");
        properties.setProperty(ApplicationConfiguration.MIN_TABLE_OF_CONTENT, "80");
        return properties;
    }

    /**
     * this method deserialized json file
     *
     * @param <T>
     * @param type
     * @param path
     * @return T
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public static <T> T getObjects(final TypeReference<T> type, String path) throws JsonParseException, JsonMappingException, IOException {

        T results = null;
        try(InputStream inputStream = new FileInputStream(path);) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            results = mapper.readValue(inputStream, type);
            return results;
        }
    }

    private BodyContentHandler bodyContentHandler = null;
    private AutoDetectParser autoDetectParser = null;
    private Metadata metadata = null;
    private ParseContext parseContext = null;
    private List<List<String>> textCorpus = null;
    private TextProcessing textProcessing = null;
    private String fileName = null;
    private ByteArrayInputStream bufferedInputStream = null;
    private Integer maxNewLines = null;
    private String cleaningPattern = null;
    private Integer minTermLength = null;
    private Integer minTermCount = null;
    private Integer minTableOfContent = null;

    /**
     * constructor.
     *
     * @param properties {@code DocumentConverter.getDefaultProperties()}
     * @param textProcessing
     * @throws IOException
     */
    public DocumentConverter(Properties properties, TextProcessing textProcessing) throws IOException {
        if (validateProperties(properties)) {
            this.textProcessing = textProcessing;
        }
    }

    @Override
    public void close() throws Exception {
        autoDetectParser = null;
        bodyContentHandler = null;
        metadata = null;
        parseContext = null;
        if (textCorpus != null) {
            textCorpus.clear();
        }
        textCorpus = null;
    }

    /**
     * this method return BodyContentHandler of a FileInputStream
     *
     * @param byteArrayInputStream
     * @param fileName
     * @return
     * @throws SAXException
     * @throws TikaException
     * @throws IOException
     * @throws Exception
     */
    public BodyContentHandler getBodyContentHandler(
            @Nonnull final ByteArrayInputStream byteArrayInputStream, @Nonnull String fileName)
            throws SAXException, TikaException, IOException, Exception {

        if (!byteArrayInputStream.equals(this.bufferedInputStream)) {
            this.bufferedInputStream = byteArrayInputStream;
            this.fileName = fileName;
            setTikaComponents(this.bufferedInputStream, fileName);
        }
        return bodyContentHandler;
    }

    /**
     * this method tokenize bodyContentHandler in sentences and each term and remove footers, table of content
     *
     * @param bodyContentHandler
     * @param everySingleSentence
     * @return
     * @throws IOException
     */
    public List<List<String>> getCleanedTextCorpus(BodyContentHandler bodyContentHandler, boolean everySingleSentence) throws IOException {
        return (everySingleSentence) ? getCleanedTextCorpusBySentences(bodyContentHandler) : getCleanedTextCorpusOnHoleCorpus(bodyContentHandler);
    }

    /**
     * this method tokenize bodyContentHandler in sentences and each term and remove footers, table of content
     *
     * @param bodyContentHandler {@code BodyContentHandler}
     * @return {@code List<List<String>>}
     * @throws IOException
     */
    private List<List<String>> getCleanedTextCorpusBySentences(BodyContentHandler bodyContentHandler) throws IOException {

        textCorpus = new ArrayList<List<String>>();
        List<String> sentences = textProcessing.tokenize2Sencences(bodyContentHandler, maxNewLines);
        // tokenize sentence in each term
        for (String sentence : sentences) {
            textCorpus.add(textProcessing.try2CleanSentence(sentence, cleaningPattern, minTermLength, minTermCount, minTableOfContent));
        }
        LOGGER.info("text corpus cleaned");
        return textCorpus;
    }

    /**
     * this method tokenize bodyContentHandler in sentences and each term term and remove footers, table of content (on hole text corpus)
     *
     * @param bodyContentHandler {@code BodyContentHandler}
     * @return {@code List<List<String>>}
     * @throws IOException
     */
    private List<List<String>> getCleanedTextCorpusOnHoleCorpus(BodyContentHandler bodyContentHandler) throws IOException {

        textCorpus = new ArrayList<List<String>>();

        List<String> sentences = textProcessing.getPreProcessedTextCorpus(bodyContentHandler.toString());
        int count = 0;

        for (String sentence : sentences) {
            List<String> temp = textProcessing.tokenize2Term(sentence);
            textCorpus.add(temp);
            count += temp.size();
        }

        LOGGER.info(MessageFormat.format("text corpus cleaned, {0} terms remain", count));
        return textCorpus;
    }

    /**
     * this method return parsed document title of fileInputStream
     *
     * @param byteArrayInputStream
     * @param fileName
     * @return
     * @throws SAXException
     * @throws TikaException
     * @throws IOException
     * @throws Exception
     */
    public String getDocumentTitle(
            @Nonnull final ByteArrayInputStream byteArrayInputStream, @Nonnull String fileName)
            throws SAXException, TikaException, IOException, Exception {
        if (!this.bufferedInputStream.equals(byteArrayInputStream)) {
            this.bufferedInputStream = byteArrayInputStream;
            this.fileName = fileName;
            setTikaComponents(byteArrayInputStream, fileName);
        }
        return metadata.get("title");

    }

    /**
     * this method return metadata of file
     *
     * @param byteArrayInputStream
     * @param fileName
     * @return
     * @throws SAXException
     * @throws TikaException
     * @throws IOException
     * @throws Exception
     */
    public Metadata getMetaData(final ByteArrayInputStream byteArrayInputStream, String fileName) throws SAXException, TikaException, IOException, Exception {
        if (!this.bufferedInputStream.equals(byteArrayInputStream)) {
            this.bufferedInputStream = byteArrayInputStream;
            this.fileName = fileName;
            setTikaComponents(byteArrayInputStream, fileName);
        }
        metadata.add("name", (metadata.get("name") == null) ? metadata.get("name") : fileName);
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
        config.setExtractUniqueInlineImagesOnly(false);
        config.setIfXFAExtractOnlyXFA(false);
        config.setSetKCMS(false);
        config.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.NO_OCR);
        config.setSortByPosition(false);
        config.setSuppressDuplicateOverlappingText(false);

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
    private ParseContext prepareParserContext(String fileName) {
        int indexOfDot = fileName.lastIndexOf(".");
        if (indexOfDot == -1) {
            return new ParseContext();
        }
        String fileType = "";
        fileType = fileName.substring(indexOfDot);
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
     * this method convert a fileInputStream to BodyContentHandler includes text
     *
     * @param byteArrayInputStream
     * @param fileName
     * @return
     * @throws SAXException
     * @throws TikaException
     * @throws IOException
     * @throws Exception
     */
    private BodyContentHandler setTikaComponents(final ByteArrayInputStream byteArrayInputStream, String fileName)
            throws SAXException, TikaException, IOException, Exception {

        this.bodyContentHandler = new BodyContentHandler(Integer.MAX_VALUE);
        this.autoDetectParser = new AutoDetectParser();
        this.metadata = new Metadata();
        this.parseContext = prepareParserContext(fileName);
        autoDetectParser.parse(this.bufferedInputStream, bodyContentHandler, metadata, parseContext);
        LOGGER.info(MessageFormat.format("file parsed: {0}", fileName));
        return bodyContentHandler;

    }

    /**
     * this method validate passing properties and set them
     *
     * @param properties
     * @return
     * @throws IllegalArgumentException
     */
    public Boolean validateProperties(Properties properties) throws IllegalArgumentException {

        String property = properties.getProperty(ApplicationConfiguration.MAX_NEW_LINES);
        if (property == null) {
            throw new IllegalArgumentException("set property --> " + "ApplicationConfiguration.MAX_NEW_LINES");
        }
        maxNewLines = Integer.parseInt(property);
        cleaningPattern = properties.getProperty(ApplicationConfiguration.CLEANING_REGEX);
        if (cleaningPattern == null) {
            throw new IllegalArgumentException("set property --> " + "ApplicationConfiguration.CLEANING_REGEX");
        }
        property = properties.getProperty(ApplicationConfiguration.MIN_TERM_LENGTH);
        if (property == null) {
            throw new IllegalArgumentException("set property --> " + "ApplicationConfiguration.MIN_TERM_LENGTH");
        }
        minTermLength = Integer.parseInt(property);

        property = properties.getProperty(ApplicationConfiguration.MIN_TERM_COUNT);
        if (property == null) {
            throw new IllegalArgumentException("set property --> " + "ApplicationConfiguration.MIN_TERM_COUNT");
        }
        minTermCount = Integer.parseInt(property);

        property = properties.getProperty(ApplicationConfiguration.MIN_TABLE_OF_CONTENT);
        if (property == null) {
            throw new IllegalArgumentException("set property --> " + "ApplicationConfiguration.MIN_TABLE_OF_CONTENT");
        }
        minTableOfContent = Integer.parseInt(property);

        return true;
    }

    public TextProcessing getTextProcessing() {
        return this.textProcessing;
    }

    public HashMap<String, List<String>> getTextSections(BodyContentHandler bodyContentHandler, List<String> seekingWords) throws IOException {
        HashMap<String, List<String>> results = new HashMap<String, List<String>>();
        List<List<String>> textCorpus = getTextCorpus(bodyContentHandler);
        for (List<String> sentence : textCorpus) {
            List<String> textSection = null;
            String concatSentence = textProcessing.concat(sentence);
            for (String word : seekingWords) {
                if (concatSentence.contains(word)) {
                    if (!results.containsKey(word)) {
                        textSection = new ArrayList<String>();
                        textSection.add(concatSentence);
                        results.put(word, textSection);
                    } else {
                        textSection = results.get(word);
                        if (textSection.contains(word)) {
                            continue;
                        }
                        textSection.add(concatSentence);
                        results.put(word, textSection);
                    }
                }
            }
        }
        return results;
    }

}
