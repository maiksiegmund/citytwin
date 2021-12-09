package de.citytwin.catalog;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

import de.citytwin.model.ALKIS;
import de.citytwin.model.Term;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * interface catalog entry
 *
 * @author Maik Siegmund,FH Erfurt
 * @version $Revision: 1.0 $
 * @since CityTwin_KeyWord_Extraction_ProtoType 1.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @Type(value = ALKIS.class, name = "ALKIS"),
    @Type(value = Term.class, name = "Term") })
public interface CatalogEntryHasName {

    public String getName();

}
