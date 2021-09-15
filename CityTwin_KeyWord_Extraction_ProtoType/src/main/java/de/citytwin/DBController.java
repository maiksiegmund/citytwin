package de.citytwin;

import com.sun.javafx.binding.StringFormatter;

import java.lang.invoke.MethodHandles;
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

    private boolean existOntologyEntry(final Transaction transaction, OntologyDTO dto) {
        Result result = transaction.run("Match (ont:OntologyEntry) where ont.name = $name",
                Values.parameters("name", dto.getWord()));
        if (result.hasNext())
            return true;
        return false;
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
                        dto.getWord(),
                        "stemm",
                        dto.getStemm()));

    }

    private boolean existALKISEntry(final Transaction transaction, ALKISDTO dto) {
        Result result = transaction.run("Match (alkis:ALKISEntry) where alkis.name = $name",
                Values.parameters("name", dto.getName()));
        if (result.hasNext())
            return true;
        return false;
    }

    private Result addALKISEntry(final Transaction transaction, ALKISDTO dto) {
        return transaction.run("CREATE (:ALKISEntry {categorie: $categorie, code: $code, name: $name})",
                Values.parameters(
                        "categorie",
                        dto.getCategorie(),
                        "code",
                        dto.getCode(),
                        "name",
                        dto.getName()));

    }

    private boolean existOntologyType(final Transaction transaction, final OntologyDTO dto) {
        Result result = transaction.run("Match (ont:OntologyType) where ont.type = $type",
                Values.parameters("type", dto.getType()));
        if (result.hasNext())
            return true;
        return false;
    }

    private Result addOntologyType(final Transaction transaction, final OntologyDTO dto) {
        return transaction.run("CREATE (:OntologyType {type: $type})",
                Values.parameters(
                        "type",
                        dto.getType()));

    }

    private boolean existDocument(final Transaction transaction, final Metadata metadata) {
        Result result = transaction.run("Match (doc:Document) where doc.name = $name return doc",
                Values.parameters(
                        "name",
                        metadata.get("name")));
        if (result.hasNext())
            return true;
        return false;
    }

    private Result addDocument(final Transaction transaction, final Metadata metadata) {

        StringBuilder stringBuilder = new StringBuilder();
        String comma = ",";
        String name = "";
        Object data = null;

        Map<String, Object> parameters = new HashMap<String, Object>();

        StringFormatter.format("{0}: ${0}", name);
        for (int index = 0; index < metadata.names().length; ++index) {
            name = metadata.names()[index];
            data = metadata.get(metadata.names()[index]);
            if (index == metadata.names().length - 1) {
                comma = "";
            }
            stringBuilder.append(StringFormatter.format("{0}: ${0}{1}", name, comma));
            parameters.put(name, data);
        }
        stringBuilder.append(StringFormatter.format("CREATE (:Document {{{0}}})", stringBuilder.toString()));
        return transaction.run(stringBuilder.toString(), parameters);
    }

    private boolean existKeyWord(final Transaction transaction, final String name) {
        Result result = transaction.run("Match (keyw:Keyword) where keyw.name = $name return keyw",
                Values.parameters(
                        "name",
                        name));
        if (result.hasNext())
            return true;
        return false;
    }

    private Result addKeyword(final Transaction transaction, final String name) {
        return transaction.run("CREATE (:Keyword {name: $name})",
                Values.parameters(
                        "name",
                        name));
    }

    public void createOntologyEntries(List<OntologyDTO> dtos) {

        try(Session session = driver.session()) {

            Map<String, OntologyDTO> distinctList = new HashMap<String, OntologyDTO>();
            for (OntologyDTO dto : dtos) {
                session.writeTransaction(transaction -> addOntologyEntry(transaction, dto));
                // simple way to distinct ontology types
                if (!distinctList.containsKey(dto.getType())) {
                    distinctList.put(dto.getType(), dto);
                }
            }

            for (String key : distinctList.keySet()) {
                session.writeTransaction(transaction -> addOntologyType(transaction, distinctList.get(key)));
            }

        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }
    }

    public void createALKISEntries(List<ALKISDTO> dtos) {
        try(Session session = driver.session()) {

            for (ALKISDTO dto : dtos) {
                session.writeTransaction(transaction -> addALKISEntry(transaction, dto));
            }
        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }
    }

    public void close() {
        if (session.isOpen())
            session.close();
        driver.close();

    }

    private Result linkOntologyTypeAndDocument(final Transaction transaction, final OntologyDTO ontologyDTO, final Metadata documentMetadata) {
        return transaction.run("Match (ont:OntologyType) ,(doc:Document) where ont.type = $ontologyType and doc.name = $name"
                + " Create (doc)-[:listed] ->(add)",
                Values.parameters("ontologyType", ontologyDTO.getType(), "$name", documentMetadata.get("name")));
    }

    private boolean isLinked(final Transaction transaction, final OntologyDTO ontologyDTO, final Metadata documentMetadata) {
        Result result = transaction.run("Match edge=(ont:OntologyType{type: $type})-[:listed]->(doc:Document{name: $name}) return edge",
                Values.parameters("type", ontologyDTO.getType(), "name", documentMetadata.get("name")));
        if (result.hasNext())
            return true;
        return false;
    }

    private Result linkOntologyTypeAndKeyword(final Transaction transaction, final OntologyDTO ontologyDTO, final String keyword) {
        return transaction.run("Match (ont:OntologyType), (keyw:Keyword) where ont.type = $ontologyType and keyw.name = $name"
                + " Create (keyw)-[:partof] ->(ont)",
                Values.parameters("ontologyType", ontologyDTO.getType(), "$name", keyword));
    }

    private boolean isLinked(final Transaction transaction, final OntologyDTO ontologyDTO, final String keyword) {
        Result result = transaction.run("Match edge=(ont:OntologyType{type: $type})-[:listed]->(Keyword{name: $keyword}) return edge",
                Values.parameters("type", ontologyDTO.getType(), "keyword", keyword));
        if (result.hasNext())
            return true;
        return false;
    }

    private Result linkDocumentAndKeyword(final Transaction transaction, final Metadata documentMetadata, final String keyword, final double weight) {
        return transaction.run("Match (doc:Document), (keyw:Keyword) where doc.name = $name and keyw.name = $keyowrd"
                + " Create (doc)-[:contains{weight:$weight} ] ->(keyw)",
                Values.parameters("name",
                        documentMetadata.get("name"),
                        "keyowrd",
                        keyword,
                        "weight",
                        weight));
    }

    private boolean isLinked(final Transaction transaction, final Metadata documentMetadata, final String keyword) {
        Result result = transaction.run("Match edge=(doc:Document{name: $name})-[:listed]->(Keyword{name: $keyword}) return edge",
                Values.parameters("name", documentMetadata.get("name"), "keyword", keyword));
        if (result.hasNext())
            return true;
        return false;
    }

    private Result linkDocumentAndALKIS(final Transaction transaction, final Metadata documentMetadata, final ALKISDTO alkisdto, final double weight) {
        return transaction.run("Match (doc:Document), (alkis:ALKISEntry) where doc.name = $name and keyw.name = $alkisname"
                + " Create (doc)-[:contains{weight:$weight} ] ->(alkis)",
                Values.parameters("name",
                        documentMetadata.get("name"),
                        "alkisname",
                        alkisdto.getName(),
                        "weight",
                        weight));
    }

    private boolean isLinked(final Transaction transaction, final Metadata documentMetadata, final ALKISDTO alkisdto) {
        Result result = transaction.run("Match edge=(doc:Document{name: $filename})-[:contains]->(ALKISEntry{name: $alkisname}) return edge",
                Values.parameters("filename", documentMetadata.get("name"), "alkisname", alkisdto.getName()));
        if (result.hasNext())
            return true;
        return false;
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

    public <T> void persist(Map<String, Pair<T, Double>> filteredKeyWords, Metadata metadata) {

        try(Session session = driver.session()) {

            Pair<T, Double> pair;
            Transaction transaction = session.beginTransaction();

            if (!existDocument(transaction, metadata)) {
                addDocument(transaction, metadata);
            }
            for (String key : filteredKeyWords.keySet()) {
                pair = filteredKeyWords.get(key);
                T dto = pair.getLeft();
                if (!existKeyWord(transaction, key)) {
                    addKeyword(transaction, key);
                }
                if (!isLinked(transaction, metadata, key)) {
                    linkDocumentAndKeyword(transaction, metadata, key, pair.getRight());
                }
                if (dto instanceof OntologyDTO) {
                    OntologyDTO ontologyDTO = (OntologyDTO)dto;
                    if (!this.existOntologyType(transaction, ontologyDTO)) {
                        addOntologyEntry(transaction, ontologyDTO);
                        addOntologyType(transaction, ontologyDTO);
                    }
                    if (!isLinked(transaction, ontologyDTO, metadata)) {
                        linkOntologyTypeAndDocument(transaction, ontologyDTO, metadata);
                    }

                }

                if (dto instanceof ALKISDTO) {
                    ALKISDTO ontologyDTO = (ALKISDTO)dto;

                }

            }

        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }

    }

}
