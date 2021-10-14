package de.citytwin;

import java.util.List;

import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;

// TODO Javadoc! Consider renaming without 'CityTwin'.
public class CityTwinSentenceIterator implements SentenceIterator {

    public List<String> sentences;
    public int currentIndex;
    public SentencePreProcessor preProcessor;

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
