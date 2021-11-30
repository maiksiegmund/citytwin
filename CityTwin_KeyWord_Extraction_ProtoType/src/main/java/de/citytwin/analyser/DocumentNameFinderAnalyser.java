package de.citytwin.analyser;

import de.citytwin.converter.DocumentConverter;
import de.citytwin.namedentities.NamedEntitiesExtractor;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.apache.tika.sax.BodyContentHandler;

/**
 * * this class is a named entities extractor <br>
 *
 * @author Maik Siegmund, FH Erfurt
 */
public class DocumentNameFinderAnalyser implements NamedEntities, AutoCloseable {

    private DocumentConverter documentConverter = null;

    /**
     * constructor
     */
    public DocumentNameFinderAnalyser(DocumentConverter documentConverter) {
        this.documentConverter = documentConverter;
    }

    @Override
    public void close() throws Exception {
        this.documentConverter = null;
    }

    @Override
    public Set<String> getNamedEntities(File file, NamedEntitiesExtractor namedEntitiesExtractor) throws Exception {
        BodyContentHandler bodyContentHandler = documentConverter.getBodyContentHandler(file);
        List<List<String>> textcorpus = documentConverter.getCleanedTextCorpus(bodyContentHandler);
        return namedEntitiesExtractor.getNamedEntities(textcorpus);

    }

}
