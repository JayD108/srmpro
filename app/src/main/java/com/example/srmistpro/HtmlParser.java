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
                    // Matching the Python Logic: Code(0), Name(1), Max(2), Att(3), Abs(4), %(5)
                    if (cols.size() >= 6) {
                        String code = cols.get(0).text().trim();
                        if (code.equalsIgnoreCase("Total")) continue;
                        if (code.isEmpty()) continue;

                        try {
                            String name = cols.get(1).text().trim();
                            int max = Integer.parseInt(cols.get(2).text().trim());
                            int att = Integer.parseInt(cols.get(3).text().trim());

                            // Filter out "Course" or Header rows if they sneak in
                            if (name.equalsIgnoreCase("Description")) continue;

                            list.add(new AppData.Subject(code, name, att, max));
                        } catch (NumberFormatException e) {
                            // Skip header rows or malformed data
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