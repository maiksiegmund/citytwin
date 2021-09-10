package de.citytwin;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Query;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.TransactionWork;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	public long addDocument(final String name) {

		session = driver.session();
		try {

			session.writeTransaction(new TransactionWork<Void>() {
				public Void execute(Transaction tx) {

					if (getDocumentId(tx, name) == -1L)
						tx.run("CREATE (:Dokument {name: $name})", Values.parameters("name", name));
					return null;
				}
			});
			return session.readTransaction(new TransactionWork<Long>() {
				public Long execute(Transaction tx) {
					return getDocumentId(tx, name);
				}
			});
		} 
		finally {
			session.close();
			logger.info("addDocument");
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
