package de.citytwin;

import java.io.IOException;
import java.io.InputStream;

import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;
import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;

//TODO Javadoc! Rename without 'CityTwin'.
public class CityTwinTokenizerFactory implements TokenizerFactory {

    private GermanTextProcessing textProcessing = null;
    private TokenPreProcess tokenPreProcess = null;

    public CityTwinTokenizerFactory() throws IOException {
        this.textProcessing = new GermanTextProcessing();
    }

    //// de.citytwin.Word2VecAnalyser.CityTwinTokenizerFactory
    public CityTwinTokenizerFactory(GermanTextProcessing textProcessing) {
        this.textProcessing = textProcessing;
    }

    @Override
    public Tokenizer create(InputStream toTokenize) {
        // replace with create(toTokenize.toString())?
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
