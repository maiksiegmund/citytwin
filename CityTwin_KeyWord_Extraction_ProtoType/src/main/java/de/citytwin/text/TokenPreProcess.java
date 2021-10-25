package de.citytwin.text;

/**
 * This class implements {@link org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess}
 *
 * @author Maik, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class TokenPreProcess implements org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess {

    @Override
    // input already tokenized
    public String preProcess(String token) {
        return token;
    }

}
