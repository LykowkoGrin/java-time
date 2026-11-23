package mts.java.time;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ReportFormatter {

    public static String formatWindow(MaintenanceWindow window, ZoneId zone, Locale locale){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm XXX (VV)", locale);

        String start = window.start().atZone(zone).format(formatter);
        String end = window.end().atZone(zone).format(formatter);

        return start + " – " + end;
    }
}
