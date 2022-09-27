package de.citytwin.database;

import de.citytwin.catalog.HasName;
import de.citytwin.config.ApplicationConfiguration;
import de.citytwin.model.ALKIS;
import de.citytwin.model.Address;
import de.citytwin.model.Keyword;
import de.citytwin.model.Location;
import de.citytwin.model.Ontology;
import de.citytwin.model.Term;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.sql.Array;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.tika.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Maik Siegmund, FH Erfurt
 */
public class PostgreSQLController implements AutoCloseable {

    private static transient final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static transient final String POSTGRECLASSNAME = "org.postgresql.Driver";

    /** default values */
    public static final String TABLE_ALKIS = "nlp_alkis";
    public static final String TABLE_DOCUMENTS = "nlp_documents";
    public static final String TABLE_KEYWORDS = "nlp_keywords";

    public static final String TABLE_ONTOLOGIES = "nlp_ontologies";
    public static final String TABLE_TERMS = "nlp_terms";
    public static final String TABLE_LOCATIONS = "nlp_locations";
    public static final String TABLE_ADDRESSES = "fis_s_rbs_adr";
    // todo add to application config
    public static final String TABLE_SECTIONS = "fis_s_wfs_alkis_bezirk";
    public static final String TABLE_DISTRICTS = "fis_s_wfs_alkis_ortsteile";
    public static final String MAPPING_TABLE_DOCUMENTS_ALKIS = "nlp_documents_alkis";
    public static final String MAPPING_TABLE_DOCUMENTS_KEYWORDS = "nlp_documents_keywords";
    public static final String MAPPING_TABLE_DOCUMENTS_TERMS = "nlp_documents_terms";

    public static final String MAPPING_TABLE_DOCUMENTS_LOCATIONS = "nlp_documents_locations";
    public static final String MAPPING_TABLE_DOCUMENTS_ONTOLOGIES = "nlp_documents_ontologies";
    public static final String MAPPING_TABLE_DOCUMENTS_ADDRESSES = "nlp_documents_addresses";
    public static final String MAPPING_TABLE_TERMS_ONTOLOGIES = "nlp_terms_ontologies";
    public static final String SCHEMA = "public";
    public static final String VIEW_LOCATION = "vnlp_locations_berlin";

    public static Properties getDefaultProperties() {
        Properties properties = new Properties();
        properties.setProperty(ApplicationConfiguration.PATH_2_POSTGRESQL_PROPERTY_FILE, "postgreSQL.properties");
        properties.setProperty(ApplicationConfiguration.POSTGRESQL_URI, "jdbc:postgresql://localhost/database");
        return properties;
    }

    private String postgreUrl = null;
    private String path2PostgrePropertieFile = null;
    private Properties postgreSQLPorperties = null;
    private Connection connection = null;

    /**
     * constructor.
     *
     * @param properties
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public PostgreSQLController(final Properties properties) throws IOException, ClassNotFoundException, SQLException {
        if (validateProperties(properties)) {
            postgreSQLPorperties = new Properties();
            try(InputStream inputStream = new FileInputStream(path2PostgrePropertieFile)) {
                postgreSQLPorperties.load(inputStream);
            }
            connect();
        }
    }

    @Override
    public void close() throws Exception {
        if (connection == null) {
            return;
        }
        if (!connection.isClosed()) {
            connection.close();
        }
    }

    /**
     * this method create a mapping table (N:M)
     *
     * @param mappingTableName
     * @return
     * @throws SQLException
     */
    public Boolean createMappingTable(String mappingTableName) throws SQLException {
        String[] tabels = mappingTableName.split("_");
        String tableLeft = MessageFormat.format("nlp_{0}", tabels[1]);
        String tableRight = MessageFormat.format("nlp_{0}", tabels[2]);
        if (tabels[2].equals("addresses")) {
            tableRight = PostgreSQLController.TABLE_ADDRESSES;
        }
        List<String> parameters = createMappingTableParameters(tableLeft, tableRight);
        if (PostgreSQLController.MAPPING_TABLE_DOCUMENTS_KEYWORDS.equals(mappingTableName)) {
            parameters.add("WEIGHT DOUBLE PRECISION DEFAULT 0.0::double precision");
            parameters.add("textpassage text[]");
        }
        return createTable(mappingTableName, parameters);
    }

    /**
     * this method create ddl (data definition language) parameters for a mapping table
     *
     * @param tabelLeft
     * @param tabelRight
     * @return
     */
    private List<String> createMappingTableParameters(String tabelLeft, String tabelRight) {
        String idRowNameLeft = (tabelLeft.equals(PostgreSQLController.TABLE_ADDRESSES)) ? "fid" : "id";
        String idRowNameRight = (tabelRight.equals(PostgreSQLController.TABLE_ADDRESSES)) ? "fid" : "id";
        List<String> parameters = new ArrayList<String>();
        parameters.add(MessageFormat.format("{0}_{1} BIGINT REFERENCES {2}.{0}({1}) ON DELETE CASCADE", tabelLeft, idRowNameLeft, PostgreSQLController.SCHEMA));
        parameters
                .add(MessageFormat.format("{0}_{1} BIGINT REFERENCES {2}.{0}({1}) ON DELETE CASCADE", tabelRight, idRowNameRight, PostgreSQLController.SCHEMA));
        parameters.add(MessageFormat.format("CONSTRAINT {0}_{1}_pkey PRIMARY KEY ({0}_{2},{1}_{3})", tabelLeft, tabelRight, idRowNameLeft, idRowNameRight));
        return parameters;
    }

    /**
     * this method create a table
     *
     * @param tableName
     * @return
     * @throws SQLException
     */
    public Boolean createTable(String tableName) throws SQLException {
        List<String> parameters = getTableParameters(tableName);
        return createTable(tableName, parameters);
    }

    /**
     * this method create a table
     *
     * @param table
     * @param parameters
     * @return
     * @throws SQLException
     */
    private Boolean createTable(String table, List<String> parameters) throws SQLException {

        StringBuilder stringBuilder = new StringBuilder();
        for (String parameter : parameters) {
            stringBuilder.append(parameter + ",");
        }
        String sqlStatement = stringBuilder.toString();
        sqlStatement = sqlStatement.substring(0, sqlStatement.length() - 1);
        sqlStatement = MessageFormat.format("CREATE TABLE IF NOT EXISTS {0}.{1} ({2})", PostgreSQLController.SCHEMA, table.toLowerCase(), sqlStatement);
        Statement statement = connection.createStatement();
        return statement.execute(sqlStatement);
    }

    /**
     * this method perform delete statement on a given table, use careful !!! if parameter condition notnull, keyword <strong>where </strong> will append by
     * this method.
     *
     * @param table
     * @param condition (e.g) id = 1 and name = "...."
     * @return affected rows
     * @throws SQLException
     */
    private int deleteStatement(String table, @javax.annotation.Nullable String condition) throws SQLException {
        String sqlStatement = (condition == null) ? MessageFormat.format("DELETE FROM {0}", table)
                : MessageFormat.format("DELETE FROM {0}.{1} where {2}", table, PostgreSQLController.SCHEMA, condition);
        Statement statement = connection.createStatement();
        return statement.executeUpdate(sqlStatement);

    }

    /**
     * this method return address
     *
     * @return
     */
    private String selectAddressStatement() {

        String strName = "strnam";
        String hausNr = "hausnr";
        String hausNrZ = "hausnrz";
        String bez_Name = "bez_name";
        String featureId = "feature_id"; // origin ID

        String sqlStatement = MessageFormat
                .format("SELECT {0}, {1}, {2}, {3}, {4} from {5}.{6} where feature_id = ?",
                        strName,
                        hausNr,
                        hausNrZ,
                        bez_Name,
                        featureId,
                        PostgreSQLController.SCHEMA,
                        PostgreSQLController.TABLE_ADDRESSES);

        return sqlStatement;
    }

    /**
     * this method get address from db
     *
     * @param id
     * @return
     * @throws SQLException
     */
    public Address getAddress(String featureId) throws SQLException {

        Address address = null;

        String strName = "strnam";
        String hausNr = "hausnr";
        String hausNrZ = "hausnrz";
        String bez_Name = "bez_name";
        String feature_id = "feature_id";

        String sqlStatement = selectAddressStatement();

        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        preparedStatement.setString(1, featureId);

        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {

            address = new Address(
                    resultSet.getString(strName),
                    resultSet.getDouble(hausNr),
                    resultSet.getString(hausNrZ),
                    resultSet.getString(bez_Name),
                    resultSet.getString(feature_id));

        }
        return address;
    }

    /**
     * this method get a ALKIS form db
     *
     * @param id
     * @return new reference of ALKIS or null
     * @throws SQLException
     */
    public ALKIS getALKIS(long id) throws SQLException {

        String sqlStatement = MessageFormat
                .format("SELECT Name,Categorie,Code FROM {0}.{1} WHERE id = ?",
                        PostgreSQLController.SCHEMA,
                        PostgreSQLController.TABLE_ALKIS);

        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        preparedStatement.setLong(1, id);
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            String name = resultSet.getString(1);
            String categorie = resultSet.getString(2);
            int code = resultSet.getInt(3);
            String type = "ALKIS";
            return new ALKIS(name, categorie, code, type);
        }
        return null;

    }

    /**
     * this method set connection handle to db
     *
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    private void connect() throws ClassNotFoundException, SQLException {
        if (connection == null) {
            Class.forName(POSTGRECLASSNAME);
            connection = DriverManager.getConnection(postgreUrl, postgreSQLPorperties);
        }
    }

    /**
     * this method return sql-distance calculation statement in meters
     *
     * @param origin
     * @param column
     * @param distance in meters
     * @return
     */
    private String getDistanceCalculationStatement(Location origin, String column, Double distance) {
        String pointOrigin = String.format(Locale.US, "'POINT(%1$.6f %2$.6f)'", origin.getLongitude(), origin.getLatitude());
        String columns = MessageFormat.format("''POINT(''|| {0} || '' '' || {1} || '')''", "LONGITUDE", "LATITUDE");
        // https://postgis.net/docs/ST_Point.html
        String distanceCalculation = MessageFormat.format(
                "ST_Distance(ST_Transform(ST_GeomFromText({0},4326),2100),ST_Transform(ST_GeomFromText({1} ,4326),2100))",
                pointOrigin,
                columns);
        String sqlStatement = MessageFormat
                .format("SELECT ID FROM {0}.{1} WHERE {2} ilike ? and {3} < ?",
                        PostgreSQLController.SCHEMA,
                        PostgreSQLController.TABLE_LOCATIONS,
                        column,
                        distanceCalculation);
        return sqlStatement;
    }

    /**
     * this method get an featur_ids by strname, optional --> house number, additional house number <br>
     *
     * @param address
     * @return new reference of List<String>
     * @throws SQLException
     */
    public List<String> getFeatureIds(Address address) throws SQLException {
        String what = "feature_id";
        List<String> results = new ArrayList<String>();
        ResultSet resultSet = addressPreparedStatement(address, what).executeQuery();
        while (resultSet.next()) {
            results.add(resultSet.getString(1));
        }
        return results;
    }

    /**
     * this method return featur_id by strname, optional --> house number, additional house number or empty string<br>
     *
     * @param address
     * @return new reference of List<String>
     * @throws SQLException
     */
    public String getFeatureId(Address address) throws SQLException {
        String what = "feature_id";
        ResultSet resultSet = addressPreparedStatement(address, what).executeQuery();
        while (resultSet.next()) {
            return resultSet.getString(1);
        }
        return "";
    }

    /**
     * this method return count of founded addresses in db
     *
     * @param address
     * @return Long
     * @throws SQLException
     */
    public Long countOfAddresses(Address address) throws SQLException {

        String what = "Count(feature_id)";
        ResultSet resultSet = addressPreparedStatement(address, what).executeQuery();
        while (resultSet.next()) {
            return resultSet.getLong(1);
        }
        return 0L;
    }

    /**
     * this method return founded sections
     *
     * @param address
     * @return new reference of {@code Set<String>}
     * @throws SQLException
     */
    public Set<String> getSections(Address address) throws SQLException {
        String what = "distinct bez_name";
        Set<String> results = new HashSet<String>();
        ResultSet resultSet = addressPreparedStatement(address, what).executeQuery();
        while (resultSet.next()) {
            results.add(resultSet.getString(1));
        }
        return results;
    }

    /**
     * this method return address statement
     *
     * @param address
     * @param what (selected columns)
     * @return String
     * @throws SQLException
     */
    private PreparedStatement addressPreparedStatement(Address address, String what) throws SQLException {

        if (address.getFeatureId() != null && address.getFeatureId().trim().length() != 0L) {
            PreparedStatement preparedStatement = connection.prepareStatement(MessageFormat.format("SELECT feature_id FROM {0}.{1} WHERE feature_id = ?",
                    PostgreSQLController.SCHEMA,
                    PostgreSQLController.TABLE_ADDRESSES));
            preparedStatement.setString(1, address.getFeatureId());
            return preparedStatement;
        }

        boolean setBez_nameCondition = false;
        boolean setHnrCondition = false;
        boolean setHnr_zusatzCondition = false;

        int index = 1;
        int indexStr_name = 1;
        int indexBez_nameCondition = 0;
        int indexHnrCondition = 0;
        int indexHnr_zusatzCondition = 0;

        String bez_nameCondition = "";
        String additionalCondition = "";

        if (address.getBez_name() != null && !address.getBez_name().trim().isEmpty()) {
            setBez_nameCondition = true;
            bez_nameCondition = "and bez_name ilike ?";
            indexBez_nameCondition = ++index;
        }

        if (address.getHausnr() != 0.0d) {
            setHnrCondition = true;
            indexHnrCondition = ++index;
            additionalCondition = "and hausnr = ?";
            if (address.getHausnrz() != null && address.getHausnrz().length() != 0) {
                setHnr_zusatzCondition = true;
                indexHnr_zusatzCondition = ++index;
                additionalCondition = "and hausnr = ? and hausnrz ilike ?";
            }
        }

        String sqlStatement = MessageFormat.format("SELECT {0} FROM {1}.{2} WHERE strnam ilike ? {3} {4}",
                what,
                PostgreSQLController.SCHEMA,
                PostgreSQLController.TABLE_ADDRESSES,
                bez_nameCondition,
                additionalCondition);

        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        preparedStatement.setString(indexStr_name, address.getName());
        if (setBez_nameCondition) {
            preparedStatement.setString(indexBez_nameCondition, address.getBez_name());
        }
        if (setHnrCondition) {
            preparedStatement.setString(indexHnrCondition, String.valueOf(address.getHausnr().intValue()));
            if (setHnr_zusatzCondition) {
                preparedStatement.setString(indexHnr_zusatzCondition, address.getHausnrz());
            }
        }
        return preparedStatement;
    }

    /**
     * this method get an id of ALKIS
     *
     * @param alkis
     * @return id or 0
     * @throws SQLException
     */
    public long getId(ALKIS alkis) throws SQLException {
        String sqlStatement = MessageFormat
                .format("SELECT id FROM {0}.{1} WHERE lower(Name) = ? and lower(Categorie) = ? and Code = ?",
                        PostgreSQLController.SCHEMA,
                        PostgreSQLController.TABLE_ALKIS);

        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        preparedStatement.setString(1, alkis.getName().toLowerCase());
        preparedStatement.setString(2, alkis.getCategorie().toLowerCase());
        preparedStatement.setInt(3, alkis.getCode());
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            return resultSet.getLong(1);
        }
        return 0;

    }

    /**
     * this method get an id of CatalogEntryHasName by name
     *
     * @param hasName
     * @return id or 0
     * @throws SQLException
     */
    public long getId(HasName hasName) throws SQLException, IllegalArgumentException {
        if (hasName instanceof Address)
            throw new IllegalArgumentException("id of an Address is an String, use getFeatureIds(Address address)");
        if (hasName instanceof Location)
            return getId((Location)hasName);
        if (hasName instanceof ALKIS)
            return getId((ALKIS)hasName);
        if (hasName instanceof Term) {
            return getId((Term)hasName);
        }
        if (hasName instanceof Ontology) {
            return getId((Ontology)hasName);
        }
        if (hasName instanceof Keyword) {
            return getId(hasName.getName(), PostgreSQLController.TABLE_KEYWORDS);
        }
        throw new IllegalArgumentException("wrong type " + hasName.getClass().toString());

    }

    /**
     * this method get an id of a Location by name, lat and long
     *
     * @param location
     * @return id or 0
     * @throws SQLException
     */
    public long getId(Location location) throws SQLException {
        String sqlStatement = MessageFormat
                .format("SELECT id FROM {0}.{1} WHERE lower(Name) LIKE ? and LATITUDE = ? AND LONGITUDE = ?",
                        PostgreSQLController.SCHEMA,
                        PostgreSQLController.TABLE_LOCATIONS);

        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        preparedStatement.setString(1, "%" + location.getName().toLowerCase() + "%");
        preparedStatement.setDouble(2, location.getLatitude());
        preparedStatement.setDouble(3, location.getLongitude());
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            return resultSet.getLong(1);
        }
        return 0;

    }

    /**
     * this method get an id of metadata/document by name(unique)
     *
     * @param metadata
     * @return id or 0
     * @throws SQLException
     */
    public long getId(Metadata metadata) throws SQLException {

        if (metadata.get("Uri") == null)
            throw new IllegalArgumentException("parameter: \"Uri\" in metadata is null or missing");

        String sqlStatement = MessageFormat
                .format("SELECT ID FROM {0}.{1} WHERE name ilike ? and uri ilike ?",
                        PostgreSQLController.SCHEMA,
                        PostgreSQLController.TABLE_DOCUMENTS);

        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        preparedStatement.setString(1, metadata.get("name").toLowerCase());
        preparedStatement.setString(2, metadata.get("Uri").toLowerCase());
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            return resultSet.getLong(1);
        }
        return 0;

    }

    /**
     * this method get a id from a table by name(column)
     *
     * @param table
     * @param name
     * @return id or 0
     * @throws SQLException
     */
    private long getId(@Nonnull String name, @Nonnull String table) throws SQLException {
        return getId("id", name, table);
    }

    /**
     * this method get a id from a table by name(column)
     *
     * @param idColumnName
     * @param tabel
     * @param name
     * @return id or 0
     * @throws SQLException
     */
    private long getId(String idColumnName, String name, String table) throws SQLException {

        String sqlStatement = MessageFormat
                .format("SELECT DISTINCT {0} FROM {1}.{2} WHERE lower(Name) = ?", idColumnName, PostgreSQLController.SCHEMA, table.toUpperCase());
        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        preparedStatement.setString(1, name.toLowerCase());
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            return resultSet.getLong(1);
        }
        return 0;

    }

    /**
     * this method get a id from term by name, morphem and isCore
     *
     * @param term
     * @return id or 0
     * @throws SQLException
     */
    public long getId(Term term) throws SQLException {

        String sqlStatement = MessageFormat
                .format("SELECT id FROM {0}.{1} WHERE lower(Name) = ? and lower(Morphem) = ? and IsCore = ?",
                        PostgreSQLController.SCHEMA,
                        PostgreSQLController.TABLE_TERMS);

        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        preparedStatement.setString(1, term.getName().toLowerCase());
        preparedStatement.setString(2, term.getMorphem().toLowerCase());
        preparedStatement.setBoolean(3, term.getIsCore());
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            return resultSet.getLong(1);
        }
        return 0;
    }

    /**
     * this method get a id from ontology by name
     *
     * @param term
     * @return id or 0
     * @throws SQLException
     */
    public long getId(Ontology ontology) throws SQLException {

        String sqlStatement = MessageFormat
                .format("SELECT id FROM {0}.{1} WHERE Name ilike ? ",
                        PostgreSQLController.SCHEMA,
                        PostgreSQLController.TABLE_ONTOLOGIES);

        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        preparedStatement.setString(1, ontology.getName().toLowerCase());
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            return resultSet.getLong(1);
        }
        return 0;
    }

    /**
     * this method get ids from location by name and distance
     *
     * @param origin
     * @param to
     * @param distanceInMeters
     * @return new reference of List<Long>
     * @throws SQLException
     */
    public List<Long> getIds(Location origin, Location to, Double distanceInMeters) throws SQLException {

        String sqlStatement = getDistanceCalculationStatement(origin, "Name", distanceInMeters);
        List<Long> ids = new ArrayList<Long>();
        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        preparedStatement.setString(1, to.getName());
        preparedStatement.setDouble(2, distanceInMeters);
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            ids.add(resultSet.getLong(1));
        }
        return ids;
    }

    /**
     * this method get ids form a location and there synonyms on a specific view <br>
     * bounding box of berlin
     *
     * @param to
     * @return
     * @throws SQLException
     */
    public List<Long> getIds(Location to, boolean containsInSynonyms) throws SQLException {
        String sqlStatement = MessageFormat
                .format("select id from {0}.{1} where name ilike ?",
                        PostgreSQLController.SCHEMA,
                        PostgreSQLController.VIEW_LOCATION);

        List<Long> ids = new ArrayList<Long>();
        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        preparedStatement.setString(1, to.getName());
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            ids.add(resultSet.getLong(1));
        }
        if (!containsInSynonyms) {
            return ids;
        }
        // check synonym
        sqlStatement = MessageFormat
                .format("select id from {0}.{1} where synonyms ilike ?",
                        PostgreSQLController.SCHEMA,
                        PostgreSQLController.VIEW_LOCATION);
        for (String synonym : to.getSynonyms()) {
            preparedStatement = connection.prepareStatement(sqlStatement);
            preparedStatement.setString(1, "%" + synonym + "%");
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                ids.add(resultSet.getLong(1));
            }
        }

        return ids;

    }

    /**
     * this method get a ids from location contains synoymn and distance
     *
     * @param origin
     * @param synoymn
     * @param distanceInMeters
     * @return new reference of List<Long>
     * @throws SQLException
     */
    public List<Long> getIds(Location origin, String synoymn, Double distanceInMeters) throws SQLException {
        List<Long> ids = new ArrayList<Long>();
        String sqlStatement = getDistanceCalculationStatement(origin, "Synonyms", distanceInMeters);
        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        preparedStatement.setString(1, synoymn.toLowerCase());
        preparedStatement.setDouble(2, distanceInMeters);
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            ids.add(resultSet.getLong(1));
        }
        return ids;

    }

    /**
     * this method return all ids of documents they are not analyzed
     *
     * @return new reference {@code  List<Long>}
     * @throws SQLException
     */
    public List<Long> getUnanalyzeDocumentIDs() throws SQLException {

        List<Long> ids = new ArrayList<Long>();
        String sqlStatement = MessageFormat.format("SELECT id from {0}.{1} where not isAnalysed",
                PostgreSQLController.SCHEMA,
                PostgreSQLController.TABLE_DOCUMENTS);
        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            ids.add(resultSet.getLong(1));
        }
        return ids;

    }

    /**
     * set a document state to analyzed with timestamp
     *
     * @param id
     * @return
     * @throws SQLException
     */
    public int setDocumentIsAnalysed(long id) throws SQLException {
        String sqlStatement = MessageFormat
                .format("UPDATE {0}.{1} SET isAnalysed = ?, AnalysedOn = now()"
                        + " where id = ?",
                        PostgreSQLController.SCHEMA,
                        PostgreSQLController.TABLE_DOCUMENTS);

        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        preparedStatement.setBoolean(1, true);
        preparedStatement.setLong(2, id);
        return preparedStatement.executeUpdate();
    }

    /**
     * this method get mapping statement
     *
     * @param mappingTable
     * @return sql statement
     */
    private String getIsMappedStatement(String mappingTable) {
        String left = MessageFormat.format("{0}_id", PostgreSQLController.TABLE_DOCUMENTS);
        String right = "{0}_id";

        switch(mappingTable) {
            case PostgreSQLController.MAPPING_TABLE_DOCUMENTS_ADDRESSES:
                // tabelname: fis_s_rbs_adr | columnname: feature_id
                right = MessageFormat.format("{0}_feature_id", PostgreSQLController.TABLE_ADDRESSES);
                break;
            case PostgreSQLController.MAPPING_TABLE_DOCUMENTS_ALKIS:
                right = MessageFormat.format(right, PostgreSQLController.TABLE_ALKIS);
                break;
            case PostgreSQLController.MAPPING_TABLE_DOCUMENTS_KEYWORDS:
                right = MessageFormat.format(right, PostgreSQLController.TABLE_KEYWORDS);
                break;
            case PostgreSQLController.MAPPING_TABLE_DOCUMENTS_LOCATIONS:
                right = MessageFormat.format(right, PostgreSQLController.TABLE_LOCATIONS);
                break;
            case PostgreSQLController.MAPPING_TABLE_DOCUMENTS_ONTOLOGIES:
                right = MessageFormat.format(right, PostgreSQLController.TABLE_ONTOLOGIES);
                break;
            case PostgreSQLController.MAPPING_TABLE_DOCUMENTS_TERMS:
                right = MessageFormat.format(right, PostgreSQLController.TABLE_TERMS);
                break;
            case PostgreSQLController.MAPPING_TABLE_TERMS_ONTOLOGIES:
                left = MessageFormat.format("{0}_id", PostgreSQLController.TABLE_TERMS);
                right = MessageFormat.format(right, PostgreSQLController.TABLE_ONTOLOGIES);

        }
        return MessageFormat.format("SELECT {0},{1} FROM {2}.{3} WHERE {0} = ? AND {1} = ?", left, right, PostgreSQLController.SCHEMA, mappingTable);
    }

    /**
     * this method get keyword by id
     *
     * @param id
     * @return keyword or null
     * @throws SQLException
     */
    public Keyword getKeyword(long id) throws SQLException {
        String sqlStatement = MessageFormat
                .format("SELECT Name FROM {0}.{1} WHERE id = ?",
                        PostgreSQLController.SCHEMA,
                        PostgreSQLController.TABLE_KEYWORDS);

        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        preparedStatement.setLong(1, id);
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            return new Keyword(resultSet.getString(1), 0.0d, id);
        }
        return null;
    }

    /**
     * this method get location by id
     *
     * @param id
     * @return new reference of Location or null
     * @throws SQLException
     */
    public Location getLocation(long id) throws SQLException {
        String sqlStatement = MessageFormat
                .format("SELECT NAME,FEATURE_CODE,LATITUDE,LONGITUDE,SYNONYMS FROM {0}.{1} where id = ?",
                        PostgreSQLController.SCHEMA,
                        PostgreSQLController.TABLE_LOCATIONS);
        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        preparedStatement.setLong(1, id);
        ResultSet resultSet = preparedStatement.executeQuery();

        while (resultSet.next()) {
            String name = resultSet.getString(1);
            String featureCode = resultSet.getString(2);
            double latitude = resultSet.getDouble(3);
            double longitude = resultSet.getDouble(4);
            Set<String> synonyms = new HashSet<String>();
            if (resultSet.getString(5).length() != 0) {
                synonyms = new HashSet<String>(Arrays.asList(resultSet.getString(5).split(",")));
            }
            return new Location(id, name, featureCode, latitude, longitude, synonyms);
        }
        return null;

    }

    /**
     * this method get metadata/document by id
     *
     * @param id
     * @return new reference of metadata(name, author) or null
     * @throws SQLException
     */
    public Metadata getMetadata(long id) throws SQLException {
        String sqlStatement = MessageFormat
                .format("SELECT Name,Author,Uri FROM {0}.{1} WHERE id = ?",
                        PostgreSQLController.SCHEMA,
                        PostgreSQLController.TABLE_DOCUMENTS);

        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        preparedStatement.setLong(1, id);
        ResultSet resultSet = preparedStatement.executeQuery();

        while (resultSet.next()) {
            Metadata metadata = new Metadata();
            metadata.add("name", resultSet.getString(1));
            metadata.add("Author", resultSet.getString(2));
            metadata.add("Uri", resultSet.getString(3));
            return metadata;
        }
        return null;
    }

    /**
     * this method get ontology by id
     *
     * @param id
     * @return string or null
     * @throws SQLException
     */
    public Ontology getOntology(long id) throws SQLException {

        String sqlStatement = MessageFormat
                .format("SELECT Name FROM {0}.{1} WHERE id = ?",
                        PostgreSQLController.SCHEMA,
                        PostgreSQLController.TABLE_ONTOLOGIES);

        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        preparedStatement.setLong(1, id);
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            return new Ontology(resultSet.getString(1));
        }
        return null;
    }

    /**
     * this method get the mapping statement
     *
     * @param mappingTable
     * @return sql mapping statement
     */
    private String getTableMappingStatement(String mappingTable) {
        String columnNames = "{0}_id, {1}_id";
        String values = "?,?";

        switch(mappingTable) {
            case PostgreSQLController.MAPPING_TABLE_DOCUMENTS_ADDRESSES:
                columnNames = MessageFormat.format("{0}_id, {1}_feature_id", PostgreSQLController.TABLE_DOCUMENTS, PostgreSQLController.TABLE_ADDRESSES);
                break;
            case PostgreSQLController.MAPPING_TABLE_DOCUMENTS_ALKIS:
                columnNames = MessageFormat.format(columnNames, PostgreSQLController.TABLE_DOCUMENTS, PostgreSQLController.TABLE_ALKIS);
                break;
            case PostgreSQLController.MAPPING_TABLE_DOCUMENTS_KEYWORDS:
                columnNames = MessageFormat.format("{0}_id, {1}_id, WEIGHT", PostgreSQLController.TABLE_DOCUMENTS, PostgreSQLController.TABLE_KEYWORDS);
                values = "?,?,?";
                break;
            case PostgreSQLController.MAPPING_TABLE_DOCUMENTS_LOCATIONS:
                columnNames = MessageFormat.format(columnNames, PostgreSQLController.TABLE_DOCUMENTS, PostgreSQLController.TABLE_LOCATIONS);
                break;
            case PostgreSQLController.MAPPING_TABLE_DOCUMENTS_ONTOLOGIES:
                columnNames = MessageFormat.format(columnNames, PostgreSQLController.TABLE_DOCUMENTS, PostgreSQLController.TABLE_ONTOLOGIES);
                break;
            case PostgreSQLController.MAPPING_TABLE_DOCUMENTS_TERMS:
                columnNames = MessageFormat.format(columnNames, PostgreSQLController.TABLE_DOCUMENTS, PostgreSQLController.TABLE_TERMS);
                break;
            case PostgreSQLController.MAPPING_TABLE_TERMS_ONTOLOGIES:
                columnNames = MessageFormat.format(columnNames, PostgreSQLController.TABLE_TERMS, PostgreSQLController.TABLE_ONTOLOGIES);
        }
        return MessageFormat.format("INSERT INTO {0}({1}) VALUES({2})", mappingTable.toLowerCase(), columnNames, values);
    }

    /**
     * this method return specific table parameters
     *
     * @param tabelName
     * @return new reference List<String>
     */
    private List<String> getTableParameters(String tabelName) {
        List<String> parameters = new ArrayList<String>();

        switch(tabelName) {
            case PostgreSQLController.TABLE_KEYWORDS:
                parameters.add("ID BIGSERIAL PRIMARY KEY");
                parameters.add("Name VARCHAR UNIQUE NOT NULL");
                break;
            case PostgreSQLController.TABLE_ALKIS:
                parameters.add("ID BIGSERIAL PRIMARY KEY");
                parameters.add("Name VARCHAR NOT NULL");
                parameters.add("Categorie VARCHAR NOT NULL");
                parameters.add("Code INTEGER");
                break;
            case PostgreSQLController.TABLE_TERMS:
                parameters.add("ID BIGSERIAL PRIMARY KEY");
                parameters.add("Name VARCHAR NOT NULL");
                parameters.add("IsCore BOOLEAN");
                parameters.add("Morphem VARCHAR");
                parameters.add("UNIQUE (Name, IsCore, Morphem)");
                break;
            case PostgreSQLController.TABLE_DOCUMENTS:
                parameters.add("ID BIGSERIAL PRIMARY KEY");
                parameters.add("Name VARCHAR NOT NULL");
                parameters.add("Author VARCHAR NOT NULL");
                parameters.add("Uri VARCHAR NOT NULL");
                parameters.add("IsAnalysed boolean NOT NULL DEFAULT false");
                parameters.add("AnalysedOn timestamptz");
                parameters.add("UNIQUE (Name, Uri)");
                break;
            case PostgreSQLController.TABLE_ONTOLOGIES:
                parameters.add("ID BIGSERIAL PRIMARY KEY");
                parameters.add("Name VARCHAR UNIQUE NOT NULL");
                break;
            case PostgreSQLController.TABLE_LOCATIONS:
                parameters.add("ID BIGSERIAL PRIMARY KEY");
                parameters.add("Name VARCHAR NOT NULL");
                parameters.add("FEATURE_CODE VARCHAR");
                parameters.add("LATITUDE DOUBLE PRECISION");
                parameters.add("LONGITUDE DOUBLE PRECISION");
                parameters.add("SYNONYMS VARCHAR");
                break;
        }
        return parameters;
    }

    /**
     * this method get term by id
     *
     * @param id
     * @return new reference of Term or null
     * @throws SQLException
     */
    public Term getTerm(long id) throws SQLException {
        String sqlStatement = MessageFormat
                .format("SELECT Name,IsCore,Morphem  FROM {0}.{1} WHERE id = ?",
                        PostgreSQLController.SCHEMA,
                        PostgreSQLController.TABLE_TERMS);

        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        preparedStatement.setLong(1, id);
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            String name = resultSet.getString(1);
            boolean isCore = resultSet.getBoolean(2);
            String morphem = resultSet.getString(3);
            String type = "Term";
            return new Term(isCore, morphem, name, new ArrayList<>(), type);
        }
        return null;
    }

    /**
     * this method import locations in db
     *
     * @param locations
     * @return {@code Boolean}
     * @throws SQLException
     */
    public Boolean importLocationsTabel(List<Location> locations) throws SQLException {
        // all entries
        deleteStatement(PostgreSQLController.TABLE_LOCATIONS, null);

        String sqlStatement = MessageFormat.format("INSERT INTO {0}.{1}(ID,NAME,FEATURE_CODE,LATITUDE,LONGITUDE,SYNONYMS) VALUES(?,?,?,?,?,?)",
                PostgreSQLController.SCHEMA,
                PostgreSQLController.TABLE_LOCATIONS);

        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        int count = 0;

        for (Location location : locations) {
            String synonyms = "";
            for (String synonym : location.getSynonyms()) {
                synonyms += synonym + ",";
            }
            // remove last comma
            synonyms = synonyms.substring(0, synonyms.length() - 1);
            preparedStatement.setLong(1, location.getId());
            preparedStatement.setString(2, location.getName());
            preparedStatement.setString(3, location.getFeatureCode());
            preparedStatement.setDouble(4, location.getLatitude());
            preparedStatement.setDouble(5, location.getLongitude());
            preparedStatement.setString(6, synonyms);
            preparedStatement.addBatch();
            count++;
            if (count % 1000 == 0 || count == locations.size()) {
                LOGGER.info(MessageFormat.format("{0} of {1}", count, locations.size()));
                preparedStatement.executeBatch();
            }
        }
        return true;
    }

    /**
     * this method insert CatalogEntryHasName in db
     *
     * @param hasName
     * @return id
     * @throws SQLException
     */
    public long insert(HasName hasName) throws SQLException {
        if (hasName instanceof Address)
            throw new IllegalArgumentException("insert of Type " + hasName.getClass().toString() + " not allowed");

        String table = "";
        Map<String, Object> parameters = new HashMap<String, Object>();
        if (hasName instanceof Term) {
            table = PostgreSQLController.TABLE_TERMS;
            parameters.put("name", hasName.getName());
            parameters.put("morphem", ((Term)hasName).getMorphem());
            parameters.put("isCore", ((Term)hasName).getIsCore());

            long idCatalogEntryHasName = insertStatement(table, parameters);

            for (Ontology ontology : ((Term)hasName).getOntologies()) {
                long idOntology = getId(ontology);
                if (idOntology == 0) {
                    idOntology = insert(ontology);
                }
                if (!isMapped(MAPPING_TABLE_TERMS_ONTOLOGIES, idCatalogEntryHasName, idOntology)) {
                    map(MAPPING_TABLE_TERMS_ONTOLOGIES, idCatalogEntryHasName, idOntology, null);
                }

            }
            return idCatalogEntryHasName;

        }
        if (hasName instanceof ALKIS) {
            table = PostgreSQLController.TABLE_ALKIS;
            parameters.put("name", hasName.getName());
            parameters.put("categorie", ((ALKIS)hasName).getCategorie());
            parameters.put("code", ((ALKIS)hasName).getCode());
        }
        if (hasName instanceof Keyword) {
            table = PostgreSQLController.TABLE_KEYWORDS;
            parameters.put("name", hasName.getName());
            // only in mapping table
            // parameters.put("weight", ((Keyword)hasName).getWeight());
        }
        if (hasName instanceof Ontology) {
            table = PostgreSQLController.TABLE_ONTOLOGIES;
            parameters.put("name", hasName.getName());
        }

        return insertStatement(table, parameters);
    }

    /**
     * this method insert location in db
     *
     * @param location
     * @return id
     * @throws SQLException
     */
    public long insert(Location location) throws SQLException {
        Map<String, Object> parameters = new HashMap<String, Object>();
        String synonyms = "";
        parameters.put("ID", location.getId());
        parameters.put("NAME", location.getName());
        parameters.put("FEATURE_CODE", location.getFeatureCode());
        parameters.put("LATITUDE", location.getLatitude());
        parameters.put("LONGITUDE", location.getLongitude());
        for (String synonym : location.getSynonyms()) {
            synonyms += synonym + ",";
        }
        parameters.put("SYNONYMS", synonyms);
        return insertStatement(PostgreSQLController.TABLE_LOCATIONS, parameters);
    }

    /**
     * this method insert location in db
     *
     * @param location
     * @return id
     * @throws SQLException
     */
    public long insert(Ontology ontology) throws SQLException {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("NAME", ontology.getName());
        return insertStatement(PostgreSQLController.TABLE_ONTOLOGIES, parameters);
    }

    /**
     * this method insert metadata/document in db
     *
     * @param metadata
     * @return id
     * @throws SQLException
     * @throws IllegalArgumentException
     */

    public long insert(Metadata metadata) throws SQLException, IllegalArgumentException {
        if (metadata.get("Uri") == null)
            throw new IllegalArgumentException("parameter: \"Uri\" in metadata is null or missing");
        Map<String, Object> parameters = new HashMap<String, Object>();
        String author = (metadata.get("Author") == null) ? "unknown" : metadata.get("Author");
        String uri = metadata.get("Uri");
        parameters.put("name", metadata.get("name"));
        parameters.put("Author", author);
        parameters.put("Uri", uri);
        parameters.put("IsAnalysed", false);
        return insertStatement(PostgreSQLController.TABLE_DOCUMENTS, parameters);
    }

    public void deleteDocument(long id) throws SQLException {

        String sqlStatement = MessageFormat.format("DELETE FROM {0}.{1} WHERE id = ?", PostgreSQLController.SCHEMA, PostgreSQLController.TABLE_DOCUMENTS);
        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        preparedStatement.setLong(1, id);
        preparedStatement.executeUpdate();
    }

    /**
     * this method update author field of metadata/document
     *
     * @param id
     * @param author
     * @return
     * @throws SQLException
     */
    public long updateDocumentAuthor(long id, String author) throws SQLException {
        String sqlStatement = MessageFormat
                .format("update {0}.{1} SET Author = ? where id = ?", PostgreSQLController.SCHEMA, PostgreSQLController.TABLE_DOCUMENTS);
        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        preparedStatement.setString(1, author);
        preparedStatement.setLong(2, id);
        return preparedStatement.executeUpdate();
    }

    /**
     * this method insert specific name in a specific table
     *
     * @param name
     * @param table
     * @return
     * @throws SQLException
     */
    public long insert(String name, String table) throws SQLException {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("name", name);
        return insertStatement(table, parameters);
    }

    /**
     * this method create insert statement and perform possible parameter types Integer, Double, Float, Date, Boolean, String
     *
     * @param tabel
     * @param parameters
     * @return new id or 0
     * @throws SQLException
     */
    private long insertStatement(String tabel, Map<String, Object> parameters) throws SQLException {

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
        sqlStatement = (MessageFormat.format("INSERT INTO {0}.{1}({2}) VALUES({3})", PostgreSQLController.SCHEMA, tabel, values, questionMark));
        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement, Statement.RETURN_GENERATED_KEYS);
        // Assign data
        int index = 1;
        for (String key : parameters.keySet()) {

            Object data = parameters.get(key);
            if (data instanceof Integer) {
                preparedStatement.setInt(index++, (Integer)data);
                continue;
            }
            if (data instanceof Long) {
                preparedStatement.setLong(index++, (Long)data);
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
        if (preparedStatement.executeUpdate() > 0) {
            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
        }

        return 0;
    }

    /**
     * this method checks whether are two ids mapped
     *
     * @param mappingTable
     * @param leftId
     * @param rightId
     * @return true or false
     * @throws SQLException
     */
    private Boolean isMapped(String mappingTable, long leftId, long rightId) throws SQLException {
        String sqlStatement = getIsMappedStatement(mappingTable);
        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        preparedStatement.setLong(1, leftId);
        preparedStatement.setLong(2, rightId);
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            return true;
        }
        return false;

    }

    // ToDO test
    /**
     * @param mappingTable
     * @param docId
     * @param feature_id
     * @return
     * @throws SQLException
     */
    private Boolean isMapped(String mappingTable, long docId, String feature_id) throws SQLException {
        String sqlStatement = getIsMappedStatement(mappingTable);
        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        preparedStatement.setLong(1, docId);
        preparedStatement.setString(2, feature_id);
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            return true;
        }
        return false;

    }

    /**
     * this method provide some logic to map metadata/documents and address <br>
     * checks whether metadata/documents exist, if not insert <br>
     * checks address exist, if not return <br>
     * and mapped the both
     *
     * @param metadata
     * @param idAddress
     * @throws SQLException
     */
    private void map(Metadata metadata, Address address) throws SQLException {

        if (address.getFeatureId() == null && address.getFeatureId().trim().length() == 0) {
            throw new SQLException("id of address is null or 0");
        }
        long idDocument = getId(metadata);
        if (idDocument == 0) {
            idDocument = insert(metadata);
        }
        if (!isMapped(PostgreSQLController.MAPPING_TABLE_DOCUMENTS_ADDRESSES, idDocument, address.getFeatureId())) {
            map(PostgreSQLController.MAPPING_TABLE_DOCUMENTS_ADDRESSES, idDocument, address.getFeatureId());
        }

    }

    /**
     * this method provide some logic to map metadata/documents and catalogEntryHasName <br>
     * checks whether metadata/documents exist, if not insert <br>
     * checks whether catalogEntryHasName exist, if not insert <br>
     * and mapped the both
     *
     * @param metadata
     * @param hasName
     * @throws SQLException
     */
    public void map(Metadata metadata, HasName hasName) throws SQLException {
        long idCatalogEntryHasName = 0;
        String mappingTabelName = "";

        if (hasName instanceof Address) {
            Address address = (Address)hasName;
            map(metadata, address);
            return;
        }

        if (hasName instanceof Location) {
            Location location = (Location)hasName;
            map(metadata, location);
            return;
        }

        if (hasName instanceof ALKIS) {
            idCatalogEntryHasName = getId(hasName);
            mappingTabelName = PostgreSQLController.MAPPING_TABLE_DOCUMENTS_ALKIS;
        }
        if (hasName instanceof Term) {
            idCatalogEntryHasName = getId((Term)hasName);
            mappingTabelName = PostgreSQLController.MAPPING_TABLE_DOCUMENTS_TERMS;
        }

        if (hasName instanceof Ontology) {
            idCatalogEntryHasName = getId((Ontology)hasName);
            mappingTabelName = PostgreSQLController.MAPPING_TABLE_DOCUMENTS_ONTOLOGIES;
        }

        long idDocument = getId(metadata);
        if (idDocument == 0) {
            idDocument = insert(metadata);
        }
        if (idCatalogEntryHasName == 0) {
            idCatalogEntryHasName = insert(hasName);
        }
        if (!isMapped(mappingTabelName, idDocument, idCatalogEntryHasName)) {
            map(mappingTabelName, idDocument, idCatalogEntryHasName, 0.0d);
        }

    }

    /**
     * this method provide some logic to map metadata/documents and location <br>
     * checks whether metadata/documents exist, if not insert <br>
     * checks whether catalogEntryHasName exist, if not insert <br>
     * and mapped the both
     *
     * @param metadata
     * @param catalogEntryHasName
     * @throws SQLException
     */
    private void map(Metadata metadata, Location location) throws SQLException {
        long idDocument = getId(metadata);
        long idLocation = getId(location);
        if (idDocument == 0) {
            idDocument = insert(metadata);
        }
        if (idLocation == 0) {
            idLocation = insert(location);
        }
        if (!isMapped(PostgreSQLController.MAPPING_TABLE_DOCUMENTS_LOCATIONS, idDocument, idLocation)) {
            map(PostgreSQLController.MAPPING_TABLE_DOCUMENTS_LOCATIONS, idDocument, idLocation, 0.0d);
        }
    }

    /**
     * this method provide some logic to map metadata/documents and ontology <br>
     * checks whether metadata/documents exist, if not insert <br>
     * checks whether ontology exist, if not insert <br>
     * and mapped the both
     *
     * @param metadata
     * @param ontology
     * @throws SQLException
     */
    public void map(Metadata metadata, Ontology ontology) throws SQLException {

        long idDocument = getId(metadata);
        long idOntology = getId(ontology);

        if (idDocument == 0) {
            idDocument = insert(metadata);
        }
        if (idOntology == 0) {
            idOntology = insert(ontology);
        }

        if (!isMapped(PostgreSQLController.MAPPING_TABLE_DOCUMENTS_ONTOLOGIES, idDocument, idOntology)) {
            map(PostgreSQLController.MAPPING_TABLE_DOCUMENTS_ONTOLOGIES, idDocument, idOntology, null);
        }
    }

    /**
     * this method provide some logic to map metadata/documents and keyword with weight<br>
     * checks whether metadata/documents exist, if not insert <br>
     * checks whether keyword exist, if not insert <br>
     * and mapped the both
     *
     * @param metadata
     * @param keyword
     * @param weigth
     * @throws SQLException
     */
    public void map(Metadata metadata, Keyword keyword) throws SQLException {

        long idDocument = getId(metadata);
        long idKeyword = getId(keyword);

        if (idDocument == 0) {
            idDocument = insert(metadata);
        }
        if (idKeyword == 0) {
            idKeyword = insert(keyword);
        }
        if (!isMapped(PostgreSQLController.MAPPING_TABLE_DOCUMENTS_KEYWORDS, idDocument, idKeyword)) {
            map(PostgreSQLController.MAPPING_TABLE_DOCUMENTS_KEYWORDS, idDocument, idKeyword, keyword.getWeight());
        }
    }

    /**
     * this method map two ids
     *
     * @param mappingTable
     * @param leftId
     * @param rightId
     * @param weight
     * @throws SQLException
     */
    private void map(String mappingTable, long leftId, long rightId, @javax.annotation.Nullable Double weight)
            throws SQLException {

        String sqlStatement = getTableMappingStatement(mappingTable);
        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        preparedStatement.setLong(1, leftId);
        preparedStatement.setLong(2, rightId);
        if (mappingTable.equals(PostgreSQLController.MAPPING_TABLE_DOCUMENTS_KEYWORDS)) {
            preparedStatement.setDouble(3, (weight == null) ? 0.0 : weight);
        }
        preparedStatement.executeUpdate();

    }

    /**
     * this method maps document and address by ids
     *
     * @param mappingTable
     * @param docId
     * @param featureId
     * @throws SQLException
     */
    private void map(String mappingTable, long docId, String featureId)
            throws SQLException {

        String sqlStatement = getTableMappingStatement(mappingTable);
        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        preparedStatement.setLong(1, docId);
        preparedStatement.setString(2, featureId);
        preparedStatement.executeUpdate();

    }

    /**
     * this method analysis result to db (documents) {@link Metadata} and {@link Address}
     *
     * @param metadata
     * @param HasName (Location or Address)
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public void persist(Metadata metadata, HasName hasName, @javax.annotation.Nullable Set<String> textPassages) throws ClassNotFoundException, SQLException {
        if (hasName instanceof Location || hasName instanceof Address) {
            map(metadata, hasName);
            if (textPassages != null && textPassages.size() > 0)
                update(metadata, hasName, textPassages);
        } else {
            throw new IllegalArgumentException("Type not allowed " + hasName.getClass().getSimpleName() + " \n only " +
                    Location.class.getName() + "and \n " +
                    Address.class.getName());

        }

    }

    /**
     * this method save result to db (documents) {@link Metadata}, keyword (weight is optional), and {@link HasName}
     *
     * @param metadata (document)
     * @param keyword (keyword)
     * @param hasName (catalog entry ALKIS or Term)
     * @param textPassages (Set<String>)
     * @throws ClassNotFoundException
     * @throws SQLException
     */

    public void persist(Metadata metadata, Keyword keyword, @javax.annotation.Nullable HasName hasName, @javax.annotation.Nullable Set<String> textPassages)
            throws ClassNotFoundException, SQLException {

        map(metadata, keyword);
        if (textPassages != null && textPassages.size() > 0)
            update(metadata, keyword, textPassages);

        if (hasName != null) {
            map(metadata, hasName);
            update(metadata, hasName, textPassages);
            if (hasName instanceof Term) {
                for (Ontology ontology : ((Term)hasName).getOntologies()) {
                    map(metadata, ontology);
                }
            }
        }
    }

    public Set<String> getStreetNames() throws SQLException {
        return getNames(TABLE_ADDRESSES, "strnam");
    }

    public Set<String> getDistrictsNames() throws SQLException {
        return getNames(TABLE_DISTRICTS, "nam");
    }

    public Set<String> getSectionsNames() throws SQLException {
        return getNames(TABLE_SECTIONS, "\"NAMGEM\"");
    }

    private Set<String> getNames(String table, String column) throws SQLException {
        String sqlStatement = MessageFormat.format("select {0} from {1}.{2} group by {0} order by {0}", column, PostgreSQLController.SCHEMA, table);
        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        ResultSet resultSet = preparedStatement.executeQuery();
        Set<String> streetNames = new HashSet<String>();
        while (resultSet.next()) {
            streetNames.add(resultSet.getString(1));
        }
        return streetNames;
    }

    /**
     * this method validate properties and set them
     *
     * @param properties
     * @return
     */
    public Boolean validateProperties(final Properties properties) {

        postgreUrl = properties.getProperty(ApplicationConfiguration.POSTGRESQL_URI);
        if (postgreUrl == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.POSTGRESQL_URI);
        }
        path2PostgrePropertieFile = properties.getProperty(ApplicationConfiguration.PATH_2_POSTGRESQL_PROPERTY_FILE);
        if (path2PostgrePropertieFile == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.PATH_2_POSTGRESQL_PROPERTY_FILE);
        }

        return true;
    }

    /**
     * this method update hole text passages column of a given document and keyword
     *
     * @param metadata
     * @param object
     * @param textpassages
     * @throws SQLException
     */
    private void update(Metadata metadata, HasName hasName, Set<String> textpassages) throws SQLException {

        long idDocument = getId(metadata);
        long idObject = 0L;
        String featureId = "";
        if (hasName instanceof Address)
            featureId = getFeatureId((Address)hasName);
        else
            idObject = getId(hasName);

        Array array = connection.createArrayOf("text", textpassages.toArray());
        String sqlStatement = getUpdateTextPassagesSQLStatement(hasName);
        if (sqlStatement.equals("")) // no mapping available
            return;
        PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
        preparedStatement.setArray(1, array);
        preparedStatement.setLong(2, idDocument);
        if (idObject != 0) // switch to another id type
            preparedStatement.setLong(3, idObject);
        else
            preparedStatement.setString(3, featureId);
        preparedStatement.executeUpdate();
    }

    private String getUpdateTextPassagesSQLStatement(HasName hasName) {

        if (hasName instanceof Location)
            return MessageFormat.format("update {0}.{1} SET textpassages = ? where nlp_documents_id = ? and nlp_locations_id = ?",
                    PostgreSQLController.SCHEMA,
                    PostgreSQLController.MAPPING_TABLE_DOCUMENTS_LOCATIONS);
        if (hasName instanceof Address)
            return MessageFormat.format("update {0}.{1} SET textpassages = ? where nlp_documents_id = ? and fis_s_rbs_adr_feature_id = ?",
                    PostgreSQLController.SCHEMA,
                    PostgreSQLController.MAPPING_TABLE_DOCUMENTS_ADDRESSES);
        if (hasName instanceof Keyword) {
            return MessageFormat.format("update {0}.{1} SET textpassages = ? where nlp_documents_id = ? and nlp_keywords_id = ?",
                    PostgreSQLController.SCHEMA,
                    PostgreSQLController.MAPPING_TABLE_DOCUMENTS_KEYWORDS);
        }
        return "";
    }

}
