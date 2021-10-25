package de.citytwin.text;

import java.util.List;

/**
 * this class implements {@link org.deeplearning4j.text.sentenceiterator.SentenceIterator}
 *
 * @author Maik, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class SentenceIterator implements org.deeplearning4j.text.sentenceiterator.SentenceIterator {

    private List<String> sentences;
    private int currentIndex;
    private SentencePreProcessor sentencePreProcessor;

    public SentenceIterator(final List<String> sentences) {
        this.sentences = sentences;
        this.currentIndex = 0;
        this.sentencePreProcessor = new SentencePreProcessor();
    }

    @Override
    public void finish() {
        this.currentIndex = sentences.size();

    }

    @Override
    public SentencePreProcessor getPreProcessor() {
        return sentencePreProcessor;
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
    public void setPreProcessor(org.deeplearning4j.text.sentenceiterator.SentencePreProcessor preProcessor) {
        sentencePreProcessor = (SentencePreProcessor)preProcessor;

    }

}
