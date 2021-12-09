package de.citytwin.database;

import de.citytwin.catalog.CatalogEntryHasName;
import de.citytwin.config.ApplicationConfiguration;
import de.citytwin.model.ALKIS;
import de.citytwin.model.Term;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.tika.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Maik Siegmund, FH Erfurt
 */
public class PostgreSQLController implements AutoCloseable {

    private static transient final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static transient final String POSTGRECLASSNAME = "org.postgresql.Driver";

    private String postgreUrl = null;
    private String path2PostgrePropertieFile = null;
    private Properties postgreSQLPorperties = null;

    /** default values */
    public static final String TABEL_ALKIS = "table.alkis";
    public static final String TABEL_DOCUMENT = "node.documen";
    public static final String TABEL_KEYWORD = "node.keyword";
    public static final String TABEL_ONTOLOGY = "node.ontology";
    public static final String TABEL_TERM = "node.term";
    public static final String RELATION_AFFECT = "edge.affect";
    public static final String RELATION_BELONGSTO = "edge.belongsTo";
    public static final String RELATION_CONTAINS = "edge.contains";

    public PostgreSQLController(final Properties properties) throws IOException {
        if (validateProperties(properties)) {
            postgreSQLPorperties = new Properties();
            try(InputStream inputStream = new FileInputStream(path2PostgrePropertieFile)) {
                postgreSQLPorperties.load(inputStream);
            }

        }
    }

    private Connection getConnection() throws ClassNotFoundException, SQLException {
        Class.forName(POSTGRECLASSNAME);
        return DriverManager.getConnection(postgreUrl, postgreSQLPorperties);

    }

    private Boolean validateProperties(final Properties properties) {

        postgreUrl = properties.getProperty(ApplicationConfiguration.POSTGRESQL_URL);
        if (postgreUrl == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.POSTGRESQL_URL);
        }
        path2PostgrePropertieFile = properties.getProperty(ApplicationConfiguration.PATH_2_POSTGRESQL_PROPERTY_FILE);
        if (path2PostgrePropertieFile == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.PATH_2_POSTGRESQL_PROPERTY_FILE);
        }

        return true;
    }

    public static Properties getDefaultProperties() {
        Properties properties = new Properties();
        properties.setProperty(ApplicationConfiguration.PATH_2_POSTGRESQL_PROPERTY_FILE, "jdbc:postgresql://localhost/test");
        properties.setProperty(ApplicationConfiguration.PATH_2_POSTGRESQL_PROPERTY_FILE, "postgreSQL.properties");
        return properties;
    }

    public long insert(Metadata metadata) throws ClassNotFoundException, SQLException {

        String tabel = "documents";
        Map<String, Object> parameters = new HashMap<String, Object>();

        for (String name : metadata.names()) {
            String cleaned = name.replace("-", "");
            cleaned = cleaned.replace(":", "");
            cleaned = cleaned.replace("-", "");
            parameters.put(cleaned, metadata.get(name));
        }
        return insertStatement(tabel, parameters);

    }

    /**
     * this method insert specific name in a specific tabel
     *
     * @param name
     * @param tabel
     * @return
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public long insert(String name, String tabel) throws ClassNotFoundException, SQLException {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(name, name);
        return insertStatement(tabel, parameters);
    }

    /**
     * this method insert Term or ALKIS
     *
     * @param catalogEntry
     * @return
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public long insert(CatalogEntryHasName catalogEntry) throws ClassNotFoundException, SQLException {
        String tabel = "";
        Map<String, Object> parameters = new HashMap<String, Object>();
        if (catalogEntry instanceof Term) {
            tabel = "Term";
            parameters.put("name", catalogEntry.getName());
            parameters.put("morphem", ((Term)catalogEntry).getMorphem());
            parameters.put("isCore", ((Term)catalogEntry).getIsCore());
        }
        if (catalogEntry instanceof ALKIS) {
            tabel = "ALKIS";
            parameters.put("name", catalogEntry.getName());
            parameters.put("categorie", ((ALKIS)catalogEntry).getCategorie());
            parameters.put("code", ((ALKIS)catalogEntry).getCode());
        }
        return insertStatement(tabel, parameters);
    }

    public void link(String keyord, Metadata metadata) {

    }

    @Override
    public void close() throws Exception {

    }

    /**
     * this method create insert statement and perform possible parameter types Integer, Double, Float, Date, Boolean last String
     *
     * @param tabel
     * @param parameters
     * @return new id or 0
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    private long insertStatement(String tabel, Map<String, Object> parameters) throws ClassNotFoundException, SQLException {

        String sqlStatement = "";
        String values = "";
        String questionMark = "";
        StringBuilder valuesNames = new StringBuilder();
        StringBuilder questionMarks = new StringBuilder();

        // create statement
        for (String key : parameters.keySet()) {
            valuesNames.append(MessageFormat.format("{0},", key));
            questionMarks.append(MessageFormat.format("{0},", "?"));
        }
        // remove last comma
        values = valuesNames.toString();
        values = values.substring(0, values.length() - 1);
        questionMark = questionMarks.toString();
        questionMark = questionMark.substring(0, questionMark.length() - 1);
        sqlStatement = (MessageFormat.format("INSERT INTO {0}({1}) VALUES({2}))", tabel, values, questionMark));
        try(Connection connection = getConnection()) {

            PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement, Statement.RETURN_GENERATED_KEYS);
            // Assign data
            int index = 0;
            for (String key : parameters.keySet()) {

                Object data = parameters.get(key);
                if (data instanceof Integer) {
                    preparedStatement.setInt(index++, (Integer)data);
                    continue;
                }
                if (data instanceof Double) {
                    preparedStatement.setDouble(index++, (Double)data);
                    continue;
                }
                if (data instanceof Float) {
                    preparedStatement.setDouble(index++, (Float)data);
                    continue;
                }
                if (data instanceof Date) {
                    preparedStatement.setDate(index++, (Date)data);
                    continue;
                }
                if (data instanceof Boolean) {
                    preparedStatement.setBoolean(index++, (Boolean)data);
                    continue;
                }
                preparedStatement.setString(index++, data.toString());
            }
            // perform statement
            if (preparedStatement.executeUpdate() > 0) {
                ResultSet resultSet = preparedStatement.getGeneratedKeys();
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
            }
        }

        return 0;
    }

    public Boolean exist(String keyword) throws ClassNotFoundException, SQLException {
        return existStatement("Keywords", keyword);
    }

    public Boolean exist(CatalogEntryHasName catalogEntryHasName) throws ClassNotFoundException, SQLException {
        String tabel = "";
        String name = "";
        if (catalogEntryHasName instanceof Term) {
            tabel = "Term";
        }
        if (catalogEntryHasName instanceof ALKIS) {
            tabel = "ALKIS";
        }
        name = catalogEntryHasName.getName();
        return existStatement(tabel, name);
    }

    private Boolean existStatement(String tabel, String name) throws ClassNotFoundException, SQLException {

        String sqlStatement = MessageFormat.format("SELECT {0} FROM {1} WHERE Name = ?", name.toUpperCase(), tabel.toUpperCase());
        try(Connection connection = getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
            preparedStatement.setString(1, name);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                return true;
            }
            return false;
        }

    }

    private Boolean createTabelStatement(String tabel, List<String> parameters) throws ClassNotFoundException, SQLException {

        StringBuilder stringBuilder = new StringBuilder();
        for (String parameter : parameters) {
            stringBuilder.append(parameter);
        }
        String sqlStatement = stringBuilder.toString();
        sqlStatement = sqlStatement.substring(0, sqlStatement.length() - 1);
        sqlStatement = MessageFormat.format("CREATE TABLE IF NOT EXISTS {0} ({1});", tabel.toLowerCase(), sqlStatement);
        try(Connection connection = getConnection()) {
            Statement statement = connection.createStatement();
            return statement.execute(sqlStatement);
        }

    }

    public Boolean createKeywordTabel() throws ClassNotFoundException, SQLException {
        String tabel = "keywords";
        List<String> parameters = new ArrayList<String>();
        parameters.add("ID  BIGSERIAL PRIMARY KEY");
        parameters.add("Name VARCHAR (50) UNIQUE");
        return createTabelStatement(tabel, parameters);
    }

    public Boolean createALKISTabel() throws ClassNotFoundException, SQLException {
        String tabel = "ALKIS";
        List<String> parameters = new ArrayList<String>();
        parameters.add("ID BIGSERIAL PRIMARY KEY");
        parameters.add("Name VARCHAR (50) UNIQUE");
        parameters.add("Categorie VARCHAR (50)");
        parameters.add("Code INTEGER");
        return createTabelStatement(tabel, parameters);
    }

    public Boolean createTermTabel() throws ClassNotFoundException, SQLException {
        String tabel = "Terms";
        List<String> parameters = new ArrayList<String>();
        parameters.add("ID BIGSERIAL PRIMARY KEY");
        parameters.add("Name VARCHAR (50) UNIQUE");
        parameters.add("IsCore BOOLEAN");
        parameters.add("Morphem VARCHAR (50)");
        return createTabelStatement(tabel, parameters);
    }

    public Boolean createDocumentTabel() throws ClassNotFoundException, SQLException {
        String tabel = "Documents";
        List<String> parameters = new ArrayList<String>();
        parameters.add("ID BIGSERIAL PRIMARY KEY");
        parameters.add("Name VARCHAR (50) UNIQUE");
        parameters.add("From DATE");
        return createTabelStatement(tabel, parameters);
    }

}
