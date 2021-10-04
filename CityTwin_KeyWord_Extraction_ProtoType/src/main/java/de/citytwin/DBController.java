package de.citytwin;

import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
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

    public void addTermEntry(TermDTO dto) {

        Boolean exist = false;
        try(Session session = driver.session()) {
            exist = session.readTransaction(new TransactionWork<Boolean>() {

                @Override
                public Boolean execute(Transaction transaction) {
                    return existOntologyNode(transaction, dto.getTerm());

                }
            });
            if (exist) {
                session.writeTransaction(new TransactionWork<Void>() {

                    @Override
                    public Void execute(Transaction transaction) {
                        // TODO Auto-generated method stub
                        return createOntologyEntryNode(transaction, dto);
                    }
                });
            }

        }
    }

    public void close() {
        if (session.isOpen()) {
            session.close();
        }
        driver.close();

    }

    private TransactionWork<Void> create(Metadata metadata) {
        return new TransactionWork<Void>() {

            @Override
            public Void execute(Transaction transaction) {
                return createDocumentNode(transaction, metadata);
            }
        };
    }

    public void createALKISEntries(List<ALKISDTO> dtos) {
        try(Session session = driver.session()) {

            Boolean exist = false;
            for (ALKISDTO dto : dtos) {

                exist = session.readTransaction(new TransactionWork<Boolean>() {

                    @Override
                    public Boolean execute(Transaction transaction) {
                        return existALKISNode(transaction, dto);

                    }
                });
                if (!exist) {
                    session.writeTransaction(new TransactionWork<Void>() {

                        @Override
                        public Void execute(Transaction transaction) {

                            return createALKISNode(transaction, dto);
                        }

                    });
                }

            }

        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }
    }

    private Void createALKISNode(Transaction transaction, ALKISDTO dto) {
        transaction.run("CREATE (:ALKISEntry {categorie: $categorie, code: $code, name: $name})",
                Values.parameters(
                        "categorie",
                        dto.getCategorie(),
                        "code",
                        dto.getCode(),
                        "name",
                        dto.getName()));
        return null;

    }

    private Void createDocumentNode(Transaction transaction, final Metadata metadata) {

        String query = "";
        StringBuilder stringBuilder = new StringBuilder();
        String name = "";
        Object data = null;

        Map<String, Object> parameters = new HashMap<String, Object>();

        MessageFormat.format("{0}: ${0}", name);
        for (int index = 0; index < metadata.names().length; ++index) {
            name = metadata.names()[index].replace("-", "");
            name = name.replace(":", "");
            data = metadata.get(metadata.names()[index]);
            stringBuilder.append(MessageFormat.format("{0}: ${0},", name));
            parameters.put(name, data);
        }
        // default property
        stringBuilder.append(MessageFormat.format("{0}: ${0},", "name"));
        query = (MessageFormat.format("CREATE (:Document '{'{0}'}')", stringBuilder.toString()));
        transaction.run(query, parameters);
        return null;
    }

    private Void createKeywordNode(Transaction transaction, final String name) {
        transaction.run("CREATE (:Keyword {name: $name})",
                Values.parameters(
                        "name",
                        name));
        return null;
    }

    private Void createOntologyEntryNode(Transaction transaction, TermDTO dto) {

        transaction.run("CREATE (:OntologyEntry {isCore: $isCore, morphem: $morphem, name: $term })",
                Values.parameters(
                        "isCore",
                        dto.isCore,
                        "morphem",
                        dto.morphem,
                        "isSematnic",
                        dto.term,
                        "term"));
        return null;

    }

    private Void createOntologyNode(Transaction transaction, final String ontology) {
        transaction.run("CREATE (:Ontology {name: $ontology})",
                Values.parameters(
                        "ontology",
                        ontology));
        return null;

    }

    private TransactionWork<Boolean> exist(Metadata metadata) {
        return new TransactionWork<Boolean>() {

            @Override
            public Boolean execute(Transaction transaction) {
                return existDocument(transaction, metadata);
            }
        };
    }

    private Boolean existALKISNode(Transaction transaction, ALKISDTO dto) {
        Result result = transaction.run("Match (alkis:ALKISEntry) where alkis.name = $name",
                Values.parameters("name", dto.getName()));
        if (result.hasNext()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private Boolean existDocument(Transaction transaction, final Metadata metadata) {
        Result result = transaction.run("Match (doc:Document) where doc.fileName = $fileName return (doc)",
                Values.parameters(
                        "fileName",
                        metadata.get("fileName")));
        if (result.hasNext()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private Boolean existKeyWordNode(Transaction transaction, final String name) {
        Result result = transaction.run("Match (keyw:Keyword) where keyw.name = $name return keyw",
                Values.parameters(
                        "name",
                        name));
        if (result.hasNext()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private Boolean existOntologEntryyNode(Transaction transaction, TermDTO dto) {

        Result result = transaction.run("Match (ont:OntologyEntry) where ont.name = $name return id(ont)",
                Values.parameters("name", dto.getName()));
        if (result.hasNext()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private Boolean existOntologyNode(Transaction transaction, final String ontology) {
        Result result = transaction.run("Match (ont:Ontology) where ont.name = $ontology return (ont)",
                Values.parameters("ontology", ontology));
        if (result.hasNext()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private Boolean isLinked(Transaction transaction, final Metadata documentMetadata, final ALKISDTO alkisdto) {
        Result result = transaction.run("Match edge=(doc:Document{fileName: $fileName})-[:contains]->(ALKISEntry{name: $alkisname}) return edge",
                Values.parameters("fileName", documentMetadata.get("fileName"), "alkisname", alkisdto.getName()));
        if (result.hasNext()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private Boolean isLinked(Transaction transaction, final Metadata documentMetadata, final String keyword) {
        Result result = transaction.run("Match edge=(doc:Document{fileName: $fileName})-[:contains]->(Keyword{name: $keyword}) return edge",
                Values.parameters("fileName", documentMetadata.get("fileName"), "keyword", keyword));
        if (result.hasNext()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private Boolean isLinked(Transaction transaction, final String ontology, final Metadata documentMetadata) {
        Result result = transaction.run("Match edge=(ont:Ontology{type: $ontology})-[:listed]->(doc:Document{fileName: $fileName}) return (edge)",
                Values.parameters("ontology", ontology, "fileName", documentMetadata.get("fileName")));
        if (result.hasNext()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private Boolean isLinked(Transaction transaction, final String ontology, final String keyword) {
        Result result = transaction.run("Match edge=(Ontology{name: $ontology})-[:listed]->(Keyword{name: $keyword}) return (edge)",
                Values.parameters("ontology", ontology, "keyword", keyword));
        if (result.hasNext()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private Boolean isLinked(Transaction transaction, final String ontology, final TermDTO dto) {
        Result result = transaction.run("Match edge=(Ontology{name: $ontology})-[:listed]->(OntologyEntry{name: $term}) return (edge)",
                Values.parameters("ontology", ontology, "term", dto.getTerm()));
        if (result.hasNext()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private Void linkDocumentAndALKIS(Transaction transaction, final Metadata documentMetadata, final ALKISDTO alkisdto, final double weight) {
        transaction.run("Match (doc:Document), (alkis:ALKISEntry) where doc.fileName = $fileName and keyw.name = $alkisname"
                + " Create (doc)-[:contains{weight:$weight} ] ->(alkis)",
                Values.parameters("fileName",
                        documentMetadata.get("fileName"),
                        "alkisname",
                        alkisdto.getName(),
                        "weight",
                        weight));
        return null;
    }

    private Void linkDocumentAndKeyword(Transaction transaction, final Metadata documentMetadata, final String keyword, final double weight) {
        transaction.run("Match (doc:Document), (keyw:Keyword) where doc.fileName = $fileName and keyw.name = $keyowrd"
                + " Create (doc)-[:contains{weight:$weight} ] ->(keyw)",
                Values.parameters("fileName",
                        documentMetadata.get("fileName"),
                        "keyowrd",
                        keyword,
                        "weight",
                        weight));
        return null;
    }

    private Void linkOntologyAndDocument(Transaction transaction, final String ontology, final Metadata documentMetadata) {

        transaction.run("Match (ont:Ontology) ,(doc:Document) where ont.name = $ontology and doc.fileName = $fileName"
                + " Create (doc)-[:listed] ->(add)",
                Values.parameters("ontology", ontology, "fileName", documentMetadata.get("fileName")));

        return null;
    }

    private Void linkOntologyAndKeyword(Transaction transaction, final String ontology, final String keyword) {
        transaction.run("Match (ont:Ontology), (keyw:Keyword) where ont.name = $ontology and keyw.name = $name"
                + " Create (keyw)-[:partof] ->(ont)",
                Values.parameters("ontology", ontology, "$name", keyword));
        return null;
    }

    private Void linkOntologyAndKeyword(Transaction transaction, final String ontology, final TermDTO keyword) {
        transaction.run("Match (ont:Ontology), (keyw:Keyword) where ont.name = $ontology and keyw.name = $name"
                + " Create (keyw)-[:partof] ->(ont)",
                Values.parameters("ontology", ontology, "$name", keyword));
        return null;
    }

    public <T> void persist(Map<String, Pair<T, Double>> filteredKeyWords, Metadata metadata) {

        try(Session session = driver.session()) {

            Boolean exist = false;
            Boolean linked = false;
            Pair<T, Double> pair;

            // exist = session.readTransaction(new TransactionWork<Boolean>() {
            //
            // @Override
            // public Boolean execute(Transaction transaction) {
            // return existDocument(transaction, metadata);
            // }
            // });
            exist = session.readTransaction(exist(metadata));
            if (!exist) {
                // session.writeTransaction(new TransactionWork<Void>() {
                //
                // @Override
                // public Void execute(Transaction transaction) {
                // return createDocumentNode(transaction, metadata);
                // }
                // });
                session.writeTransaction(create(metadata));
            }
            for (String key : filteredKeyWords.keySet()) {
                pair = filteredKeyWords.get(key);
                T dto = pair.getLeft();
                exist = session.readTransaction(new TransactionWork<Boolean>() {

                    @Override
                    public Boolean execute(Transaction transaction) {

                        return existKeyWordNode(transaction, key);
                    }
                });
                if (!exist) {
                    session.writeTransaction(new TransactionWork<Void>() {

                        @Override
                        public Void execute(Transaction transaction) {
                            return createKeywordNode(transaction, key);
                        }
                    });
                }
                linked = session.readTransaction(new TransactionWork<Boolean>() {

                    @Override
                    public Boolean execute(Transaction transaction) {
                        return isLinked(transaction, metadata, key);
                    }
                });
                if (!linked) {
                    double weight = pair.getRight();
                    session.writeTransaction(new TransactionWork<Void>() {

                        @Override
                        public Void execute(Transaction transaction) {
                            return linkDocumentAndKeyword(transaction, metadata, key, weight);
                        }
                    });
                }
                if (dto instanceof TermDTO) {

                }

                if (dto instanceof ALKISDTO) {
                    ALKISDTO ontologyDTO = (ALKISDTO)dto;

                }

            }

        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }

    }

    private void purgeDB() {
        try(Session session = driver.session()) {

            Transaction transaction = session.beginTransaction();

            transaction.run("MATCH (n) DETACH DELETE n");
            transaction.commit();
            transaction.close();

        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }
    }

}
