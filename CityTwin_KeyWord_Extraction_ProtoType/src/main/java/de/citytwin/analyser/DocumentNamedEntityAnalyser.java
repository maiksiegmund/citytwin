package de.citytwin.analyser;

import de.citytwin.config.ApplicationConfiguration;
import de.citytwin.converter.DocumentConverter;
import de.citytwin.database.PostgreSQLController;
import de.citytwin.model.Address;
import de.citytwin.model.Location;
import de.citytwin.namedentities.NamedEntitiesExtractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.tika.sax.BodyContentHandler;
import org.geonames.InsufficientStyleException;
import org.geonames.Toponym;
import org.geonames.ToponymSearchCriteria;
import org.geonames.ToponymSearchResult;
import org.geonames.WebService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * * this class provides document named entity analyser <br>
 *
 * @author Maik Siegmund, FH Erfurt
 */
public class DocumentNamedEntityAnalyser implements NamedEntities, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private DocumentConverter documentConverter = null;

    private String user = null;
    private String countryCode = null;
    private Integer maxRows = null;
    private String geoNamesServer = null;
    private Double maxDistanceInMeters = null;
    private String url2DumpFile = null;
    private String zipEntry = null;

    private String originName = null;
    private Double originLatitude = null;
    private Double originLongitude = null;
    private Location originLocation = null;

    private Integer maxStreetCount = null;
    private Integer minNamedEntityLength = null;
    private Integer maxSectionCount = null;

    public Location getOriginLocation() {
        return originLocation;
    }

    public void setOriginLocation(Location originLocation) {
        this.originLocation = originLocation;
    }

    /**
     * constructor
     */
    public DocumentNamedEntityAnalyser(final Properties properties, final DocumentConverter documentConverter) {
        if (validateProperties(properties)) {
            this.documentConverter = documentConverter;
            this.originLocation = new Location(originName, countryCode, originLatitude, originLongitude, new HashSet<String>());
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

    public Set<Location> filterNamedEntities(final Set<String> extractedLocations) throws Exception {
        return filterNamedEntities(extractedLocations, originLocation, maxDistanceInMeters);
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
    public Set<Location> filterNamedEntities(final Set<String> extractedLocations, final Location origin, double distance) throws Exception {

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

    public Set<Location> filterNamedEntities(final Set<String> extractedLocations, final List<Location> locations) throws Exception {
        return filterNamedEntities(extractedLocations, locations, originLocation, maxDistanceInMeters);
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
    public Set<Location> filterNamedEntities(final Set<String> extractedLocations, final List<Location> locations, final Location origin, double distance)
            throws Exception {

        Set<Location> filteredNamedEntities = new HashSet<Location>();
        for (String extractedlocation : extractedLocations) {
            List<Location> foundeds = locations.stream().filter(location -> extractedlocation.equals(location.getName())).collect(Collectors.toList());
            for (Location founded : foundeds) {
                if (Math.abs(origin.distanceTo(founded)) > distance) {
                    continue;
                }
                filteredNamedEntities.add(founded);
            }
        }
        return filteredNamedEntities;
    }

    /**
     * this method filter locations via db
     *
     * @param extractedLocations
     * @param controller
     * @return new reference of {@code Set<Location>}
     * @throws Exception
     */
    public Set<Location> validateLocations(final Set<String> extractedLocations, PostgreSQLController controller) throws Exception {
        return validateLocations(extractedLocations, controller, getOriginLocation(), maxDistanceInMeters);
    }

    public Set<Location> validateLocations(final Set<String> extractedLocations, PostgreSQLController controller, final Location origin,
            double distanceInMeters)
            throws Exception {

        Set<Location> locations = new HashSet<Location>();
        for (String extractedLocation : extractedLocations) {
            String[] extractedLocationParts = extractedLocation.split(" ");
            for (String extractedLocationPart : extractedLocationParts) {
                if (extractedLocationPart.matches(".*\\d.*") || extractedLocationPart.length() <= minNamedEntityLength) {
                    continue;
                }
                LOGGER.info(MessageFormat.format("query db for {0}", extractedLocationPart));
                // synoyms
                List<Long> synoymIds = controller.getIds(origin, extractedLocationPart, maxDistanceInMeters);
                for (Long synoymId : synoymIds) {
                    Location location = controller.getLocation(synoymId);
                    locations.add(location);
                }
                // geonames
                Location tempLocation = new Location(extractedLocationPart, "", 0, 0, new HashSet<String>());
                List<Long> locationIds = controller.getIds(origin, tempLocation, distanceInMeters);
                for (Long locationId : locationIds) {
                    Location location = controller.getLocation(locationId);
                    locations.add(location);
                }
            }
        }
        return locations;
    }

    public Set<Address> validateAddresses(final Set<String> extractedLocations, PostgreSQLController controller)
            throws ClassNotFoundException, SQLException {
        Set<Address> addresses = new HashSet<Address>();
        for (String extractedLocation : extractedLocations) {
            Set<Address> queryAddresses = getAddresses(extractedLocation);
            LOGGER.info(queryAddresses.toString());
            for (Address queryAddress : queryAddresses) {
                Long count = controller.countOfAddresses(queryAddress);
                if (count > 0 && count <= maxStreetCount) {
                    Set<String> sections = controller.getSections(queryAddress);
                    Set<String> foundedSections = sections.stream().filter(section -> extractedLocations.contains(section)).collect(Collectors.toSet());
                    if (foundedSections.size() <= maxSectionCount) {
                        for (String foundedSection : foundedSections) {
                            Address address = new Address(queryAddress.getName(), queryAddress.getHnr(), queryAddress.getHnr_zusatz(), foundedSection);
                            for (Long id : controller.getIds(address)) {
                                addresses.add(controller.getAddress(id));
                            }
                        }
                    }
                }
            }

        }
        return addresses;
    }

    private Set<Address> getAddresses(String string) {

        Set<Address> results = new HashSet<Address>();
        String[] addressParts;
        Boolean containsDigits = string.matches(".*\\d.*");

        results.add(new Address(string.trim(), 0.0d, null));

        if (!containsDigits) {
            String temp = "";
            addressParts = string.split("[/ ]");
            for (String addressPart : addressParts) {
                temp += addressPart + " ";
                results.add(new Address(temp.trim(), 0.0d, null));
                results.add(new Address(addressPart.trim(), 0.0d, null));
            }

        } else {
            addressParts = string.split(" ");
            String temp = "";
            for (String addressPart : addressParts) {
                if (addressPart.matches(".*\\d.*")) {
                    String[] numberParts = addressPart.split("[/-]");
                    for (int index = 0; index < numberParts.length; ++index) {
                        char[] chars = numberParts[index].toCharArray();
                        String tempNumber = "";
                        String tempHnrAdditional = null;
                        for (char chr : chars) {

                            if (Character.isDigit(chr)) {
                                tempNumber += Character.toString(chr);

                            }
                            if (Character.isLetter(chr)) {
                                tempHnrAdditional = Character.toString(chr);
                            }
                        }
                        if (tempNumber.trim().length() == 0) {
                            tempNumber = "0.0d";
                        }
                        results.add(new Address(addressPart.trim(), Double.parseDouble(tempNumber), tempHnrAdditional));
                        results.add(new Address(temp.trim(), Double.parseDouble(tempNumber), tempHnrAdditional));
                    }
                } else {
                    temp += addressPart + " ";
                    results.add(new Address(temp.trim(), 0.0d, null));
                    results.add(new Address(addressPart.trim(), 0.0d, null));
                }
            }
        }
        return results;

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
        property = properties.getProperty(ApplicationConfiguration.MAX_DISTANCE_IN_METERS);
        if (property == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.MAX_DISTANCE_IN_METERS);
        }
        maxDistanceInMeters = Double.parseDouble(property);
        url2DumpFile = properties.getProperty(ApplicationConfiguration.GEONAMES_URL_2_DUMP_FILE);
        if (url2DumpFile == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.GEONAMES_URL_2_DUMP_FILE);
        }
        zipEntry = properties.getProperty(ApplicationConfiguration.GEONAMES_ZIP_ENTRY);
        if (zipEntry == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.GEONAMES_ZIP_ENTRY);
        }
        originName = properties.getProperty(ApplicationConfiguration.ORIGIN_LOCATION_NAME);
        if (originName == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.ORIGIN_LOCATION_NAME);
        }
        property = properties.getProperty(ApplicationConfiguration.ORIGIN_LOCATION_LATITUDE);
        if (property == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.ORIGIN_LOCATION_LATITUDE);
        }
        originLatitude = Double.parseDouble(property);
        property = properties.getProperty(ApplicationConfiguration.ORIGIN_LOCATION_LONGITUDE);
        if (property == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.ORIGIN_LOCATION_LONGITUDE);
        }
        originLongitude = Double.parseDouble(property);

        property = properties.getProperty(ApplicationConfiguration.MAX_STREET_COUNT);
        if (property == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.MAX_STREET_COUNT);
        }
        maxStreetCount = Integer.parseInt(property);

        property = properties.getProperty(ApplicationConfiguration.MIN_NAMED_ENTITY_LENGTH);
        if (property == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.MIN_NAMED_ENTITY_LENGTH);
        }
        minNamedEntityLength = Integer.parseInt(property);

        property = properties.getProperty(ApplicationConfiguration.MAX_SECTION_COUNT);
        if (property == null) {
            throw new IllegalArgumentException("set property --> " + ApplicationConfiguration.MAX_SECTION_COUNT);
        }
        maxSectionCount = Integer.parseInt(property);
        return true;
    }

    public static Properties getDefaultProperties() {
        Properties properties = new Properties();
        properties.setProperty(ApplicationConfiguration.GEONAMES_USER, "demo");
        properties.setProperty(ApplicationConfiguration.GEONAMES_COUNTRYCODE, "de");
        properties.setProperty(ApplicationConfiguration.GEONAMES_MAXROWS, "10");
        properties.setProperty(ApplicationConfiguration.GEONAMES_WEBSERVICE, "api.geonames.org");
        properties.setProperty(ApplicationConfiguration.GEONAMES_URL_2_DUMP_FILE, "https://download.geonames.org/export/dump/DE.zip");
        properties.setProperty(ApplicationConfiguration.GEONAMES_ZIP_ENTRY, "DE.txt");
        properties.setProperty(ApplicationConfiguration.ORIGIN_LOCATION_NAME, "Berlin");
        properties.setProperty(ApplicationConfiguration.ORIGIN_LOCATION_LATITUDE, "52.530644d");
        properties.setProperty(ApplicationConfiguration.ORIGIN_LOCATION_LONGITUDE, "13.383068d");
        properties.setProperty(ApplicationConfiguration.MAX_DISTANCE_IN_METERS, "12500.0d");
        properties.setProperty(ApplicationConfiguration.MAX_STREET_COUNT, "10");
        properties.setProperty(ApplicationConfiguration.MIN_NAMED_ENTITY_LENGTH, "5");
        properties.setProperty(ApplicationConfiguration.MAX_SECTION_COUNT, "2");
        return properties;
    }

    public List<String> getGeoNamesDumpContent(String zipFileURL, String zipedFileName) throws IOException {

        List<String> lines = new ArrayList<String>();
        try(ZipInputStream zipInputStream = new ZipInputStream(new URL(zipFileURL).openStream())) {

            ZipEntry zipEntry = null;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.getName().equals(zipedFileName)) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream, "UTF-8"));
                    while (reader.ready()) {
                        lines.add(reader.readLine());
                    }
                    break;
                }
            }
        }
        return lines;
    }

    /**
     * this method returns list of locations
     *
     * @return
     * @throws IOException
     */
    public List<Location> getLocationsBasedOnGeoNamesDump() throws IOException {
        return getLocationsBasedOnGeoNamesDump(url2DumpFile, zipEntry);
    }

    public List<Location> getLocationsBasedOnGeoNamesDump(final String zipFileURL, final String zipedFileName) throws IOException {

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

        List<String> lines = getGeoNamesDumpContent(zipFileURL, zipedFileName);
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
