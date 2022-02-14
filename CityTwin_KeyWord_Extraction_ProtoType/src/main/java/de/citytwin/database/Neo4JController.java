package de.citytwin.database;

import com.beust.jcommander.internal.Nullable;

import de.citytwin.catalog.CatalogEntryHasName;
import de.citytwin.config.ApplicationConfiguration;
import de.citytwin.model.ALKIS;
import de.citytwin.model.Location;
import de.citytwin.model.Term;

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
 */
public class Neo4JController implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /** default values */
    public static final String NODE_ALKIS = "node.alkis";
    public static final String NODE_DOCUMENT = "node.documen";
    public static final String NODE_KEYWORD = "node.keyword";
    public static final String NODE_ONTOLOGY = "node.ontology";
    public static final String NODE_TERM = "node.term";
    public static final String NODE_LOCATION = "node.location";
    public static final String EDGE_AFFECT = "edge.affect";
    public static final String EDGE_BELONGSTO = "edge.belongsTo";
    public static final String EDGE_CONTAINS = "edge.contains";

    public static Properties getDefaultProperties() {
        Properties properties = new Properties();
        properties.setProperty(ApplicationConfiguration.NEO4J_URI, "bolt://localhost:7687");
        properties.setProperty(ApplicationConfiguration.NEO4J_USER, "neo4j");
        properties.setProperty(ApplicationConfiguration.NEO4J_PASSWORD, "C1tyTw1n!");
        // optional properties
        properties.setProperty(Neo4JController.NODE_ALKIS, "ALKIS");
        properties.setProperty(Neo4JController.NODE_DOCUMENT, "Document");
        properties.setProperty(Neo4JController.NODE_KEYWORD, "Keyword");
        properties.setProperty(Neo4JController.NODE_ONTOLOGY, "Ontology");
        properties.setProperty(Neo4JController.NODE_LOCATION, "Location");
        properties.setProperty(Neo4JController.NODE_TERM, "Term");
        properties.setProperty(Neo4JController.EDGE_AFFECT, "affect");
        properties.setProperty(Neo4JController.EDGE_BELONGSTO, "belongsTo");
        properties.setProperty(Neo4JController.EDGE_CONTAINS, "contains");

        return properties;

    }

    private Driver driver;
    private Session session;
    private String uri = null;
    private String user = null;
    private String password = null;

    private String nodeALKIS = "ALKIS";
    private String nodeDocument = "Document";
    private String nodeKeyword = "Keyword";
    private String nodeOntology = "Ontology";
    private String nodeTerm = "Term";
    private String nodeLocation = "Location";
    private String edgeAffect = "affect";
    private String edgeBelongsTo = "belongsTo";
    private String edgeContains = "contains";

    /**
     * constructor.
     *
     * @param properties
     * @throws IOException
     */
    public Neo4JController(Properties properties) throws IOException {

        if (validateProperties(properties)) {
            String uri = (String)properties.get(ApplicationConfiguration.NEO4J_URI);
            String user = (String)properties.get(ApplicationConfiguration.NEO4J_USER);
            String password = (String)properties.get(ApplicationConfiguration.NEO4J_PASSWORD);
            driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
            setOptionalProperties(properties);
        }

    }

    /**
     * this method build a part of city graph
     *
     * @param metadata
     * @param keyword
     * @param catalogEntry
     * @param weigth
     */
    public void buildGraph(Metadata metadata, String keyword, @Nullable CatalogEntryHasName catalogEntry, Double weigth) {

        try(Session session = driver.session()) {

            Boolean exist = false;
            Boolean isLinked = false;

            // document
            exist = session.readTransaction(existNode(metadata));
            if (!exist) {
                session.writeTransaction(createNode(metadata));
            }

            exist = session.readTransaction(existNode(keyword, nodeKeyword));
            if (!exist) {
                session.writeTransaction(createNode(keyword, nodeKeyword));
            }
            // link keyword and document
            isLinked = session.readTransaction(isLinked(metadata, edgeContains, keyword, nodeKeyword));
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
                    exist = session.readTransaction(existNode(ontology, nodeOntology));
                    if (!exist) {
                        session.writeTransaction(createNode(ontology, nodeOntology));
                    }

                    isLinked = session.readTransaction(isLinked(metadata, edgeBelongsTo, ontology, nodeOntology));
                    if (!isLinked) {
                        session.writeTransaction(link(metadata, ontology));
                    }
                }
            }

        } catch (Exception exception) {
            LOGGER.error(exception.getMessage(), exception);
        }

    }

    /**
     * this method build a part of citygraph
     *
     * @param metadata
     * @param location
     */
    public void buildGraph(Metadata metadata, Location location) {
        try(Session session = driver.session()) {
            boolean exist = false;
            boolean linked = false;

            // document
            exist = session.readTransaction(existNode(metadata));
            if (!exist) {
                session.writeTransaction(createNode(metadata));
            }
            // location
            exist = session.readTransaction(existNode(location));
            if (!exist) {
                session.writeTransaction(createNode(location));
            }
            linked = session.readTransaction(isLinked(metadata, location));
            if (!linked) {
                session.writeTransaction(link(metadata, location));
            }

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
                    label = nodeALKIS;
                }
                if (catalogEntry instanceof Term) {
                    Term term = (Term)catalogEntry;
                    parameters.put("name", term.getName());
                    parameters.put("morphem", term.getMorphem());
                    parameters.put("isCore", term.getIsCore());
                    label = nodeTerm;
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
                String label = nodeDocument;
                Map<String, Object> parameters = new HashMap<String, Object>();

                for (String name : metadata.names()) {
                    String cleaned = name.replace("-", "");
                    cleaned = cleaned.replace(":", "");
                    cleaned = cleaned.replace("-", "");
                    parameters.put(cleaned, metadata.get(name));
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
    private TransactionWork<Void> createNode(Location location) {
        return new TransactionWork<Void>() {

            @Override
            public Void execute(Transaction transaction) {
                String label = nodeDocument;
                Map<String, Object> parameters = new HashMap<String, Object>();
                parameters.put("name", location.getName());
                parameters.put("featureCode", location.getFeatureCode());
                parameters.put("latitude", location.getLatitude());
                parameters.put("longitude", location.getLongitude());
                parameters.put("longitude", location.getLongitude());
                parameters.put("synonyms", String.join(",", location.getSynonyms()));
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
     * this method create node
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
        String comma = ",";
        // Map<String, Object> parameters = new HashMap<String, Object>();

        MessageFormat.format("{0}: ${0}", name);
        int index = 0;
        for (String key : parameters.keySet()) {
            name = key;
            comma = (index == parameters.size() - 1) ? " " : ", ";
            stringBuilder.append(MessageFormat.format("{0}: ${0}{1} ", name, comma));
            index++;
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
            return existNode(catalogEntry.getName(), nodeTerm);
        }
        if (catalogEntry instanceof ALKIS) {
            return existNode(catalogEntry.getName(), nodeALKIS);
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
        return existNode(metadata.get("name"), nodeDocument);
    }

    private TransactionWork<Boolean> existNode(String keyword, String label) {

        return new TransactionWork<Boolean>() {

            @Override
            public Boolean execute(Transaction transaction) {
                return existNodeCypher(transaction, keyword, label);
            }
        };
    }

    private TransactionWork<Boolean> existNode(Location location) {

        return new TransactionWork<Boolean>() {

            @Override
            public Boolean execute(Transaction transaction) {
                Map<String, Object> parameters = new HashMap<String, Object>();
                parameters.put("name", location.getName());
                parameters.put("longitude", location.getLongitude());
                parameters.put("latitude", location.getLatitude());
                return existNodeCypher(transaction, parameters, nodeLocation);
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

    private Boolean existNodeCypher(Transaction transaction, Map<String, Object> parameters, String label) {

        StringBuilder andConditions = new StringBuilder();
        String and = "and";
        int index = 0;
        for (String key : parameters.keySet()) {
            and = (index == parameters.size() - 1) ? " " : "and ";
            andConditions.append(MessageFormat.format("{0}.{1} = ${1} {2}", label, key.toLowerCase(), key, and));
        }
        String query = MessageFormat.format("Match ({0}:{1}) where {2} return ({0})", label.toLowerCase(), label, andConditions.toString());
        Result result = transaction.run(query, parameters);
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
                String leftLabel = nodeDocument;
                String leftName = metaData.get("name");
                String edgeName = edgeContains;
                String rightLabel = (catalogEntry instanceof ALKIS) ? nodeALKIS : "";
                rightLabel = (catalogEntry instanceof Term) ? nodeTerm : rightLabel;
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
                String leftLabel = nodeDocument;
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
                String leftLabel = nodeKeyword;
                String leftName = keyword;
                String edgeName = edgeBelongsTo;
                String rightName = catalogEntry.getName();
                String rightLabel = (catalogEntry instanceof ALKIS) ? nodeALKIS : "";
                rightLabel = (catalogEntry instanceof Term) ? nodeTerm : rightLabel;
                return isLinkedCypher(transaction, leftLabel, leftName, edgeName, rightName, rightLabel);
            }
        };
    }

    private TransactionWork<Boolean> isLinked(final Metadata metadata, final Location location) {
        return new TransactionWork<Boolean>() {

            @Override
            public Boolean execute(Transaction transaction) {
                return isLinkedCypher(transaction, metadata, location);
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
    private Boolean isLinkedCypher(Transaction transaction, Metadata metadata, Location location) {

        String leftName = metadata.get("name");
        String leftLabel = nodeDocument;
        String rightName = location.getName();
        String rightLabel = nodeLocation;
        String edgeName = edgeContains;

        String query = MessageFormat
                .format("Match edge=(:{0}'{'name: $leftName'}')-[:{1}]->(:{2}'{'name: $rightName , latitude: $latitude, longitude: $longitude'}') return edge",
                        leftLabel,
                        edgeName,
                        rightLabel);

        Result result = transaction.run(query,
                Values.parameters("leftName", leftName, "rightName", rightName, "latitude", location.getLatitude(), "longitude", location.getLongitude()));
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
                String leftLabel = (catalogEntry instanceof ALKIS) ? nodeALKIS : "";
                leftLabel = (catalogEntry instanceof Term) ? nodeTerm : leftLabel;
                String leftName = catalogEntry.getName();
                String thereEdgeName = edgeAffect;
                String returnEdgeName = edgeContains;
                String rightName = metaData.get("name");
                String rightLabel = nodeDocument;
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
                String leftLabel = nodeDocument;
                String leftName = metaData.get("name");
                String thereEdgeName = edgeBelongsTo;
                String returnEdgeName = edgeAffect;
                String rightName = ontology;
                String rightLabel = nodeOntology;
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
                String leftLabel = nodeDocument;
                String leftName = metaData.get("name");
                String thereEdgeName = edgeContains;
                String returnEdgeName = edgeAffect;
                String rightName = keyword;
                String rightLabel = nodeKeyword;
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
                String leftLabel = nodeKeyword;
                String leftName = keyword;
                String edgeName = edgeBelongsTo;
                String rightName = catalogEntry.getName();
                String rightLabel = (catalogEntry instanceof ALKIS) ? nodeALKIS : "";
                rightLabel = (catalogEntry instanceof Term) ? nodeTerm : rightLabel;
                return linkCypher(transaction, leftLabel, leftName, edgeName, null, rightName, rightLabel, null);
            }
        };
    }

    private TransactionWork<Void> link(final Metadata metadata, final Location location) {
        return new TransactionWork<Void>() {

            @Override
            public Void execute(Transaction transaction) {
                return linkCypher(transaction, metadata, location);
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
     * this method link metadata/document and location
     *
     * @param transaction
     * @param metadata
     * @param location
     * @return
     */
    private Void linkCypher(Transaction transaction, Metadata metadata, Location location) {

        String leftLabel = nodeDocument;
        String rightLabel = nodeLocation;
        String thereEdgeName = edgeContains;
        String returnEdgeName = edgeAffect;
        Map<String, Object> parameters = new HashMap<String, Object>();

        parameters.put("leftName", metadata.get("name"));
        parameters.put("rightName", location.getName());
        parameters.put("longitude", location.getLongitude());
        parameters.put("latitude", location.getLatitude());

        String nodeQuery = MessageFormat
                .format("Match (leftNode:{0}), (rightNode:{1}) where leftNode.name = $leftName and rightNode.name = $rightName and rightNode.longitude = $longitude and rightNode.latitude = $latitude",
                        leftLabel,
                        rightLabel);

        String thereEdgeNameQuery = MessageFormat.format(" Create (leftNode)-[:{0}] ->(rightNode)", thereEdgeName);
        String returnEdgeNameQuery = MessageFormat.format(" Create (rightNode)-[:{0}] ->(leftNode)", returnEdgeName);
        String query = nodeQuery + thereEdgeNameQuery + returnEdgeNameQuery;

        transaction.run(query, parameters);
        return null;
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
            LOGGER.error(exception.getMessage(), exception);
        }
    }

    /**
     * this method overwrite default values, optional
     *
     * @param properties
     */
    private void setOptionalProperties(Properties properties) {

        nodeALKIS = properties.getProperty(Neo4JController.NODE_ALKIS, "ALKIS");
        nodeDocument = properties.getProperty(Neo4JController.NODE_DOCUMENT, "Document");
        nodeKeyword = properties.getProperty(Neo4JController.NODE_KEYWORD, "Keyword");
        nodeOntology = properties.getProperty(Neo4JController.NODE_ONTOLOGY, "Ontology");
        nodeTerm = properties.getProperty(Neo4JController.NODE_TERM, "Term");
        edgeAffect = properties.getProperty(Neo4JController.EDGE_AFFECT, "affect");
        edgeBelongsTo = properties.getProperty(Neo4JController.EDGE_BELONGSTO, "belongsTo");
        edgeContains = properties.getProperty(Neo4JController.EDGE_CONTAINS, "contains");

    }

    /**
     * checks passed properties and set them
     *
     * @param properties
     * @return
     * @throws IllegalArgumentException
     */
    private Boolean validateProperties(Properties properties) throws IllegalArgumentException {

        uri = properties.getProperty(ApplicationConfiguration.NEO4J_URI);
        if (uri == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.NEO4J_URI);
        }
        user = properties.getProperty(ApplicationConfiguration.NEO4J_USER);
        if (user == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.NEO4J_USER);
        }
        password = properties.getProperty(ApplicationConfiguration.NEO4J_PASSWORD);
        if (password == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.NEO4J_PASSWORD);
        }
        return true;
    }

}