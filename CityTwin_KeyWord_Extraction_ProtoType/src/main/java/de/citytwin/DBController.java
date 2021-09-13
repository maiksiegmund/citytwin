package de.citytwin;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
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

    public List<String> getStreetNames() {

        List<String> streets = new ArrayList<String>();
        Result dbResult;
        session = driver.session();
        dbResult = session.run("Match (a:ADRESSE) return DISTINCT a.str_name as name Order By name");
        while (dbResult.hasNext()) {
            Record row = dbResult.next();
            streets.add(row.get("name").asString());
        }
        session.close();
        return streets;
    }

    public String getIDStreetName(String street) {

        String result = "";
        Result dbResult;
        session = driver.session();
        dbResult = session.run("Match (a:ADRESSE) where a.str_name = $street return ID(a) as id limit 1",
                Values.parameters("street", street));
        if (dbResult.hasNext()) {
            Record row = dbResult.single();
            result = String.valueOf(row.get(0).asInt());
        }

        session.close();
        return result;
    }

    private Result addOntologyEntry(final Transaction transaction, OntologyDTO dto) {

        return transaction.run("CREATE (:OntologyEntry {isCore: $isCore, isKeyWord: $isKeyWord, isSemantic: $isSemantic, word: $word, stemm: $stemm})",
                Values.parameters(
                        "isCore",
                        dto.isCore(),
                        "isKeyWord",
                        dto.isKeyWord(),
                        "isSematnic",
                        dto.isSemantic(),
                        "word",
                        dto.word,
                        "stemm",
                        dto.stemm));

    }

    private Result addALKIS(final Transaction transaction, ALKISDTO dto) {
        return transaction.run("CREATE (:ALKIS {categorie: $categorie, code: $code, name: $name})",
                Values.parameters(
                        "categorie",
                        dto.getCategorie(),
                        "code",
                        dto.getCode(),
                        "name",
                        dto.getName()));

    }

    private Result addOntologyType(final Transaction transaction, String type) {
        return transaction.run("CREATE (:OntologyType {type: $type})",
                Values.parameters(
                        "type",
                        type));

    }

    public void createOntologies(List<OntologyDTO> dtos) {

        try(Session session = driver.session()) {

            Set<String> distinctList = new HashSet<String>();
            for (OntologyDTO dto : dtos) {
                session.writeTransaction(transaction -> addOntologyEntry(transaction, dto));
                // simple way to distinct ontology types
                if (!distinctList.contains(dto.getType())) {
                    distinctList.add(dto.getType());
                }
            }

            for (String type : distinctList) {
                session.writeTransaction(transaction -> addOntologyType(transaction, type));
            }

        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }
    }

    public void createALKIS(List<ALKISDTO> dtos) {
        try(Session session = driver.session()) {

            for (ALKISDTO dto : dtos) {
                session.writeTransaction(transaction -> addALKIS(transaction, dto));
            }
        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }
    }

    public void linkDocumentAndStreet(final String document, final String street) {
        session = driver.session();
        try {
            long idDoc, idAddress;
            idDoc = session.readTransaction(tx -> getDocumentId(tx, document)).longValue();
            idAddress = session.readTransaction(tx -> getAddressId(tx, street)).longValue();
            session.writeTransaction(tx -> linkStreetAndAddress(tx, idDoc, idAddress));
            session.writeTransaction(tx -> linkAddressAndStreet(tx, idDoc, idAddress));

        }

        finally {
            session.close();
            logger.info("linked document and street");
        }
    }

    public void close() {
        if (session.isOpen())
            session.close();
        driver.close();

    }

    private Long getDocumentId(final Transaction tx, final String name) {
        Result result = tx.run("Match (n:Dokument {name: $name}) Return id(n) limit 1",
                Values.parameters("name", name));
        if (result.hasNext())
            return result.single().get(0).asLong();
        return -1L;

    }

    private Long getAddressId(final Transaction tx, final String str_name) {
        Result result = tx.run("Match (n:ADRESSE {str_name: $name}) Return id(n) limit 1",
                Values.parameters("name", str_name));
        if (result.hasNext())
            return result.single().get(0).asLong();
        return -1L;

    }

    private Result linkStreetAndAddress(final Transaction tx, final long idDoc, final long idAddress) {
        return tx.run(
                "match (doc:Dokument), (add:ADRESSE) where ID(doc) = $IdDoc and ID(add) = $IdAddress Create (doc)-[r:betrifft]->(add)",
                Values.parameters("IdDoc", idDoc, "IdAddress", idAddress));
    }

    private Result linkAddressAndStreet(final Transaction tx, final long idDoc, final long idAddress) {
        return tx.run(
                "match (doc:Dokument), (add:ADRESSE) where ID(doc) = $IdDoc and ID(add) = $IdAddress Create (add)-[r:aufgefuehrt]->(doc)",
                Values.parameters("IdDoc", idDoc, "IdAddress", idAddress));

    }
}
