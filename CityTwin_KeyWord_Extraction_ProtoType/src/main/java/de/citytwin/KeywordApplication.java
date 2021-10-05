package de.citytwin;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.tika.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeywordApplication {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * get all files in a folder, run recursive
     *
     * @param path
     * @param foundedFiles reference to List of {@code List<File>}
     */

    private static void getFiles(String path, @Nonnull List<File> foundedFiles) {

        File root = new File(path);
        File[] files = root.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                getFiles(file.getAbsolutePath(), foundedFiles);
            } else {
                foundedFiles.add(file);
            }

        }
    }

    /**
     * create output folder
     *
     * @param subfolderName
     * @return {@code File}
     * @throws IOException
     */
    private static File getOutputFolder(String subfolderName) throws IOException {

        File folder = new File(Config.OUTPUT_FOLDER + "/" + subfolderName + "/");

        if (!folder.exists()) {
            boolean created = folder.mkdirs();
            if (!created) {
                throw new IOException("Ordner konnte nicht erstellt werden!");
            }
        }
        return folder;
    }

    public static void main(String[] args) {

        try {
            if (Config.exsit()) {
                Config.load();
            } else {
                Config.save();
            }
            System.out.print(Config.getConfigContent());
            List<File> files = new ArrayList<File>();
            getFiles(Config.INPUT_FOLDER, files);
            DocumentAnalyser documentAnalyser = new DocumentAnalyser();
            DBController dbController = new DBController(Config.DATABASE_URI, Config.DATABASE_USER, Config.DATABASE_PASSWORD);
            Metadata metaData = null;
            for (File file : files) {

                Map<String, Pair<ALKISDTO, Double>> alkis = documentAnalyser.analyse2ALKIS(file);
                metaData = documentAnalyser.getMetaData(file);
                dbController.persist(alkis, metaData);
            }
            documentAnalyser.close();
        } catch (Exception exception) {

        }

    }

}
