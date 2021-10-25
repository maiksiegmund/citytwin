package de.citytwin.text;

import java.util.List;

import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;

/**
 * This class implements {@link org.deeplearning4j.text.tokenization.tokenizer.Tokenizer}
 *
 * @author Maik, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class Tokenizer implements org.deeplearning4j.text.tokenization.tokenizer.Tokenizer {

    private List<String> tokens = null;
    private int currentIndex = 0;

    public Tokenizer(TextProcessing textProcessing, String toTokenize) {
        this.tokens = textProcessing.tokenize2Term(toTokenize);
    }

    @Override
    public int countTokens() {
        return tokens.size();
    }

    @Override
    public List<String> getTokens() {
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
