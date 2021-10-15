package de.citytwin;

import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.tika.metadata.Metadata;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.TransactionWork;
import org.neo4j.driver.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * //TODO Javadoc! Rename (DAO?)
 * check whether methods can be generalized to avoid copy paste style code and make use of the concept of "overloading"
 *  
 * @author Maik, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class DBController {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Driver driver;
    private Session session;

    public DBController(String uri, String user, String password) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));

    }

    public void addTerm(TermDTO termDTO) {

        Boolean exist = false;
        try(Session session = driver.session()) {
            exist = session.readTransaction(new TransactionWork<Boolean>() {

                @Override
                public Boolean execute(Transaction transaction) {
                    return existTermNodeCypher(transaction, termDTO);

                }
            });
            if (exist) {
                session.writeTransaction(new TransactionWork<Void>() {

                    @Override
                    public Void execute(Transaction transaction) {
                        // TODO Auto-generated method stub
                        return createTermNodeCypher(transaction, termDTO);
                    }
                });
            }

        }
    }

    private void ALKISPart(Session session, Metadata metadata, ALKISDTO alkisDTO, String term, double weight) {
        boolean linked = false;
        boolean exist = false;
        // ALKIS
        exist = session.readTransaction(existALKISNode(alkisDTO));
        if (!exist) {
            session.writeTransaction(createALKISNode(alkisDTO));
        }
        // link alkis and document
        linked = session.readTransaction(isDocumentAndALKISLinked(metadata, alkisDTO));
        if (!linked) {
            session.writeTransaction(linkDocumentAndALKIS(metadata, alkisDTO));
        }
        // link alkis and keyword
        linked = session.readTransaction(isKeywordAndALKISLinked(term, alkisDTO));
        if (!linked) {
            session.writeTransaction(linkKeywordAndALKIS(term, alkisDTO, weight));
        }
    }

    public void close() {

        if (session != null && session.isOpen()) {
            session.close();
        }
        driver.close();

    }

    private TransactionWork<Void> createALKISNode(ALKISDTO dto) {
        return new TransactionWork<Void>() {

            @Override
            public Void execute(Transaction transaction) {

                return createALKISNodeCypher(transaction, dto);
            }
        };
    }

    /**
     * this method create a node with label as ALKIS, properties: categorie, code, name
     *
     * @param transaction
     * @param dto
     * @return
     */
    private Void createALKISNodeCypher(Transaction transaction, ALKISDTO dto) {
        transaction.run("CREATE (:ALKIS {categorie: $categorie, code: $code, name: $name})",
                Values.parameters(
                        "categorie",
                        dto.getCategorie(),
                        "code",
                        dto.getCode(),
                        "name",
                        dto.getName()));
        return null;

    }

    private TransactionWork<Void> createDocumentNode(Metadata metadata) {
        return new TransactionWork<Void>() {

            @Override
            public Void execute(Transaction transaction) {
                return createDocumentNodeCypher(transaction, metadata);
            }
        };
    }

    /**
     * this method create a node with label as document, properties by metaData
     *
     * @param transaction
     * @param metadata
     * @return {@code Void}
     */
    private Void createDocumentNodeCypher(Transaction transaction, final Metadata metadata) {

        String query = "";
        StringBuilder stringBuilder = new StringBuilder();
        String name = "";
        Object data = null;
        String comma = ",";
        Map<String, Object> parameters = new HashMap<String, Object>();

        MessageFormat.format("{0}: ${0}", name);
        for (int index = 0; index < metadata.names().length; ++index) {
            name = metadata.names()[index].replace("-", "");
            name = name.replace(":", "");
            data = metadata.get(metadata.names()[index]);
            comma = (index == metadata.names().length - 1) ? " " : ", ";
            stringBuilder.append(MessageFormat.format("{0}: ${0}{1} ", name, comma));
            parameters.put(name, data);
        }
        query = (MessageFormat.format("CREATE (:Document '{'{0}'}')", stringBuilder.toString()));
        transaction.run(query, parameters);
        return null;
    }

    private TransactionWork<Void> createKeywordNode(final String name) {
        return new TransactionWork<Void>() {

            @Override
            public Void execute(Transaction transaction) {

                return createKeywordNodeCypher(transaction, name);
            }
        };
    }

    /**
     * this method create a node with label as keyword, no properties
     *
     * @param transaction
     * @param keyword
     * @return {@code Void}
     */
    private Void createKeywordNodeCypher(Transaction transaction, final String keyword) {
        transaction.run("CREATE (:Keyword {name: $keyword})",
                Values.parameters(
                        "keyword",
                        keyword));
        return null;
    }

    private TransactionWork<Void> createOntologyNode(final String ontology) {
        return new TransactionWork<Void>() {

            @Override
            public Void execute(Transaction transaction) {

                return createOntologyNodeCypher(transaction, ontology);
            }
        };
    }

    /**
     * this method create a node with label as Ontology, property: name
     *
     * @param transaction
     * @param ontology {@link TermDTO#getOntologies()}
     * @return
     */
    private Void createOntologyNodeCypher(Transaction transaction, final String ontology) {
        transaction.run("CREATE (:Ontology {name: $ontology})",
                Values.parameters(
                        "ontology",
                        ontology));
        return null;

    }

    private TransactionWork<Void> createTermNode(final TermDTO dto) {
        return new TransactionWork<Void>() {

            @Override
            public Void execute(Transaction transaction) {

                return createTermNodeCypher(transaction, dto);
            }
        };
    }

    /**
     * this method create a node with label as term, properties: isCore, morphem, name
     *
     * @param transaction
     * @param dto
     * @return {@code Void}
     */
    private Void createTermNodeCypher(Transaction transaction, TermDTO dto) {

        transaction.run("CREATE (:Term {isCore: $isCore, morphem: $morphem, name: $term })",
                Values.parameters(
                        "isCore",
                        dto.getIsCore(),
                        "morphem",
                        dto.getMorphem(),
                        "term",
                        dto.getTerm()));
        return null;

    }

    private TransactionWork<Boolean> existALKISNode(ALKISDTO alkisDTO) {

        return new TransactionWork<Boolean>() {

            @Override
            public Boolean execute(Transaction transaction) {

                return existALKISNodeCypher(transaction, alkisDTO);
            }
        };
    }

    private Boolean existALKISNodeCypher(Transaction transaction, ALKISDTO alkisDTO) {
        Result result = transaction.run("Match (alkis:ALKIS) where alkis.name = $name return (alkis)",
                Values.parameters("name", alkisDTO.getName()));
        if (result.hasNext()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private TransactionWork<Boolean> existDocumentNode(Metadata metadata) {
        return new TransactionWork<Boolean>() {

            @Override
            public Boolean execute(Transaction transaction) {
                return existDocumentNodeCypher(transaction, metadata);
            }
        };
    }

    /**
     * this method check whether a node labeled as document exist by name property (full filename with extension)
     *
     * @param transaction
     * @param metadata
     * @return
     */
    private Boolean existDocumentNodeCypher(Transaction transaction, final Metadata metadata) {
        Result result = transaction.run("Match (doc:Document) where doc.name = $name return (doc)",
                Values.parameters(
                        "name",
                        metadata.get("name")));
        if (result.hasNext()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private TransactionWork<Boolean> existKeyWordNode(String keyword) {

        return new TransactionWork<Boolean>() {

            @Override
            public Boolean execute(Transaction transaction) {

                return existKeyWordNodeCypher(transaction, keyword);
            }
        };
    }

    /**
     * this method check whether a node labeled as keyword exist by name property
     *
     * @param transaction
     * @param name
     * @return
     */
    private Boolean existKeyWordNodeCypher(Transaction transaction, final String name) {
        Result result = transaction.run("Match (word:Keyword) where word.name = $name return word",
                Values.parameters(
                        "name",
                        name));
        if (result.hasNext()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private TransactionWork<Boolean> existOntologyNode(final String ontology) {

        return new TransactionWork<Boolean>() {

            @Override
            public Boolean execute(Transaction transaction) {

                return existOntologyNodeCypher(transaction, ontology);
            }
        };
    }

    /**
     * this method check whether a node labeled as Ontology exist by name property
     *
     * @param transaction
     * @param ontology
     * @return
     */
    private Boolean existOntologyNodeCypher(Transaction transaction, final String ontology) {
        Result result = transaction.run("Match (ont:Ontology) where ont.name = $ontology return (ont)",
                Values.parameters("ontology", ontology));
        if (result.hasNext()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private TransactionWork<Boolean> existTermNode(final TermDTO termDTO) {

        return new TransactionWork<Boolean>() {

            @Override
            public Boolean execute(Transaction transaction) {

                return existTermNodeCypher(transaction, termDTO);
            }
        };
    }

    /**
     * this method check whether a node labeled as Term exist by name property
     *
     * @param transaction
     * @param termDTO
     * @return
     */
    private Boolean existTermNodeCypher(Transaction transaction, final TermDTO termDTO) {
        Result result = transaction.run("Match (term:Term) where term.name = $name return (term)",
                Values.parameters("name", termDTO.getName()));
        if (result.hasNext()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private TransactionWork<Boolean> isDocumentAndALKISLinked(final Metadata metaData, final ALKISDTO alkisdto) {

        return new TransactionWork<Boolean>() {

            @Override
            public Boolean execute(Transaction transaction) {

                return isDocumentAndALKISLinkedCypher(transaction, metaData, alkisdto);
            }
        };
    }

    /**
     * this method check is whether two nodes (label:document.name), label:ALKIS.name) linked
     *
     * @param transaction
     * @param metaData
     * @param alkisdto
     * @return {@code Boolean}
     */
    private Boolean isDocumentAndALKISLinkedCypher(Transaction transaction, final Metadata metaData, final ALKISDTO alkisdto) {
        Result result = transaction.run("Match edge=(:Document{name: $name})-[:contains]->(:ALKIS{name: $alkisName}) return edge",
                Values.parameters("name", metaData.get("name"), "alkisName", alkisdto.getName()));
        if (result.hasNext()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private TransactionWork<Boolean> isDocumentAndKeywordLinked(final Metadata metaData, final String keyword) {

        return new TransactionWork<Boolean>() {

            @Override
            public Boolean execute(Transaction transaction) {

                return isDocumentAndKeywordLinkedCypher(transaction, metaData, keyword);
            }
        };
    }

    /**
     * this method check is whether two nodes (label:document.name), label:Keyword.name) linked
     *
     * @param transaction
     * @param metaData
     * @param keyword
     * @return {@code Boolean}
     */
    private Boolean isDocumentAndKeywordLinkedCypher(Transaction transaction, final Metadata metaData, final String keyword) {
        Result result = transaction.run("Match edge=(:Document{name: $name})-[:contains]->(:Keyword{name: $keyword}) return edge",
                Values.parameters("name", metaData.get("name"), "keyword", keyword));
        if (result.hasNext()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private TransactionWork<Boolean> isDocumentAndOntologyLinked(final Metadata metaData, final String ontology) {

        return new TransactionWork<Boolean>() {

            @Override
            public Boolean execute(Transaction transaction) {

                return isDocumentAndOntologyLinkedCypher(transaction, metaData, ontology);
            }
        };
    }

    private Boolean isDocumentAndOntologyLinkedCypher(Transaction transaction, final Metadata metaData, final String ontology) {
        Result result = transaction.run("Match edge=(:Document{name: $docName})-[:belongsTo]->(:Ontology{name: $ontology}) return (edge)",
                Values.parameters("docName",
                        metaData.get("name"),
                        "ontology",
                        ontology));
        if (result.hasNext()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private TransactionWork<Boolean> isDocumentAndTermLinked(final Metadata metaData, final TermDTO termDTO) {

        return new TransactionWork<Boolean>() {

            @Override
            public Boolean execute(Transaction transaction) {

                return isDocumentAndTermLinkedCypher(transaction, metaData, termDTO);
            }
        };
    }

    /**
     * this method check is whether two nodes (label:document.name), label:Term.name) linked
     *
     * @param transaction
     * @param metaData
     * @param termDTO
     * @return {@code Boolean}
     */
    private Boolean isDocumentAndTermLinkedCypher(Transaction transaction, final Metadata metaData, final TermDTO termDTO) {
        Result result = transaction.run("Match edge=(:Document{name: $docName})-[:contains]->(:Term{name: $termName}) return edge",
                Values.parameters("docName", metaData.get("name"), "termName", termDTO.getTerm()));
        if (result.hasNext()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private TransactionWork<Boolean> isKeywordAndALKISLinked(final String keyword, final ALKISDTO alkisDTO) {

        return new TransactionWork<Boolean>() {

            @Override
            public Boolean execute(Transaction transaction) {

                return isKeywordAndALKISLinkedCypher(transaction, keyword, alkisDTO);
            }
        };
    }

    private Boolean isKeywordAndALKISLinkedCypher(Transaction transaction, final String keyword, final ALKISDTO alkisDTO) {
        Result result = transaction.run("Match edge=(:word{name: $keyword})-[:belongsTo]->(:ALKIS{name: $name}) return (edge)",
                Values.parameters("keyword", keyword, "name", alkisDTO.getName()));
        if (result.hasNext()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private TransactionWork<Boolean> isKeywordAndTermLinked(final String keyword, final TermDTO termDTO) {

        return new TransactionWork<Boolean>() {

            @Override
            public Boolean execute(Transaction transaction) {

                return isKeywordAndTermLinkedCypher(transaction, keyword, termDTO);
            }
        };
    }

    private Boolean isKeywordAndTermLinkedCypher(Transaction transaction, final String keyword, final TermDTO termDTO) {
        Result result = transaction.run("Match edge=(:Keyword{name: $keyword})-[:belongsTo]->(:Term{name: $term}) return (edge)",
                Values.parameters("keyword", keyword, "term", termDTO.getTerm()));
        if (result.hasNext()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private TransactionWork<Void> linkDocumentAndALKIS(final Metadata metaData, final ALKISDTO alkisdto) {

        return new TransactionWork<Void>() {

            @Override
            public Void execute(Transaction transaction) {

                return linkDocumentAndALKISCypher(transaction, metaData, alkisdto);
            }
        };
    }

    private Void linkDocumentAndALKISCypher(Transaction transaction, final Metadata metaData, final ALKISDTO alkisdto) {
        transaction.run("Match (doc:Document), (alkis:ALKIS) where doc.name = $docName and alkis.name = $alkisName"
                + " Create (doc)-[:contains] ->(alkis)"
                + " Create (alkis)-[:affected] ->(doc)",
                Values.parameters("docName",
                        metaData.get("name"),
                        "alkisName",
                        alkisdto.getName()));

        return null;
    }

    private TransactionWork<Void> linkDocumentAndKeyword(final Metadata metaData, final String keyword, final double weight) {

        return new TransactionWork<Void>() {

            @Override
            public Void execute(Transaction transaction) {

                return linkDocumentAndKeywordCypher(transaction, metaData, keyword, weight);
            }
        };
    }

    private Void linkDocumentAndKeywordCypher(Transaction transaction, final Metadata metaData, final String keyword, final double weight) {
        transaction.run("Match (doc:Document), (word:Keyword) where doc.name = $name and word.name = $keyword"
                + " Create (doc)-[:contains{weight:$weight} ] ->(word)"
                + " Create (word)-[:affected] ->(doc)",
                Values.parameters("name",
                        metaData.get("name"),
                        "keyword",
                        keyword,
                        "weight",
                        weight));
        return null;
    }

    private TransactionWork<Void> linkDocumentAndOntology(final Metadata metaData, final String ontology) {

        return new TransactionWork<Void>() {

            @Override
            public Void execute(Transaction transaction) {

                return linkDocumentAndOntologyCypher(transaction, metaData, ontology);
            }
        };
    }

    private Void linkDocumentAndOntologyCypher(Transaction transaction, final Metadata metaData, final String ontology) {

        transaction.run("Match (doc:Document), (ont:Ontology)  where doc.name = $docName and ont.name = $ontology"
                + " Create (doc)-[:belongsTo] ->(ont)"
                + " Create (ont)-[:affected] ->(doc)",
                Values.parameters("docName",
                        metaData.get("name"),
                        "ontology",
                        ontology));

        return null;
    }

    private TransactionWork<Void> linkDocumentAndTerm(final Metadata metaData, final TermDTO termDTO) {

        return new TransactionWork<Void>() {

            @Override
            public Void execute(Transaction transaction) {

                return linkDocumentAndTermCypher(transaction, metaData, termDTO);
            }
        };
    }

    private Void linkDocumentAndTermCypher(Transaction transaction, final Metadata metaData, final TermDTO termDTO) {

        transaction.run("Match (doc:Document), (term:Term)  where doc.name = $docName and term.name = $termName"
                + " Create (doc)-[:contains] ->(term)"
                + " Create (term)-[:affected] ->(doc)",
                Values.parameters("docName",
                        metaData.get("name"),
                        "termName",
                        termDTO.getTerm()));

        return null;
    }

    private TransactionWork<Void> linkKeywordAndALKIS(final String keyword, final ALKISDTO alkisdto, final double weight) {

        return new TransactionWork<Void>() {

            @Override
            public Void execute(Transaction transaction) {

                return linkKeywordAndALKISCypher(transaction, keyword, alkisdto, weight);
            }
        };
    }

    // TODO for instance: check whether this method and the linkKeywordAndTermCypher method counterpart can be generalized into a parameterized method linkKeywordAndDTOCypher, use instanceof for differentiating as the current methods implementations are identical excluding the specific "term" and "alkis" part
    // continue checking code for similiar patterns, test frequently after refactoring to ensure code is still working :)
    private Void linkKeywordAndALKISCypher(Transaction transaction, final String keyword, final ALKISDTO alkisdto, final double weight) {
        transaction.run("Match (word:Keyword), (alkis:ALKIS) where word.name = $keyword and alkis.name = $alkisName"
                + " Create (doc)-[:belongsTo{weight:$weight}] ->(alkis)",
                Values.parameters("keyword",
                        keyword,
                        "alkisName",
                        alkisdto.getName(),
                        "weight",
                        weight));

        return null;
    }

    private TransactionWork<Void> linkKeywordAndTerm(final String keyword, final TermDTO termDTO, final double weight) {

        return new TransactionWork<Void>() {

            @Override
            public Void execute(Transaction transaction) {

                return linkKeywordAndTermCypher(transaction, keyword, termDTO, weight);
            }
        };
    }

    private Void linkKeywordAndTermCypher(Transaction transaction, final String keyword, final TermDTO termDTO, final double weight) {
        transaction.run("Match (word:Keyword), (term:Term) where word.name = $keyword and term.name = $term"
                + " Create (word)-[:belongsTo{weight:$weight}] ->(term)",
                Values.parameters("keyword",
                        keyword,
                        "term",
                        termDTO.getTerm(),
                        "weight",
                        weight));

        return null;
    }

    public <T> void persist(Map<String, Pair<T, Double>> filteredKeyWords, Metadata metadata) {

        try(Session session = driver.session()) {

            Boolean exist = false;
            Boolean linked = false;
            Pair<T, Double> pair;

            // document
            exist = session.readTransaction(existDocumentNode(metadata));
            if (!exist) {
                session.writeTransaction(createDocumentNode(metadata));
            }
            for (String key : filteredKeyWords.keySet()) {
                pair = filteredKeyWords.get(key);
                T dto = pair.getLeft();
                exist = session.readTransaction(existKeyWordNode(key));
                if (!exist) {
                    session.writeTransaction(createKeywordNode(key));
                }
                // link keyword and document
                linked = session.readTransaction(isDocumentAndKeywordLinked(metadata, key));
                if (!linked) {
                    double weight = pair.getRight();
                    session.writeTransaction(linkDocumentAndKeyword(metadata, key, weight));
                }
                if (dto instanceof TermDTO) {
                    TermDTO termDTO = (TermDTO)dto;
                    termPart(session, metadata, termDTO, key, pair.getRight());
                }

                if (dto instanceof ALKISDTO) {
                    ALKISDTO alkisDTO = (ALKISDTO)dto;
                    ALKISPart(session, metadata, alkisDTO, key, pair.getRight());
                }

            }

        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }

    }

    public void purgeDBCypher() {
        try(Session session = driver.session()) {

            Transaction transaction = session.beginTransaction();

            transaction.run("MATCH (n) DETACH DELETE n");
            transaction.commit();
            transaction.close();

        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }
    }

    private void termPart(Session session, Metadata metadata, TermDTO termDTO, String term, double weight) {
        boolean linked = false;
        boolean exist = false;

        // ontology and document
        for (String ontology : termDTO.getOntologies()) {
            // exist ontology
            exist = session.readTransaction(existOntologyNode(ontology));
            if (!exist) {
                session.writeTransaction(createOntologyNode(ontology));
            }
            // link ontology and document
            linked = session.readTransaction(isDocumentAndOntologyLinked(metadata, ontology));
            if (!linked) {
                session.writeTransaction(linkDocumentAndOntology(metadata, ontology));
            }
        }
        // term
        exist = session.readTransaction(existTermNode(termDTO));
        if (!exist) {
            session.writeTransaction(createTermNode(termDTO));
        }
        // link term and document
        linked = session.readTransaction(isDocumentAndTermLinked(metadata, termDTO));
        if (!linked) {
            session.writeTransaction(linkDocumentAndTerm(metadata, termDTO));
        }
        // link keyword and Term
        linked = session.readTransaction(isKeywordAndTermLinked(term, termDTO));
        if (!linked) {
            session.writeTransaction(linkKeywordAndTerm(term, termDTO, weight));
        }
    }

}
