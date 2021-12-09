package de.citytwin.analyser;

import de.citytwin.config.ApplicationConfiguration;
import de.citytwin.converter.DocumentConverter;
import de.citytwin.model.Location;
import de.citytwin.namedentities.NamedEntitiesExtractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.tika.sax.BodyContentHandler;
import org.geonames.InsufficientStyleException;
import org.geonames.Toponym;
import org.geonames.ToponymSearchCriteria;
import org.geonames.ToponymSearchResult;
import org.geonames.WebService;

/**
 * * this class is location extractor <br>
 *
 * @author Maik Siegmund, FH Erfurt
 */
public class DocumentLocationAnalyser implements NamedEntities, AutoCloseable {

    private DocumentConverter documentConverter = null;

    private String user = null;
    private String countryCode = null;
    private Integer maxRows = null;
    private String geoNamesServer = null;
    private Double distance = null;

    /**
     * constructor
     */
    public DocumentLocationAnalyser(final Properties properties, DocumentConverter documentConverter) {
        if (validateProperties(properties)) {
            this.documentConverter = documentConverter;
        }
    }

    @Override
    public void close() throws Exception {
        this.documentConverter = null;
    }

    @Override
    public Set<String> getNamedEntities(final File file, NamedEntitiesExtractor namedEntitiesExtractor) throws Exception {
        BodyContentHandler bodyContentHandler = documentConverter.getBodyContentHandler(file);
        List<List<String>> textcorpus = documentConverter.getCleanedTextCorpus(bodyContentHandler);
        return namedEntitiesExtractor.getNamedEntities(textcorpus);

    }

    /**
     * this method filters founded locations by distance use web service
     *
     * @param extractedLocations
     * @param origin
     * @param distance in km
     * @return filtered
     * @throws Exception
     */
    public Set<Location> filterLocations(final Set<String> extractedLocations, final Location origin) throws Exception {

        Set<Location> filteredNamedEntities = new HashSet<Location>();

        WebService.setUserName(user);
        WebService.setGeoNamesServer(geoNamesServer);

        ToponymSearchCriteria searchCriteria = new ToponymSearchCriteria();
        searchCriteria.setCountryCode(countryCode);
        searchCriteria.setMaxRows(maxRows);

        for (String extractedLocation : extractedLocations) {
            searchCriteria.setQ(extractedLocation);
            ToponymSearchResult searchResult = WebService.search(searchCriteria);
            for (Toponym toponym : searchResult.getToponyms()) {
                Location location = createLocation(toponym);
                if (Math.abs(origin.distanceTo(location)) > distance) {
                    continue;
                }
                filteredNamedEntities.add(location);
            }
        }

        return filteredNamedEntities;
    }

    /**
     * this method filters founded locations by distance
     *
     * @param extractedLocations
     * @param locations
     * @param origin
     * @param distance
     * @return
     * @throws Exception
     */
    public Set<Location> filterLocations(final Set<String> extractedLocations, final List<Location> locations, final Location origin)
            throws Exception {

        Set<Location> filteredNamedEntities = new HashSet<Location>();
        for (Location location : locations) {
            if (!locations.contains(origin)) {
                continue;
            }
            if (Math.abs(origin.distanceTo(location)) > distance) {
                continue;
            }
            filteredNamedEntities.add(location);
        }
        return filteredNamedEntities;
    }

    /**
     * this method create a new instance of location
     *
     * @param toponym
     * @return new reference of {@code Location}
     * @throws InsufficientStyleException
     */
    private Location createLocation(Toponym toponym) throws InsufficientStyleException {
        return new Location(toponym.getName(),
                toponym.getFeatureCode(),
                toponym.getLatitude(),
                toponym.getLongitude(),
                new HashSet<String>(Arrays.asList(toponym.getAlternateNames().split(","))));
    }

    private Boolean validateProperties(final Properties properties) {

        user = properties.getProperty(ApplicationConfiguration.GEONAMES_USER);
        if (user == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.GEONAMES_USER);
        }
        countryCode = properties.getProperty(ApplicationConfiguration.GEONAMES_COUNTRYCODE);
        if (countryCode == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.GEONAMES_COUNTRYCODE);
        }
        String property = properties.getProperty(ApplicationConfiguration.GEONAMES_MAXROWS);
        if (property == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.GEONAMES_MAXROWS);
        }
        maxRows = Integer.parseInt(property);
        geoNamesServer = properties.getProperty(ApplicationConfiguration.GEONAMES_WEBSERVICE);
        if (geoNamesServer == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.GEONAMES_WEBSERVICE);
        }
        property = properties.getProperty(ApplicationConfiguration.MAX_DISTANCE);
        if (property == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.MAX_DISTANCE);
        }
        distance = Double.parseDouble(property);

        return true;
    }

    public static Properties getDefaultProperties() {
        Properties properties = new Properties();
        properties.setProperty(ApplicationConfiguration.GEONAMES_USER, "demo");
        properties.setProperty(ApplicationConfiguration.GEONAMES_COUNTRYCODE, "de");
        properties.setProperty(ApplicationConfiguration.GEONAMES_MAXROWS, "10");
        properties.setProperty(ApplicationConfiguration.GEONAMES_WEBSERVICE, "api.geonames.org");
        properties.setProperty(ApplicationConfiguration.MAX_DISTANCE, "1.0d");
        return properties;
    }

    private static List<String> getGeoNamesDumpContent(String zipFileURL, String zipedFileName) throws IOException {

        List<String> lines = new ArrayList<String>();
        try(ZipInputStream zipInputStream = new ZipInputStream(new URL(zipFileURL).openStream())) {

            ZipEntry zipEntry = null;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.getName().equals(zipedFileName)) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream));
                    while (reader.ready()) {
                        lines.add(reader.readLine());
                    }
                    break;
                }
            }
        }
        return lines;
    }

    public List<Location> createLocations(List<String> lines) {

        // Structure // tabs separate
        // 00 geonameid : integer id of record in geonames database
        // 01 name : name of geographical point (utf8) varchar(200)
        // 02 asciiname : name of geographical point in plain ascii characters, varchar(200)
        // 03 alternatenames : alternatenames, comma separated, ascii names automatically transliterated, convenience attribute from alternatename table,
        // varchar(10000)
        // 04 latitude : latitude in decimal degrees (wgs84)
        // 05 longitude : longitude in decimal degrees (wgs84)
        // 06 feature class : see http://www.geonames.org/export/codes.html, char(1)
        // 07 feature code : see http://www.geonames.org/export/codes.html, varchar(10)
        // 08 country code : ISO-3166 2-letter country code, 2 characters
        // 09 cc2 : alternate country codes, comma separated, ISO-3166 2-letter country code, 200 characters
        // 10 admin1 code : fipscode (subject to change to iso code), see exceptions below, see file admin1Codes.txt for display names of this code; varchar(20)
        // 11 admin2 code : code for the second administrative division, a county in the US, see file admin2Codes.txt; varchar(80)
        // 12 admin3 code : code for third level administrative division, varchar(20)
        // 13 admin4 code : code for fourth level administrative division, varchar(20)
        // 14 population : bigint (8 byte int)
        // 15 elevation : in meters, integer
        // 16 dem : digital elevation model, srtm3 or gtopo30, average elevation of 3''x3'' (ca 90mx90m) or 30''x30'' (ca 900mx900m) area in meters, integer.
        // srtm
        // 17 processed by cgiar/ciat.
        // 18 timezone : the iana timezone id (see file timeZone.txt) varchar(40)
        // 19 modification date : date of last modification in yyyy-MM-dd format

        // https://download.geonames.org/export/dump/admin1CodesASCII.txt
        // DE.15 Thuringia Thuringia 2822542
        // DE.10 Schleswig-Holstein Schleswig-Holstein 2838632
        // DE.14 Saxony-Anhalt Saxony-Anhalt 2842565
        // DE.13 Saxony Saxony 2842566
        // DE.09 Saarland Saarland 2842635
        // DE.08 Rheinland-Pfalz Rheinland-Pfalz 2847618
        // DE.07 North Rhine-Westphalia North Rhine-Westphalia 2861876
        // DE.06 Lower Saxony Lower Saxony 2862926
        // DE.12 Mecklenburg-Vorpommern Mecklenburg-Vorpommern 2872567
        // DE.05 Hesse Hesse 2905330
        // DE.04 Hamburg Hamburg 2911297
        // DE.03 Bremen Bremen 2944387
        // DE.11 Brandenburg Brandenburg 2945356
        // DE.16 Berlin Berlin 2950157
        // DE.02 Bavaria Bavaria 2951839
        // DE.01 Baden-Württemberg Baden-Wuerttemberg 2953481

        List<Location> locations = new ArrayList<Location>();

        for (String line : lines) {
            String[] lineParts = line.split("\t");
            String name = lineParts[Location.INDEX_NAME];
            Set<String> synonyms = new HashSet<String>(Arrays.asList(lineParts[Location.INDEX_SYNONYMS].split(",")));
            double latitude = Double.parseDouble(lineParts[Location.INDEX_LATITUDE]);
            double longitude = Double.parseDouble(lineParts[Location.INDEX_LONGITUDE]);
            String featureCode = lineParts[Location.INDEX_FEATURECODE];
            locations.add(new Location(name, featureCode, latitude, longitude, synonyms));
        }
        return locations;

    }

}
