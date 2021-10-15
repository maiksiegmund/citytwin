# CityTwin

prototype of research project on german textcorpus
nlp features:
- keyword extraction

## algorithm

- [TF-IDF](https://en.wikipedia.org/wiki/Tf%E2%80%93idf#References)
- [TextRank](https://aclanthology.org/W04-3252.pdf)
- [Word2Vec](https://arxiv.org/pdf/1301.3781.pdf)

## used frameworks

- [apache opennlp](https://github.com/apache/opennlp/blob/master/)
- [apache tika](https://github.com/apache/tika/blob/main/)
- [deeplearning4j word2vec](https://github.com/eclipse/deeplearning4j/blob/master/)
- [slf4j](https://github.com/qos-ch/slf4j/blob/master)
- [javatuples](https://github.com/javatuples/javatuples/blob/master/)
- [snowball-stemmer](https://github.com/rholder/snowball-stemmer/)
- [jgrapht](https://github.com/jgrapht/jgrapht/blob/master/)
- [FasterXML](https://github.com/FasterXML/jackson-core/blob/2.13/)

## usage
```java
public static void main(String[] args) {

        try {
            // select a file
            File file = new File("...");
            DocumentAnalyser documentAnalyser = new DocumentAnalyser.Builder().build();
            // filter by alkis catalog
            Map<String, Pair<ALKISDTO, Double>> alkis = documentAnalyser.analyse2ALKIS(file);
            // filter by ct_term catalog
            Map<String, Pair<TermDTO, Double>> terms = documentAnalyser.analyse2Terms(file);
           
        } catch (Exception exception) {
            // toDO Exception handling
        }

    }
``` 

## resources 

[keywordAnalyser.zip](https://github.com/maiksiegmund/citytwin/blob/main/keywordAnalyser.zip) includes resources of 

- alkis_catalog.json [ALKIS](https://www.adv-online.de/icc/extdeu/nav/a63/binarywriterservlet?imgUid=b001016e-7efa-8461-e336-b6951fa2e0c9&uBasVariant=11111111-1111-1111-1111-111111111111)
- ct_terms_catalog.json 
- de-pos-perceptron.bin [pos-tagger](http://opennlp.sourceforge.net/models-1.5/)
- de-sent.bin [sentences-detector](http://opennlp.sourceforge.net/models-1.5/)
- de-token.bin [tokenizer](http://opennlp.sourceforge.net/models-1.5/)
- stopswords_de.txt [stopwordlist](https://github.com/maiksiegmund/CityTwin_SolrConfig/blob/main/cityTwin_managed/lang/stopwords_de.txt)
- word2vectestModel.bin (test only)
