package de.citytwin;

import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;

public class CityTwinTokenPreProcess implements TokenPreProcess {

    @Override
    // input already tokenized
    public String preProcess(String token) {
        return token;
    }

}
