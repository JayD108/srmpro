package com.example.srmistpro;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.ArrayList;
import java.util.List;

public class HtmlParser {

    public static List<AppData.Subject> parseAttendance(String html) {
        List<AppData.Subject> list = new ArrayList<>();
        try {
            Document doc = Jsoup.parse(html);
            Elements tables = doc.select("table");

            for (Element table : tables) {
                Elements rows = table.select("tr");
                for (Element row : rows) {
                    Elements cols = row.select("td");

                    // We expect at least 6 columns for valid data
                    if (cols.size() >= 6) {
                        String code = cols.get(0).text().trim();

                        // --- FILTERING LOGIC ---
                        // 1. Skip the "Total" summary row
                        if (code.equalsIgnoreCase("Total")) continue;

                        // 2. Skip empty rows
                        if (code.isEmpty()) continue;

                        // 3. Skip Date/Month rows (e.g., "JAN / 2026")
                        if (code.contains("/")) continue;

                        // 4. Skip pure number rows (e.g., "42")
                        if (code.matches("^\\d+$")) continue;

                        try {
                            String name = cols.get(1).text().trim();

                            // 5. Skip Header rows if they appear in body
                            if (name.equalsIgnoreCase("Description")) continue;

                            // Parse Data
                            int max = Integer.parseInt(cols.get(2).text().trim());
                            int att = Integer.parseInt(cols.get(3).text().trim());

                            list.add(new AppData.Subject(code, name, att, max));

                        } catch (NumberFormatException e) {
                            // If parsing fails (e.g., header text in a number column), skip the row
                            continue;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}