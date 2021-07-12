package de.citytwin;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

//import org.apache.spark.sql.Dataset;
//import org.apache.spark.sql.Row;
//import org.apache.spark.sql.RowFactory;
//import org.apache.spark.sql.SparkSession;
//import org.apache.spark.sql.types.ArrayType;
//import org.apache.spark.sql.types.DataTypes;
//import org.apache.spark.sql.types.Metadata;
//import org.apache.spark.sql.types.StructField;
//import org.apache.spark.sql.types.StructType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Word2VecAnalyser {

    /** current version information */
    private static final String VERSION = "$Revision: 1.00 $";
    /** logger */
    private static final transient Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private GermanTextProcessing textProcessing = null;

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

    public Word2VecAnalyser() throws IOException {
        initialize();
    }

    private List<String> transformText(File jsonFile) throws IOException {

        List<String> results = new ArrayList<String>();
        ObjectMapper mapper = new ObjectMapper();
        JsonParser parser = mapper.createParser(jsonFile);
        JsonToken token = parser.nextToken();

        while (token != null) {
            // seeking text fieldname
            if ("text".equals(parser.getText())) {
                // next token is text field value
                token = parser.nextToken();
                if (!parser.getText().isBlank()) {
                    results.add(parser.getText());

                }
            }
            token = parser.nextToken();

        }
        logger.info(MessageFormat.format("text corpus transformation completed contains {0} useful atricles ", results.size()));
        return results;

    }

    /**
     * this method initialize nlp components
     *
     * @throws IOException
     */
    private void initialize() throws IOException {

        textProcessing = new GermanTextProcessing();
//        createDataset();
    }

    /**
     * this method transform a file with json content in a List of sentences, each sentences contains a list of terms.
     *
     * @param jsonFile
     * @return new reference of {@code List<List<String>>}
     * @throws IOException
     */
    public List<List<String>> transforJsonText(File jsonFile) throws IOException {
        List<List<String>> results = new ArrayList<List<String>>();
        List<String> articles = transformText(jsonFile);
        List<String> sentences = textProcessing.tokenizeArticlesToSencences(articles);
        for (String sentence : sentences) {
            results.add(textProcessing.tokenizeOpenNLP(sentence));
        }
        return results;

    }

    // private Dataset<Row> createDataset() {
    //
    // SparkSession spark = SparkSession
    // .builder()
    // .appName("JavaWord2VecExample")
    // .getOrCreate();
    //
    // List<Row> data = Arrays.asList(
    // RowFactory.create(Arrays.asList("Hi I heard about Spark".split(" "))),
    // RowFactory.create(Arrays.asList("I wish Java could use case classes".split(" "))),
    // RowFactory.create(Arrays.asList("Logistic regression models are neat".split(" "))));
    // StructType schema = new StructType(new StructField[] {
    // new StructField("text", new ArrayType(DataTypes.StringType, true), false, Metadata.empty())
    // });
    // return spark.createDataFrame(data, schema);
    //
    // }
}
