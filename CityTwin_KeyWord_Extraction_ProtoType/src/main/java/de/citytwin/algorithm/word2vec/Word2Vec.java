package de.citytwin.algorithm.word2vec;

import de.citytwin.text.SentenceIterator;
import de.citytwin.text.TextProcessing;
import de.citytwin.text.TokenPreProcess;
import de.citytwin.text.TokenizerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * wrapper class of {@link org.deeplearning4j.models.word2vec.Word2Vec}
 *
 * @author Maik, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class Word2Vec implements AutoCloseable {

    /** current version information */
    private static final String VERSION = "$Revision: 1.00 $";
    /** logger */
    private static final transient Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * this method return default properties
     *
     * @return
     */
    public static Properties getDefaultProperties() {

        Properties properties = new Properties();
        properties.put("path.2.word2vec.file", "..\\word2vec.bin");
        return properties;

    }

    /**
     * this method return default training parameters
     *
     * @return
     */
    public static HashMap<String, Integer> getDefaultTrainParameters() {

        HashMap<String, Integer> parameters = new HashMap<String, Integer>();
        parameters.put("batchSize", 100);
        parameters.put("epochs", 1);
        parameters.put("minWordFrequency", 5);
        parameters.put("iterations", 1);
        parameters.put("layerSize", 100);
        parameters.put("seed", 42);
        parameters.put("windowSize", 5);
        parameters.put("workers", 4);
        return parameters;
    }

    private org.deeplearning4j.models.word2vec.Word2Vec word2vec = null;

    /**
     * Konstruktor.
     *
     * @param properties = {@code Word2Vec.getDefaultProperties()}
     * @throws IOException
     */
    public Word2Vec(Properties properties) throws IOException {
        if (validateProperties(properties)) {
            String path2File = (String)properties.get("path.2.word2vec.file");
            File file = new File(path2File);
            if (file.exists()) {
                word2vec = WordVectorSerializer.readWord2VecModel(path2File);
            }
        }
    }

    /**
     * this method is wrapper for {@link org.deeplearning4j.models.word2vec.Word2Vec#accuracy(List)}
     *
     * @param questions
     * @return
     * @throws IOException
     */
    public Map<String, Double> accuracy(List<String> questions) throws IOException {
        if (word2vec != null) {
            return word2vec.accuracy(questions);
        }
        throw new IOException("no model set");

    }

    @Override
    public void close() throws Exception {
        word2vec = null;

    }

    /**
     * this method save a model on filesystem
     *
     * @param destination
     * @throws IOException
     */
    public void saveModel(String destination) throws IOException {
        if (word2vec != null) {
            WordVectorSerializer.writeWord2VecModel(word2vec, destination);
            return;
        }
        throw new IOException("no model set");
    }

    /**
     * this method is wrapper for {@link org.deeplearning4j.models.word2vec.Word2Vec#similarity(String, String)}
     *
     * @param left
     * @param right
     * @return a normalized similarity (cosine similarity)
     * @throws IOException
     */
    public double similarity(String left, String right) throws IOException {
        if (word2vec != null) {
            return word2vec.similarity(left, right);
        }
        throw new IOException("no model set");
    }

    /**
     * this method is wrapper for {@link org.deeplearning4j.models.word2vec.Word2Vec#similarWordsInVocabTo(String, String)}
     *
     * @param term
     * @param accurany
     * @return
     * @throws IOException
     */
    public List<String> similarWordsInVocabTo(String term, double accurany) throws IOException {
        if (word2vec != null) {
            return word2vec.similarWordsInVocabTo(term, accurany);
        }
        throw new IOException("no model set");
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
     * this method train a model of a given text corpus
     *
     * @param sentences
     * @param paramters
     * @param textProcessing
     */
    public void trainModel(List<String> sentences, @Nonnull Map<String, Integer> paramters, TextProcessing textProcessing) {
        SentenceIterator sentenceIterator = new SentenceIterator(sentences);
        TokenizerFactory tokenizerFactory = new TokenizerFactory(textProcessing);
        tokenizerFactory.setTokenPreProcessor(new TokenPreProcess());

        int batchSize = (paramters.get("batchSize") != null) ? paramters.get("batchSize").intValue() : 100;
        int epochs = (paramters.get("epochs") != null) ? paramters.get("epochs").intValue() : 1;
        int minWordFrequency = (paramters.get("minWordFrequency") != null) ? paramters.get("minWordFrequency").intValue() : 5;
        int iteration = (paramters.get("iterations") != null) ? paramters.get("iterations").intValue() : 1;
        int layerSize = (paramters.get("layerSize") != null) ? paramters.get("layerSize").intValue() : 100;
        int seed = (paramters.get("seed") != null) ? paramters.get("seed").intValue() : 42;
        int windowSize = (paramters.get("windowSize") != null) ? paramters.get("windowSize").intValue() : 5;
        int workers = (paramters.get("workers") != null) ? paramters.get("workers").intValue() : 4;

        if (word2vec == null) {
            word2vec = new org.deeplearning4j.models.word2vec.Word2Vec.Builder()
                    .batchSize(batchSize)
                    .epochs(epochs)
                    .minWordFrequency(minWordFrequency)
                    .iterations(iteration)
                    .layerSize(layerSize)
                    .stopWords(textProcessing.getStopwords())
                    .seed(seed)
                    .windowSize(windowSize)
                    .workers(workers)
                    .iterate(sentenceIterator)
                    .tokenizerFactory(tokenizerFactory)
                    .build();
        } else {
            word2vec.setTokenizerFactory(tokenizerFactory);
            word2vec.setSentenceIterator(sentenceIterator);
        }
        word2vec.fit();
        logger.info("model trained");

    }

    /**
     * this method validate passing properties
     *
     * @param properties
     * @return
     * @throws IOException
     */
    private Boolean validateProperties(Properties properties) throws IOException {

        String property = (String)properties.get("path.2.word2vec.file");
        if (property == null) {
            throw new IOException("set property --> path.2.word2vec.file as String");
        }
        return true;

    }

    /**
     * this method is wrapper for {@link org.deeplearning4j.models.word2vec.Word2Vec#wordsNearest(Collection, Collection, int)} <br>
     * example: king - queen + woman = man
     *
     * @param plus {@code = Arrays.asList("king", "woman")}
     * @param minus {@code = Arrays.asList("queen")}
     * @param count {@code = 10}
     * @return new reference of List<String> with the result
     * @throws IOException
     */
    public List<String> wordNearest(Collection<String> plus, Collection<String> minus, int count) throws IOException {
        if (word2vec != null) {
            return new ArrayList<String>(word2vec.wordsNearest(plus, minus, count));
        }
        throw new IOException("no model set");
    }

    /**
     * this method is wrapper for {@link org.deeplearning4j.models.word2vec.Word2Vec#wordsNearest(String, int)}
     *
     * @param word
     * @param count
     * @return
     * @throws IOException
     */
    public List<String> wordsNearest(String word, int count) throws IOException {
        if (word2vec != null) {
            return new ArrayList<String>(word2vec.wordsNearest(word, count));
        }
        throw new IOException("no model set or trained");

    }

}
