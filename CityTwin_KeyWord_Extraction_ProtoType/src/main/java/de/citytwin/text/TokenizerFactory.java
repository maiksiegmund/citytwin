package de.citytwin.text;

import java.io.InputStream;

import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;
import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;

/**
 * This class implements {@link org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory}
 *
 * @author Maik, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class TokenizerFactory implements org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory {

    private TextProcessing textProcessing = null;
    private TokenPreProcess tokenPreProcess = null;

    public TokenizerFactory() {

    }

    public TokenizerFactory(TextProcessing textProcessing) {
        this.textProcessing = textProcessing;
    }

    @Override
    public Tokenizer create(InputStream toTokenize) {
        return create(toTokenize.toString());
    }

    @Override
    public Tokenizer create(String toTokenize) {
        return new de.citytwin.text.Tokenizer(this.textProcessing, toTokenize);
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
