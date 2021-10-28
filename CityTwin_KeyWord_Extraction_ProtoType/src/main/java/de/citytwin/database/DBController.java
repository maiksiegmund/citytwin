package de.citytwin.database;

import com.beust.jcommander.internal.Nullable;

import de.citytwin.catalog.ALKIS;
import de.citytwin.catalog.CatalogEntryHasName;
import de.citytwin.catalog.Term;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
 * this class provides funtion to create a graph on neo4j db (very simple)
 *
 * @author Maik Siegmund, FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
public class DBController implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static String DOCUMENT = "Document";
    public static String ALKIS = "ALKIS";
    public static String ONTOLOGY = "Ontology";

    public static String TERM = "Term";
    public static String KEYWORD = "Keyword";
    public static String CONTAINS = "contains";
    public static String BELONGSTO = "belongsTo";
    public static String AFFECT = "affect";

    public static Properties getDefaultProperties() {
        Properties properties = new Properties();
        properties.put("neo4j.uri", "bolt://localhost:7687");
        properties.put("neo4j.user", "neo4j");
        properties.put("neo4j.password", "C1tyTw1n!");
        return properties;

    }

    private Driver driver;

    private Session session;

    /**
     * constructor.
     *
     * @param properties
     * @throws IOException
     */
    public DBController(Properties properties) throws IOException {

        if (validateProperties(properties)) {
            String uri = (String)properties.get("neo4j.uri");
            String user = (String)properties.get("neo4j.user");
            String password = (String)properties.get("neo4j.password");
            driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
        }

    }

    @Override
    public void close() {

        if (session != null && session.isOpen()) {
            session.close();
        }
        driver.close();

    }

    /**
     * this method create Node (catalog entry)
     *
     * @param catalogEntry
     * @return
     */
    private TransactionWork<Void> createNode(CatalogEntryHasName catalogEntry) {
        return new TransactionWork<Void>() {

            @Override
            public Void execute(Transaction transaction) {

                Map<String, Object> parameters = new HashMap<String, Object>();
                String label = "";
                if (catalogEntry instanceof ALKIS) {
                    ALKIS alkis = (ALKIS)catalogEntry;
                    parameters.put("name", alkis.getName());
                    parameters.put("categorie", alkis.getCategorie());
                    parameters.put("code", alkis.getCode());
                    label = DBController.ALKIS;
                }
                if (catalogEntry instanceof Term) {
                    Term term = (Term)catalogEntry;
                    parameters.put("name", term.getName());
                    parameters.put("morphem", term.getMorphem());
                    parameters.put("isCore", term.getIsCore());
                    label = DBController.TERM;
                }
                return createNodeCypher(transaction, label, parameters);
            }
        };
    }

    /**
     * this method create node label as Document
     *
     * @param hasName
     * @return
     */
    private TransactionWork<Void> createNode(Metadata metadata) {
        return new TransactionWork<Void>() {

            @Override
            public Void execute(Transaction transaction) {
                String label = DBController.DOCUMENT;
                Map<String, Object> parameters = new HashMap<String, Object>();

                for (String name : metadata.names()) {
                    parameters.put(name, metadata.get(name));
                }
                return createNodeCypher(transaction, label, parameters);
            }
        };
    }

    /**
     * this method create node
     *
     * @param name
     * @param label
     * @return
     */
    private TransactionWork<Void> createNode(final String name, String label) {
        return new TransactionWork<Void>() {

            @Override
            public Void execute(Transaction transaction) {
                Map<String, Object> parameters = new HashMap<String, Object>();
                parameters.put("name", name);
                return createNodeCypher(transaction, label, parameters);
            }
        };
    }

    /**
     * this method create node cypher
     *
     * @param transaction
     * @param label
     * @param parameters
     * @return
     */
    private Void createNodeCypher(Transaction transaction, String label, Map<String, Object> parameters) {

        String query = "";
        StringBuilder stringBuilder = new StringBuilder();
        String name = "";
        Object data = null;
        String comma = ",";
        // Map<String, Object> parameters = new HashMap<String, Object>();

        MessageFormat.format("{0}: ${0}", name);
        int index = 0;
        for (String key : parameters.keySet()) {
            name = key;
            data = parameters.get(key);
            comma = (index == parameters.size() - 1) ? " " : ", ";
            index++;
            stringBuilder.append(MessageFormat.format("{0}: ${0}{1} ", name, comma));
        }

        query = (MessageFormat.format("CREATE (:{0} '{'{1}'}')", label, stringBuilder.toString()));
        transaction.run(query, parameters);
        return null;
    }

    /**
     * this method checks if a node exist (catalog entry)
     *
     * @param catalogEntry
     * @return
     * @throws Exception
     */
    private TransactionWork<Boolean> existNode(CatalogEntryHasName catalogEntry) throws Exception {
        if (catalogEntry instanceof Term) {
            return existNode(catalogEntry.getName(), DBController.TERM);
        }
        if (catalogEntry instanceof ALKIS) {
            return existNode(catalogEntry.getName(), DBController.ALKIS);
        }
        throw new Exception();

    }

    /**
     * this method checks if a document node exist
     *
     * @param metadata
     * @return
     */
    private TransactionWork<Boolean> existNode(Metadata metadata) {
        return existNode(metadata.get("name"), DBController.DOCUMENT);
    }

    private TransactionWork<Boolean> existNode(String keyword, String label) {

        return new TransactionWork<Boolean>() {

            @Override
            public Boolean execute(Transaction transaction) {
                return existNodeCypher(transaction, keyword, label);
            }
        };
    }

    /**
     * this method checks if a node exist
     *
     * @param transaction
     * @param name
     * @param label
     * @return
     */
    private Boolean existNodeCypher(Transaction transaction, String name, String label) {
        String query = MessageFormat.format("Match ({0}:{1}) where {0}.name = $name return ({0})", label.toLowerCase(), label, name);

        Result result = transaction.run(query,
                Values.parameters("name", name));
        if (result.hasNext()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * this method checks whether two nodes (document and catalog entry) are link by edge
     *
     * @param metaData
     * @param catalogEntry
     * @return
     */
    private TransactionWork<Boolean> isLinked(final Metadata metaData, final CatalogEntryHasName catalogEntry) {

        return new TransactionWork<Boolean>() {

            @Override
            public Boolean execute(Transaction transaction) {
                String leftLabel = DBController.DOCUMENT;
                String leftName = metaData.get("name");
                String edgeName = DBController.CONTAINS;
                String rightLabel = (catalogEntry instanceof ALKIS) ? DBController.ALKIS : "";
                rightLabel = (catalogEntry instanceof Term) ? DBController.TERM : "";
                String rightName = catalogEntry.getName();
                return isLinkedCypher(transaction, leftLabel, leftName, edgeName, rightName, rightLabel);
            }
        };
    }

    /**
     * this method checks whether two nodes (document and keyword or ontology) are link by edge
     *
     * @param metaData
     * @param edgeName
     * @param name
     * @param label
     * @return
     */
    private TransactionWork<Boolean> isLinked(final Metadata metaData, String edgeName, final String name, String label) {

        return new TransactionWork<Boolean>() {

            @Override
            public Boolean execute(Transaction transaction) {
                String leftLabel = DBController.DOCUMENT;
                String leftName = metaData.get("name");
                return isLinkedCypher(transaction, leftLabel, leftName, edgeName, name, label);
            }
        };
    }

    /**
     * this method checks whether two nodes (keyword and catalog entry) are link by edge
     *
     * @param keyword
     * @param catalogEntry
     * @return
     */
    private TransactionWork<Boolean> isLinked(final String keyword, final CatalogEntryHasName catalogEntry) {

        return new TransactionWork<Boolean>() {

            @Override
            public Boolean execute(Transaction transaction) {
                String leftLabel = DBController.KEYWORD;
                String leftName = keyword;
                String edgeName = DBController.BELONGSTO;
                String rightName = catalogEntry.getName();
                String rightLabel = (catalogEntry instanceof ALKIS) ? DBController.ALKIS : "";
                rightLabel = (catalogEntry instanceof Term) ? DBController.TERM : "";
                return isLinkedCypher(transaction, leftLabel, leftName, edgeName, rightName, rightLabel);
            }
        };
    }

    /**
     * this method checks whether two nodes are link by edge
     *
     * @param transaction
     * @param leftLabel
     * @param leftName
     * @param edgeName
     * @param rightLabel
     * @param rightName
     * @return
     */
    private Boolean isLinkedCypher(Transaction transaction, String leftLabel, String leftName, String edgeName, String rightName, String rightLabel) {
        String query = MessageFormat
                .format("Match edge=(:{0}'{'name: $leftName'}')-[:{1}]->(:{2}'{'name: $rightName'}') return edge", leftLabel, edgeName, rightLabel);

        Result result = transaction.run(query,
                Values.parameters("leftName", leftName, "rightName", rightName));
        if (result.hasNext()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * this method link two nodes (document and catalog entry)
     *
     * @param metaData
     * @param catalogEntry
     * @return
     */
    private TransactionWork<Void> link(final Metadata metaData, final CatalogEntryHasName catalogEntry) {

        return new TransactionWork<Void>() {

            @Override
            public Void execute(Transaction transaction) {
                String leftLabel = (catalogEntry instanceof ALKIS) ? DBController.ALKIS : "";
                leftLabel = (catalogEntry instanceof Term) ? DBController.TERM : "";
                String leftName = catalogEntry.getName();
                String thereEdgeName = DBController.AFFECT;
                String returnEdgeName = DBController.CONTAINS;
                String rightName = metaData.get("name");
                String rightLabel = DBController.DOCUMENT;
                Double weight = null;
                return linkCypher(transaction, leftLabel, leftName, thereEdgeName, returnEdgeName, rightName, rightLabel, weight);
            }
        };
    }

    /**
     * this method link two nodes (document and ontology)t
     *
     * @param metaData
     * @param keyword
     * @param weight
     * @return
     */
    private TransactionWork<Void> link(final Metadata metaData, final String ontology) {

        return new TransactionWork<Void>() {

            @Override
            public Void execute(Transaction transaction) {
                String leftLabel = DBController.DOCUMENT;
                String leftName = metaData.get("name");
                String thereEdgeName = DBController.BELONGSTO;
                String returnEdgeName = DBController.AFFECT;
                String rightName = ontology;
                String rightLabel = DBController.ONTOLOGY;
                return linkCypher(transaction, leftLabel, leftName, thereEdgeName, returnEdgeName, rightName, rightLabel, null);
            }
        };
    }

    /**
     * this method link two nodes (document and keyword) with weight
     *
     * @param metaData
     * @param keyword
     * @param weight
     * @return
     */
    private TransactionWork<Void> link(final Metadata metaData, final String keyword, final double weight) {

        return new TransactionWork<Void>() {

            @Override
            public Void execute(Transaction transaction) {
                String leftLabel = DBController.DOCUMENT;
                String leftName = metaData.get("name");
                String thereEdgeName = DBController.CONTAINS;
                String returnEdgeName = DBController.AFFECT;
                String rightName = keyword;
                String rightLabel = DBController.KEYWORD;
                return linkCypher(transaction, leftLabel, leftName, thereEdgeName, returnEdgeName, rightName, rightLabel, weight);
            }
        };
    }

    /**
     * this method link two nodes (keyword and catalog entry)
     *
     * @param keyword
     * @param catalogEntry
     * @return
     */
    private TransactionWork<Void> link(final String keyword, final CatalogEntryHasName catalogEntry) {

        return new TransactionWork<Void>() {

            @Override
            public Void execute(Transaction transaction) {
                String leftLabel = DBController.KEYWORD;
                String leftName = keyword;
                String edgeName = DBController.BELONGSTO;
                String rightName = catalogEntry.getName();
                String rightLabel = (catalogEntry instanceof ALKIS) ? DBController.ALKIS : "";
                rightLabel = (catalogEntry instanceof Term) ? DBController.TERM : "";
                return linkCypher(transaction, leftLabel, leftName, edgeName, null, rightName, rightLabel, null);
            }
        };
    }

    /**
     * this method link two nodes
     *
     * @param transaction
     * @param leftLabel
     * @param leftName
     * @param thereEdgeName
     * @param returnEdgeName
     * @param rightName
     * @param rightLabel
     * @param weight
     * @return
     */
    private Void linkCypher(Transaction transaction, String leftLabel, String leftName, String thereEdgeName, @Nullable String returnEdgeName, String rightName,
            String rightLabel, @Nullable Double weight) {

        Map<String, Object> parameters = new HashMap<String, Object>();

        parameters.put("leftName", leftName);
        parameters.put("rightName", rightName);

        String weightProperty = (weight == null) ? "" : "weight:$weight";

        String nodeQuery = MessageFormat
                .format("Match (leftNode:{0}), (rightNode:{1}) where leftNode.name = $leftName and rightNode.name = $rightName", leftLabel, rightLabel);

        String thereEdgeNameQuery = MessageFormat.format(" Create (leftNode)-[:{0}] ->(rightNode)", thereEdgeName);
        if (weight != null) {
            thereEdgeNameQuery = MessageFormat.format(" Create (leftNode)-[:{0}'{'{1}'}'] ->(rightNode)", thereEdgeName, weightProperty);
            parameters.put("weight", weight);
        }
        String returnEdgeNameQuery = "";
        if (returnEdgeName != null) {
            returnEdgeNameQuery = MessageFormat.format(" Create (rightNode)-[:{0}] ->(leftNode)", returnEdgeName);
        }
        String query = nodeQuery + thereEdgeNameQuery + returnEdgeNameQuery;

        transaction.run(query, parameters);
        return null;
    }

    /**
     * this method build a graph only an example
     *
     * @param keyword
     * @param metadata
     * @param catalogEntry
     * @param weigth
     */
    public void persist(String keyword, Metadata metadata, @Nullable CatalogEntryHasName catalogEntry, Double weigth) {

        try(Session session = driver.session()) {

            Boolean exist = false;
            Boolean isLinked = false;

            // document
            exist = session.readTransaction(existNode(metadata));
            if (!exist) {
                session.writeTransaction(createNode(metadata));
            }

            exist = session.readTransaction(existNode(keyword, DBController.KEYWORD));
            if (!exist) {
                session.writeTransaction(createNode(keyword, DBController.KEYWORD));
            }
            // link keyword and document
            isLinked = session.readTransaction(isLinked(metadata, DBController.CONTAINS, keyword, DBController.KEYWORD));
            if (!isLinked) {
                session.writeTransaction(link(metadata, keyword, weigth));
            }
            if (catalogEntry == null) {
                return;
            }

            // ALKIS or Term
            exist = session.readTransaction(existNode(catalogEntry));
            if (!exist) {
                session.writeTransaction(createNode(catalogEntry));
            }
            isLinked = session.readTransaction(isLinked(metadata, catalogEntry));
            if (!isLinked) {
                session.writeTransaction(link(metadata, catalogEntry));
            }
            // keyword and ALKIS or Term
            isLinked = session.readTransaction(isLinked(keyword, catalogEntry));
            if (!isLinked) {
                session.writeTransaction(link(keyword, catalogEntry));
            }
            // ontology
            if (catalogEntry instanceof Term) {
                Term term = (Term)catalogEntry;
                for (String ontology : term.getOntologies()) {
                    exist = session.readTransaction(existNode(ontology, DBController.ONTOLOGY));
                    if (!exist) {
                        session.writeTransaction(createNode(ontology, DBController.ONTOLOGY));
                    }

                    isLinked = session.readTransaction(isLinked(metadata, DBController.BELONGSTO, ontology, DBController.ONTOLOGY));
                    if (!isLinked) {
                        session.writeTransaction(link(metadata, ontology));
                    }
                }
            }

        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }

    }

    /**
     * this method purge db
     */
    public void purgeDB() {
        try(Session session = driver.session()) {

            Transaction transaction = session.beginTransaction();

            transaction.run("MATCH (n) DETACH DELETE n");
            transaction.commit();
            transaction.close();

        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }
    }

    /**
     * checks passed properties
     *
     * @param properties
     * @return
     * @throws IOException
     */
    private Boolean validateProperties(Properties properties) throws IOException {

        String value = (String)properties.get("neo4j.uri");
        if (value == null) {
            throw new IOException("set property --> neo4j.uri as String");
        }
        value = (String)properties.get("neo4j.user");
        if (value == null) {
            throw new IOException("set property --> neo4j.user as String");
        }
        value = (String)properties.get("neo4j.password");
        if (value == null) {
            throw new IOException("set property --> neo4j.password as String");
        }

        return true;
    }

}
