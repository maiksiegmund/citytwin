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

    public void addOntologyEntry(OntologyDTO dto) {

        Boolean exist = false;
        try(Session session = driver.session()) {
            exist = session.readTransaction(new TransactionWork<Boolean>() {

                @Override
                public Boolean execute(Transaction transaction) {
                    return existOntologyTypeNode(transaction, dto);

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

    private Void createOntologyEntryNode(Transaction transaction, OntologyDTO dto) {

        transaction.run("CREATE (:OntologyEntry {isCore: $isCore, isKeyWord: $isKeyWord, isSemantic: $isSemantic, name: $word, stemm: $stemm})",
                Values.parameters(
                        "isCore",
                        dto.isCore(),
                        "isKeyWord",
                        dto.isKeyWord(),
                        "isSematnic",
                        dto.isSemantic(),
                        "name",
                        dto.getWord(),
                        "stemm",
                        dto.getStemm()));
        return null;

    }

    private Void createOntologyTypeNode(Transaction transaction, final OntologyDTO dto) {
        transaction.run("CREATE (:OntologyType {name: $type})",
                Values.parameters(
                        "name",
                        dto.getType()));
        return null;

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

    private Boolean existOntologEntryyNode(Transaction transaction, OntologyDTO dto) {

        Result result = transaction.run("Match (ont:OntologyEntry) where ont.name = $name return id(ont)",
                Values.parameters("name", dto.getWord()));
        if (result.hasNext()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private Boolean existOntologyTypeNode(Transaction transaction, final OntologyDTO dto) {
        Result result = transaction.run("Match (ont:OntologyType) where ont.type = $type return (ont)",
                Values.parameters("type", dto.getType()));
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

    private Boolean isLinked(Transaction transaction, final OntologyDTO ontologyDTO, final Metadata documentMetadata) {
        Result result = transaction.run("Match edge=(ont:OntologyType{type: $type})-[:listed]->(doc:Document{fileName: $fileName}) return (edge)",
                Values.parameters("type", ontologyDTO.getType(), "fileName", documentMetadata.get("fileName")));
        if (result.hasNext()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private Boolean isLinked(Transaction transaction, final OntologyDTO ontologyDTO, final String keyword) {
        Result result = transaction.run("Match edge=(ont:OntologyType{type: $type})-[:listed]->(Keyword{name: $keyword}) return (edge)",
                Values.parameters("type", ontologyDTO.getType(), "keyword", keyword));
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

    private Void linkOntologyTypeAndDocument(Transaction transaction, final OntologyDTO ontologyDTO, final Metadata documentMetadata) {
        transaction.run("Match (ont:OntologyType) ,(doc:Document) where ont.type = $ontologyType and doc.fileName = $fileName"
                + " Create (doc)-[:listed] ->(add)",
                Values.parameters("ontologyType", ontologyDTO.getType(), "fileName", documentMetadata.get("fileName")));
        return null;
    }

    private Void linkOntologyTypeAndKeyword(Transaction transaction, final OntologyDTO ontologyDTO, final String keyword) {
        transaction.run("Match (ont:OntologyType), (keyw:Keyword) where ont.type = $ontologyType and keyw.name = $name"
                + " Create (keyw)-[:partof] ->(ont)",
                Values.parameters("ontologyType", ontologyDTO.getType(), "$name", keyword));
        return null;
    }

    public <T> void persist(Map<String, Pair<T, Double>> filteredKeyWords, Metadata metadata) {

        try(Session session = driver.session()) {

            Boolean exist = false;
            Boolean linked = false;
            Pair<T, Double> pair;

            exist = session.readTransaction(new TransactionWork<Boolean>() {

                @Override
                public Boolean execute(Transaction transaction) {
                    return existDocument(transaction, metadata);
                }
            });
            if (!exist) {
                session.writeTransaction(new TransactionWork<Void>() {

                    @Override
                    public Void execute(Transaction transaction) {
                        return createDocumentNode(transaction, metadata);
                    }
                });
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
                if (dto instanceof OntologyDTO) {

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
