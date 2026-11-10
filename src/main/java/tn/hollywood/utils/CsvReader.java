package tn.hollywood.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lecteur CSV minimaliste (séparateur « , », guillemets doubles supportés, UTF-8).
 * La première ligne contient les noms de colonnes. Les lignes commençant par # sont ignorées.
 * Les fichiers sont lus depuis src/test/resources/testdata/.
 */
public final class CsvReader {

    private CsvReader() { }

    public static List<Map<String, String>> read(String fileName) {
        String path = "testdata/" + fileName;
        try (InputStream in = CsvReader.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("Fichier de données introuvable : " + path);
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            List<Map<String, String>> rows = new ArrayList<>();
            String headerLine = br.readLine();
            while (headerLine != null && (headerLine.isBlank() || headerLine.startsWith("#"))) {
                headerLine = br.readLine();                  // commentaires avant l'en-tête
            }
            if (headerLine == null) {
                return rows;
            }
            if (headerLine.startsWith("\uFEFF")) {           // BOM Excel
                headerLine = headerLine.substring(1);
            }
            List<String> headers = split(headerLine);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                List<String> cells = split(line);
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    row.put(headers.get(i).trim(), i < cells.size() ? cells.get(i) : "");
                }
                rows.add(row);
            }
            return rows;
        } catch (IOException e) {
            throw new IllegalStateException("Erreur de lecture CSV : " + fileName, e);
        }
    }

    /** Découpe une ligne CSV en respectant les guillemets. */
    static List<String> split(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                out.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString().trim());
        return out;
    }
}
