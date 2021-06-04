package de.citytwin;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

import edu.stanford.nlp.*;
import edu.stanford.nlp.coref.CorefProperties;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

public class TextCleaner {
	private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static SentenceDetectorME sentenceDetector;
	private static BodyContentHandler bodyContentHandler;
	// 2.4 Planerische Ausgangssituation
	// ............................................................................
	// 14
	private static String tabelOfContentNumeralRegEx = "^[\\d+\\.]+[\\s\\wäÄüÜöÖß,§-]+\\.{2,}\\s*\\d*\\s*";
	// example II.
	// Planinhalt..................................................................................
	private static String tabelOfContentRomanPattern = "[I|V|X]+\\.*\\s*[\\w ]*\\.{3,}\\s*\\d*";

	public TextCleaner(BodyContentHandler bodyContentHandler) throws IOException {

		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("de-sent.bin");
		SentenceModel sentenceModel = new SentenceModel(inputStream);
		sentenceDetector = new SentenceDetectorME(sentenceModel);
		inputStream.close();
		this.bodyContentHandler = bodyContentHandler;

	}

	public List<String> getSentences() throws IOException {
		List<String> results = spliteBodyContentToSencences();

		// results = removePattern(results, tabelOfContentNumeralPattern); // hyponated
		// words
		results = removePattern(results, tabelOfContentRomanPattern); // hyponated words
		results = removePattern(results, "\n"); // new lines

		return results;

	}

	private List<String> spliteBodyContentToSencences() throws IOException {

		List<String> results = new ArrayList<>();

		String[] sentences = sentenceDetector.sentDetect(bodyContentHandler.toString());
		for (String sentence : sentences) {
			results.add(sentence);
		}
		logger.info(MessageFormat.format("textcorpus contains {0} sentences.", results.size()));
		return results;
	}

	private List<String> removePattern(final List<String> senteneces, String pattern) {
		List<String> results = new ArrayList<String>();
		String replaced = "";
		String temp = "";
		for (String sentence : senteneces) {
			replaced = sentence;
			replaced = replaced.replace("Ü", "U");
			replaced = replaced.replace("Ö", "O");
			replaced = replaced.replace("Ä", "A");
			replaced = replaced.replace("ü", "u");
			replaced = replaced.replace("ö", "o");
			replaced = replaced.replace("ä", "a");
			replaced = replaced.replace("ß", "ss");
			replaced = replaced.replaceAll(pattern, "");
			String[] parts = replaced.split("\n");

			for (String part : parts) {
				temp = part.replace(pattern, "");
				if (!temp.isEmpty() && !temp.isBlank())
					results.add(temp);
			}

		}
		return results;
	}

	public void test() {

		
//		final Pattern pattern = Pattern.compile(this.tabelOfContentNumeralRegEx, Pattern.MULTILINE);
//		final Matcher matcher = pattern.matcher(sentence);
//		String result = matcher.replaceAll("");

		Properties props = new Properties();

		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse, sentiment");

		StanfordCoreNLP pipeline = new StanfordCoreNLP("german");
		Annotation annotation = pipeline.process(text);
		List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			List<CoreLabel> sentiments = sentence.get(CoreAnnotations.TokensAnnotation.class);
			for (CoreLabel sentiment : sentiments) {
				sentiment.toString();
			}

		}

	}

	private static String getRomanNumeralsAsAlternation(int n) {

		if (n < 0)
			throw new IllegalArgumentException("n must be largern than 0");

		String[] romanNumerals = { "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII",
				"XIV", "XV", "XVI", "XVIII", "XIX", "XX", "XXI", "XXII", "XXIII", "XXIV", "XXV", "XXVI", "XXVII",
				"XXVIII", "XXIX", "XXX", "XXXI", "XXXII", "XXXIII", "XXXIV", "XXXV", "XXXVI", "XXXVII", "XXXVIII",
				"XXXIX", "XL", "XLI", "XLII", "XLIII", "XLIV", "XLV", "XLVI", "XLVII", "XLVIII", "XLIX", "L", "LI",
				"LII", "LIII", "LIV", "LV", "LVI", "LVII", "LVIII", "LIX", "LX", "LXI", "LXII", "LXIII", "LXIV", "LXV",
				"LXVI", "LXVII", "LXVIII", "LXIX", "LXX", "LXXI", "LXXII", "LXXIII", "LXXIV", "LXXV", "LXXVI", "LXXVII",
				"LXXVIII", "LXXIX", "LXXX", "LXXXI", "LXXXII", "LXXXIII", "LXXXIV", "LXXXV", "LXXXVI", "LXXXVII",
				"LXXXVIII", "LXXXIX", "XC", "XCI", "XCII", "XCIII", "XCIV", "XCV", "XCVI", "XCVII", "XCVIII", "XCIX",
				"C" };
		StringBuilder stringBuilder = new StringBuilder();
		String result = "";

		for (int index = 0; index < n && index < romanNumerals.length; ++index) {
			stringBuilder.append(romanNumerals[index] + "|");
		}
		result = stringBuilder.toString();
		return result.substring(result.length() - 1);
	}


	
	public final static String text = "Bezirksamt Lichtenberg von Berlin \n" + "Abteilung Stadtentwicklung \n"
			+ "Stadtentwicklungsamt \n" + "Fachbereich Stadtplanung \n\n" + " \n" + "    \n\n" + " \n"
			+ "Begründung zum \n\n" + "Bebauungsplan XVII-50aa \n" + " \n\n"
			+ "für das Gelände zwischen Robert-Siewert-Straße, einer Linie zwischen Beerfelder Straße \n"
			+ "und Biesenhorster Weg, einer Linie nördlich des Flurstücks 346 (Deutsch-Russisches \n\n"
			+ "Museum) und Zwieseler Straße sowie für Teilabschnitte der Zwieseler Straße \n"
			+ "und der Robert-Siewert-Straße  \n\n" + " \n" + "im Bezirk Lichtenberg, Ortsteil Karlshorst \n\n"
			+ " \n\n" + " \n" + " \n\n" + "Festsetzung gemäß § 10 Absatz 1 BauGB \n" + " \n\n" + "Juli 2014 \n"
			+ " \n\n" + " \n\n" + " \n\n\n\n" + " \n\n\n\n" + "Bebauungsplan XVII-50aa Begründung \n\n" + " \n\n"
			+ "Juli 2014 1 \n\n" + "Inhalt \n" + " \n"
			+ "A Begründung ..................................................................................................................... 5 \n\n"
			+ "I. Planungsgegenstand......................................................................................................... 5 \n\n"
			+ "1. Veranlassung und Erforderlichkeit ................................................................................ 5 \n\n"
			+ "2. Plangebiet..................................................................................................................... 6 \n\n"
			+ "2.1 Geltungsbereich..................................................................................................... 6 \n\n"
			+ "2.2 Stadträumliche Einordnung .................................................................................... 7 \n\n"
			+ "2.3 Bestand.................................................................................................................. 8 \n\n"
			+ "2.4 Planerische Ausgangssituation ............................................................................ 14 \n\n"
			+ "II. Planinhalt......................................................................................................................... 22 \n\n"
			+ "1. Entwicklung der Planungsüberlegungen ..................................................................... 22 \n\n"
			+ "2. Intentionen des Plans ................................................................................................. 24 \n\n"
			+ "2.1 Planungsziele....................................................................................................... 24 \n\n"
			+ "2.2 Städtebauliches Konzept...................................................................................... 24 \n\n"
			+ "3. Umweltbericht ............................................................................................................. 29 \n\n"
			+ "3.1 Einleitung ............................................................................................................. 29 \n\n"
			+ "3.2 Beschreibung und Bewertung der Umweltauswirkungen...................................... 33 \n\n"
			+ "3.3 Zusätzliche Angaben............................................................................................ 80 \n\n"
			+ "4. Wesentlicher Planinhalt, Abwägung und Begründung einzelner Festsetzungen ......... 86 \n\n"
			+ "4.1 Grundzüge der Abwägung ................................................................................... 86 \n\n"
			+ "4.2 Art der baulichen Nutzung.................................................................................... 87 \n\n"
			+ "4.3 Maß der baulichen Nutzung ................................................................................. 89 \n\n"
			+ "4.4 Bauweise ............................................................................................................. 93 \n\n"
			+ "4.5 Überbaubare und nicht überbaubare Grundstücksflächen ................................... 93 \n\n"
			+ "4.6 Straßenverkehrsflächen ....................................................................................... 96 \n\n"
			+ "4.7 Gehrechte ............................................................................................................ 98 \n\n"
			+ "4.8 Denkmalschutz .................................................................................................... 99 \n\n"
			+ "4.9 Öffentliche Grünflächen ....................................................................................... 99 \n\n"
			+ "4.10 Grünfestsetzungen.................................................................................... 100 \n\n"
			+ "4.11 Gestalterische Festsetzungen................................................................... 101 \n\n"
			+ "4.12 Festsetzungen zum Immissionsschutz...................................................... 105 \n\n"
			+ "4.13 Sonstige Regelungen und nicht getroffene Regelungen ........................... 107 \n\n"
			+ "4.14 Nachrichtliche Übernahmen...................................................................... 109 \n\n"
			+ "4.15 Hinweise ................................................................................................... 109 \n\n"
			+ "4.16 Städtebauliche Kennzahlen....................................................................... 110 \n\n"
			+ "III.Auswirkungen des Bebauungsplans.............................................................................. 111 \n\n"
			+ "1. Stadtplanerische Auswirkungen................................................................................ 111 \n\n\n\n"
			+ "Begründung  Bebauungsplan XVII-50aa \n\n" + " \n\n" + "2 Juli 2014 \n\n"
			+ "2. Auswirkungen auf die Wohnnutzung und private Eigentümer....................................111 \n\n"
			+ "3. Auswirkungen auf den Bedarf an Einrichtungen der sozialen Infrastruktur, Sport- und \n"
			+ "Grünflächen...............................................................................................................111 \n\n"
			+ "3.1 Einrichtungen der sozialen Infrastruktur .............................................................112 \n\n"
			+ "3.2 Versorgung mit Grün- und Freiflächen................................................................112 \n\n"
			+ "4. Verkehrliche Auswirkungen .......................................................................................113 \n\n"
			+ "5. Auswirkungen auf die Lärmbelastung........................................................................114 \n\n"
			+ "6. Auswirkungen auf die Umwelt ...................................................................................115 \n\n"
			+ "IV.Verfahren.......................................................................................................................117 \n\n"
			+ "1. Information der Senatsverwaltungen .........................................................................117 \n\n"
			+ "2. Bezirksamtsbeschluss zur Aufstellung des Bebauungsplans und zur Durchführung \n"
			+ "der frühzeitigen Bürgerbeteiligung.............................................................................117 \n\n"
			+ "3. Öffentliche Bekanntmachung des Bezirksamtsbeschlusses ......................................117 \n\n"
			+ "4. Information der Senatsverwaltungen sowie der Gemeinsamen \n"
			+ "Landesplanungsabteilung zur beabsichtigten Teilung des Bebauungsplans .............117 \n\n"
			+ "5. Bezirksamtsbeschluss zur Teilung des Bebauungsplans...........................................117 \n\n"
			+ "6. Öffentliche Bekanntmachung des Bezirksamtsbeschlusses ......................................118 \n\n"
			+ "7. Frühzeitige Bürgerbeteiligung....................................................................................118 \n\n"
			+ "8. Erneute frühzeitige Bürgerbeteiligung .......................................................................118 \n\n"
			+ "9. Bezirksamtsbeschluss zum Ergebnis der frühzeitigen Bürgerbeteiligungen und zur \n"
			+ "Durchführung der Beteiligung der Träger öffentlicher Belange..................................120 \n\n"
			+ "10. Frühzeitige Beteiligung der Behörden und sonstigen Träger öffentlicher Belange \n"
			+ "sowie der Fachämter des Bezirksamts ......................................................................121 \n\n"
			+ "11. Bezirksamtsbeschluss zum Ergebnis der frühzeitigen Behördenbeteiligung und zur \n"
			+ "Durchführung der Behördenbeteiligung .....................................................................128 \n\n"
			+ "12. Beteiligung der Behörden und sonstigen Träger öffentlicher Belange sowie der \n"
			+ "Fachämter des Bezirksamts ......................................................................................128 \n\n"
			+ "13. Bezirksamtsbeschluss zum Ergebnis der Behördenbeteiligung und zur Durchführung \n"
			+ "der Öffentlichkeitsbeteiligung ....................................................................................132 \n\n"
			+ "14. Information der Senatsverwaltung sowie der Gemeinsamen \n"
			+ "Landesplanungsabteilung zur beabsichtigten Teilung des Bebauungsplans .............132 \n\n"
			+ "15. Bezirksamtsbeschluss zur Teilung des Bebauungsplans .........................................132 \n\n"
			+ "16. Öffentliche Bekanntmachung des Bezirksamtsbeschlusses.....................................132 \n\n"
			+ "17. Bezirksamtsbeschluss zur Änderung des Geltungsbereichs des Bebauungsplans ..133 \n\n"
			+ "18. Öffentliche Bekanntmachung des Bezirksamtsbeschlusses.....................................133 \n\n"
			+ "19. Erneute Beteiligung der Behörden und der sonstigen Träger öffentlicher Belange \n"
			+ "sowie der Fachämter des Bezirksamts ......................................................................133 \n\n"
			+ "20. Öffentlichkeitsbeteiligung .........................................................................................147 \n\n"
			+ "21.Bezirksamtsbeschluss zum Ergebnis der Öffentlichkeits- und Behördenbeteiligung \n"
			+ "sowie zum Bebauungsplan-Entwurf...........................................................................155 \n\n\n\n"
			+ "Bebauungsplan XVII-50aa Begründung \n\n" + " \n\n" + "Juli 2014 3 \n\n"
			+ "22. Beschluss der Bezirksverordnetenversammlung zum Bebauungsplan-Entwurf XVII-\n"
			+ "50aa und zur Verordnung über die Festsetzung des Bebauungsplan-Entwurfs ........ 156 \n\n"
			+ "23.Anzeige des Bebauungsplans gemäß § 6 Absatz 4 AGBauGB ............................... 156 \n\n"
			+ "24. Eingeschränkte Beteiligung gemäß § 4a Absatz 3 BauGB ...................................... 156 \n\n"
			+ "25. Bezirksamtsbeschluss zum Ergebnis der Eingeschränkten Beteiligung gemäß § 4a \n"
			+ "Absatz 3 BauGB ....................................................................................................... 157 \n\n"
			+ "26. Beschluss der Bezirksverordnetenversammlung zum Bebauungsplan-Entwurf XVII-\n"
			+ "50aa und zur Verordnung über die Festsetzung des Bebauungsplan-Entwurfs ........ 157 \n\n"
			+ "27. Erneute Anzeige des Bebauungsplans gemäß § 6 Absatz 4 AGBauGB.................. 158 \n\n"
			+ "28. Bezirksamtsbeschluss zur Rechtsverordnung gemäß § 4a Absatz 3 BauGB .......... 158 \n\n"
			+ "29. Veröffentlichung der Rechtsverordnung im Gesetz- und Verordnungsblatt für Berlin158 \n\n"
			+ "30. Kenntnisnahme der BVV zur Festsetzung des Bebauungsplans ............................. 158 \n\n"
			+ "B. Rechtsgrundlagen....................................................................................................... 159 \n\n"
			+ "C. Auswirkungen auf den Haushaltsplan und die Finanzierbarkeit ............................. 160 \n\n"
			+ "I.Auswirkungen auf Einnahmen und Ausgaben ................................................................... 160 \n\n"
			+ "II. Personalwirtschaftliche Auswirkungen ............................................................................. 160 \n\n"
			+ "D. Anhang......................................................................................................................... 161 \n\n"
			+ "Abkürzungsverzeichnis ..................................................................................................... 161 \n\n"
			+ "Quellenverzeichnis............................................................................................................. 162 \n\n"
			+ "Verzeichnis der textlichen Festsetzungen ....................................................................... 163 \n\n"
			+ "Anlagen:.............................................................................................................................. 168 \n\n"
			+ " \n\n\n\n" + "Begründung  Bebauungsplan XVII-50aa \n\n" + " \n\n" + "4 Juli 2014 \n\n" + " \n\n\n\n"
			+ "Bebauungsplan XVII-50aa Begründung \n\n" + " \n\n" + "Juli 2014 5 \n\n" + "A Begründung \n\n" + " \n"
			+ "I.Planungsgegenstand \n\n" + " \n" + "1. Veranlassung und Erforderlichkeit \n\n" + " \n"
			+ "Die ursprüngliche Veranlassung für die Aufstellung des Bebauungsplans XVII-50, aus \n"
			+ "dem im Laufe der Jahre zunächst die Bebauungspläne XVII-50a, XVII-50b und XVII-\n"
			+ "50c und unlängst die Bebauungspläne XVII-50aa und XVII-50ab hervorgegangen sind, \n"
			+ "war der 1993 eingeleitete Abzug der GUS-Streitkräfte und die hiermit verbundene \n"
			+ "Übergabe der ehemals militärisch genutzten Flächen an die Bundesrepublik Deutsch-\n"
			+ "land, an das Land Berlin und an verschiedene Alteigentümer.Mit der Grundstücks-\n"
			+ "übergabe waren die Flächen östlich der Zwieseler Straße wieder der gemeindlichen \n"
			+ "Bauleitplanung zugänglich und es bestand das Erfordernis, planerische Konzepte für \n"
			+ "die frei gewordenen Flächen zu entwickeln und diese durch eine abgestimmte Bauleit-\n"
			+ "planung zu sichern.Sowohl der 1996 aus einem Wettbewerb hervorgegangene städtebauliche Entwurf des \n"
			+ "Büros Springer Architekten, als auch die in den darauf folgenden Jahren entwickelten \n"
			+ "Konzeptionen sahen und sehen vor, das ehemalige Kasernenareal unter Berücksichti-\n"
			+ "gung des denkmalgeschützten Gebäudebestands zu einem neuen durchgrünten \n"
			+ "Wohnstandort zu entwickeln, der sich hinsichtlich der baulichen Dichte und seiner Nut-\n"
			+ "zungsstruktur am vorhandenen Bestand innerhalb und westlich des Plangebiets orien-\n"
			+ "tiert und sich gegenüber den östlich anschließenden, von zusätzlicher Bebauung frei-\n"
			+ "zuhaltenden Flächen deutlich abgrenzt.Bei den letzt genannten Flächen handelt es ";
}
