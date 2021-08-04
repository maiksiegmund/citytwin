package de.citytwin;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;
import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;
import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Word2VecAnalyser {

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
        public void finish() {
            this.currentIndex = sentences.size();

        }

        @Override
        public SentencePreProcessor getPreProcessor() {
            return preProcessor;
        }

        @Override
        public boolean hasNext() {
            return (currentIndex < this.sentences.size());
        }

        @Override
        public String nextSentence() {

            return this.sentences.get(currentIndex++);
        }

        @Override
        public void reset() {
            this.currentIndex = 0;

        }

        @Override
        public void setPreProcessor(SentencePreProcessor preProcessor) {
            this.preProcessor = preProcessor;

        }

    }

    public class CityTwinSentencePreProcessor implements SentencePreProcessor {

        // sentence already prepared
        @Override
        public String preProcess(String sentence) {
            // TODO Auto-generated method stub
            return sentence;
        }

    }

    public static class CityTwinTokenizer implements Tokenizer {

        private List<String> tokens = null;
        private int currentIndex = 0;

        public CityTwinTokenizer(GermanTextProcessing textProcessing, String toTokenize) {
            this.tokens = textProcessing.tokenizeOpenNLP(toTokenize);

        }

        @Override
        public int countTokens() {
            return tokens.size();
        }

        @Override
        public List<String> getTokens() {
            // TODO Auto-generated method stub
            return tokens;
        }

        @Override
        public boolean hasMoreTokens() {
            return (currentIndex < tokens.size());
        }

        @Override
        public String nextToken() {
            return (currentIndex < tokens.size()) ? tokens.get(currentIndex++) : "";
        }

        @Override
        public void setTokenPreProcessor(TokenPreProcess tokenPreProcessor) {
            // TODO Auto-generated method stub

        }

    }

    public class CityTwinTokenizerFactory implements TokenizerFactory {

        private GermanTextProcessing textProcessing = null;
        private TokenPreProcess tokenPreProcess = null;

        CityTwinTokenizerFactory(GermanTextProcessing textProcessing) {
            this.textProcessing = textProcessing;
        }

        @Override
        public Tokenizer create(InputStream toTokenize) {
            return new CityTwinTokenizer(this.textProcessing, toTokenize.toString());
        }

        @Override
        public Tokenizer create(String toTokenize) {
            return new CityTwinTokenizer(this.textProcessing, toTokenize);
        }

        @Override
        public TokenPreProcess getTokenPreProcessor() {
            // TODO Auto-generated method stub
            return tokenPreProcess;
        }

        @Override
        public void setTokenPreProcessor(TokenPreProcess preProcessor) {
            tokenPreProcess = preProcessor;
        }

    }

    public class CityTwinTokenPreProcess implements TokenPreProcess {

        @Override
        // input already tokenized
        public String preProcess(String token) {
            return token;
        }

    }

    /** current version information */
    private static final String VERSION = "$Revision: 1.00 $";
    /** logger */
    private static final transient Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private GermanTextProcessing textProcessing = null;

    private List<String> articles = new ArrayList<String>();

    private List<String> stopWords = new ArrayList<String>();

    private List<String> sentences = new ArrayList<String>();

    private Word2Vec word2vec = null;

    public Word2VecAnalyser() throws IOException {
        initialize();
    }

    public HashMap<String, Integer> getDefaultParameters() {

        HashMap<String, Integer> parameters = new HashMap<String, Integer>();
        parameters.put("batchSize", 100);
        parameters.put("minWordFrequency", 5);
        parameters.put("iterations", 1);
        parameters.put("layerSize", 100);
        parameters.put("seed", 42);
        parameters.put("windowSize", 5);
        return parameters;
    }

    /**
     * this method initialize nlp components
     *
     * @throws IOException
     */
    private void initialize() throws IOException {

        textProcessing = new GermanTextProcessing();

    }

    private void setStopWords() {
        stopWords.addAll(GermanTextProcessing.getStopwords());
    }

    public List<String> similarWordsInVocabTo(String term, double accurany) throws IOException {
        if (word2vec != null) {
            return word2vec.similarWordsInVocabTo(term, accurany);
        }
        throw new IOException("no intern model set. call trainModel(...) or new Word2VecAnalyser().withModel(...)");
    }

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

    /**
     * this method train call writeModel to save current state
     *
     * @param sentences
     */
    public void trainModel(List<String> sentences, @Nonnull Map<String, Integer> paramters) {
        SentenceIterator sentenceIterator = new CityTwinSentenceIterator(sentences);
        TokenizerFactory tokenizerFactory = new CityTwinTokenizerFactory(this.textProcessing);
        tokenizerFactory.setTokenPreProcessor(new CityTwinTokenPreProcess());

        int batchSize = (paramters.get("batchSize") != null) ? paramters.get("batchSize").intValue() : 100;
        int minWordFrequency = (paramters.get("minWordFrequency") != null) ? paramters.get("minWordFrequency").intValue() : 5;
        int iteration = (paramters.get("iterations") != null) ? paramters.get("iterations").intValue() : 1;
        int layerSize = (paramters.get("layerSize") != null) ? paramters.get("layerSize").intValue() : 100;
        int seed = (paramters.get("seed") != null) ? paramters.get("seed").intValue() : 42;
        int windowSize = (paramters.get("windowSize") != null) ? paramters.get("windowSize").intValue() : 5;

        if (word2vec == null) {
            word2vec = new Word2Vec.Builder()
                    .batchSize(batchSize)
                    .minWordFrequency(minWordFrequency)
                    .iterations(iteration)
                    .layerSize(layerSize)
                    .stopWords(stopWords)
                    .seed(seed)
                    .windowSize(windowSize)
                    .iterate(sentenceIterator)
                    .tokenizerFactory(tokenizerFactory)
                    .build();
        } else {
            word2vec.setTokenizerFactory(tokenizerFactory);
            word2vec.setSentenceIterator(sentenceIterator);
        }
        word2vec.fit();
        word2vec.
        logger.info("model trained");

    }

    /**
     * simple fluent api
     *
     * @param path
     * @return
     */
    public Word2VecAnalyser withModel(String source) {

        word2vec = WordVectorSerializer.readWord2VecModel(source);
        return this;
    }

    public void writeModel(String destination) {
        if (word2vec != null) {
            WordVectorSerializer.writeWord2VecModel(word2vec, destination);
        }
    }

}
