package de.citytwin;

import java.util.List;

import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;
import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;

//TODO Javadoc! Rename without 'CityTwin'.
public class CityTwinTokenizer implements Tokenizer {

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
