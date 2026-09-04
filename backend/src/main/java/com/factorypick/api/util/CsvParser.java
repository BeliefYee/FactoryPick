package com.factorypick.api.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class CsvParser {
    private CsvParser() {}

    public static List<Map<String, String>> parse(InputStream input) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return List.of();
            headerLine = headerLine.replace("\uFEFF", "");
            List<String> headers = split(headerLine);
            List<Map<String, String>> result = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                List<String> values = split(line);
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    row.put(headers.get(i).trim(), i < values.size() ? values.get(i).trim() : "");
                }
                result.add(row);
            }
            return result;
        }
    }

    public static List<String> split(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    value.append('"'); i++;
                } else quoted = !quoted;
            } else if (ch == ',' && !quoted) {
                values.add(value.toString()); value.setLength(0);
            } else value.append(ch);
        }
        values.add(value.toString());
        return values;
    }
}
