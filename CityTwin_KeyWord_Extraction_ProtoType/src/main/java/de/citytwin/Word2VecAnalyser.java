package de.citytwin;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;
import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;
import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
// import org.apache.spark.sql.Dataset;
// import org.apache.spark.sql.Row;
// import org.apache.spark.sql.RowFactory;
// import org.apache.spark.sql.SparkSession;
// import org.apache.spark.sql.types.ArrayType;
// import org.apache.spark.sql.types.DataTypes;
// import org.apache.spark.sql.types.Metadata;
// import org.apache.spark.sql.types.StructField;
// import org.apache.spark.sql.types.StructType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Word2VecAnalyser {

    public class CityTwinSentencePreProcessor implements SentencePreProcessor {

        // sentence already prepared
        @Override
        public String preProcess(String sentence) {
            // TODO Auto-generated method stub
            return sentence;
        }

    }

    public class CityTwinTokenizerFactory implements TokenizerFactory {

        private GermanTextProcessing textProcessing = null;
        private TokenPreProcess tokenPreProcess = null;

        CityTwinTokenizerFactory(GermanTextProcessing textProcessing) {
            this.textProcessing = textProcessing;
        }

        @Override
        public Tokenizer create(String toTokenize) {
            return new CityTwinTokenizer(this.textProcessing, toTokenize);
        }

        @Override
        public Tokenizer create(InputStream toTokenize) {
            return new CityTwinTokenizer(this.textProcessing, toTokenize.toString());
        }

        @Override
        public void setTokenPreProcessor(TokenPreProcess preProcessor) {
            tokenPreProcess = preProcessor;
        }

        @Override
        public TokenPreProcess getTokenPreProcessor() {
            // TODO Auto-generated method stub
            return tokenPreProcess;
        }

    }

    public class CityTwinTokenPreProcess implements TokenPreProcess {

        @Override
        // input already tokenized
        public String preProcess(String token) {
            return token;
        }

    }

    public class CityTwinSentenceIterator implements SentenceIterator {

        private List<String> sentences = null;
        private int currentIndex = 0;
        private SentencePreProcessor preProcessor = null;

        public CityTwinSentenceIterator(List<String> sentences) {
            this.sentences = sentences;
            this.currentIndex = 0;
            this.preProcessor = new CityTwinSentencePreProcessor();
        }

        @Override
        public String nextSentence() {

            return this.sentences.get(currentIndex++);
        }

        @Override
        public boolean hasNext() {
            return (currentIndex < this.sentences.size());
        }

        @Override
        public void reset() {
            this.currentIndex = 0;

        }

        @Override
        public void finish() {
            this.currentIndex = sentences.size();

        }

        @Override
        public SentencePreProcessor getPreProcessor() {
            return preProcessor;
        }

        @Override
        public void setPreProcessor(SentencePreProcessor preProcessor) {
            this.preProcessor = preProcessor;

        }

    }

    public static class CityTwinTokenizer implements Tokenizer {

        private List<String> tokens = null;
        private int currentIndex = 0;

        public CityTwinTokenizer(GermanTextProcessing textProcessing, String toTokenize) {
            this.tokens = textProcessing.tokenizeOpenNLP(toTokenize);

        }

        @Override
        public boolean hasMoreTokens() {
            return (currentIndex < tokens.size());
        }

        @Override
        public int countTokens() {
            return tokens.size();
        }

        @Override
        public String nextToken() {
            return (currentIndex < tokens.size()) ? tokens.get(currentIndex++) : "";
        }

        @Override
        public List<String> getTokens() {
            // TODO Auto-generated method stub
            return tokens;
        }

        @Override
        public void setTokenPreProcessor(TokenPreProcess tokenPreProcessor) {
            // TODO Auto-generated method stub

        }

    }

    /** current version information */
    private static final String VERSION = "$Revision: 1.00 $";
    /** logger */
    private static final transient Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private GermanTextProcessing textProcessing = null;

    /**
     * R&uuml;ckgabe der Klasseninformation.
     * <p>
     * Gibt den Klassennamen und die CVS Revisionsnummer zur&uuml;ck.
     * <p>
     *
     * @return Klasseninformation
     */
    @Override
    public String toString() {
        return this.getClass().getName() + " " + VERSION;
    }

    public Word2VecAnalyser() throws IOException {
        initialize();
    }

    /**
     * this method parse an json file and store the article texts. file content like <br>
     * {"id":"..." , "revid": "...", "url": "http://..." , "title": "..." , "text": "..."}
     *
     * @param jsonFile
     * @return new reference of {@code List<String>}
     * @throws IOException
     */
    private List<String> getArticleTexts(File jsonFile) throws IOException {

        List<String> results = new ArrayList<String>();
        ObjectMapper mapper = new ObjectMapper();
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
        logger.info(MessageFormat.format("json file contains {0} atricles ", results.size()));
        return results;

    }

    /**
     * this method initialize nlp components
     *
     * @throws IOException
     */
    private void initialize() throws IOException {

        textProcessing = new GermanTextProcessing();
        // createDataset();
    }

    /**
     * this method transform a file with json content in a List of sentences, each sentences contains a list of terms.
     *
     * @param jsonFile
     * @return new reference of {@code List<List<String>>}
     * @throws IOException
     */
    public List<List<String>> transforJsonText(File jsonFile) throws IOException {
        List<List<String>> results = new ArrayList<List<String>>();
        List<String> articles = getArticleTexts(jsonFile);
        List<String> sentences = textProcessing.tokenizeArticlesToSencences(articles);
        for (String sentence : sentences) {
            results.add(textProcessing.tokenizeOpenNLP(sentence));
        }
        return results;

    }

    // private Dataset<Row> createDataset() {
    //
    // SparkSession spark = SparkSession
    // .builder()
    // .appName("JavaWord2VecExample")
    // .getOrCreate();
    //
    // List<Row> data = Arrays.asList(
    // RowFactory.create(Arrays.asList("Hi I heard about Spark".split(" "))),
    // RowFactory.create(Arrays.asList("I wish Java could use case classes".split(" "))),
    // RowFactory.create(Arrays.asList("Logistic regression models are neat".split(" "))));
    // StructType schema = new StructType(new StructField[] {
    // new StructField("text", new ArrayType(DataTypes.StringType, true), false, Metadata.empty())
    // });
    // return spark.createDataFrame(data, schema);
    //
    // }

    public void word2vec(File jsonFile) throws IOException {

        List<String> stopWords = new ArrayList<String>();
        stopWords.addAll(textProcessing.getStopwords());
        List<String> sentences = this.getArticleTexts(jsonFile);

        SentenceIterator iter = new CityTwinSentenceIterator(sentences);
        // Split on white spaces in the line to get words
        TokenizerFactory t = new CityTwinTokenizerFactory(this.textProcessing);
        t.setTokenPreProcessor(new CityTwinTokenPreProcess());

        Word2Vec vec = new Word2Vec.Builder()
                .minWordFrequency(5)
                .iterations(1)
                .layerSize(100)
                .stopWords(stopWords)
                .seed(42)
                .windowSize(5)
                .iterate(iter)
                .tokenizerFactory(t)
                .build();

    }
}
