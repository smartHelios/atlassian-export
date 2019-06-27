package de.smarthelios.atlassian.export.convert

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException

/**
 * Convert provides some basic conversions for date and file sizes and names.
 */
class Convert {

    static final DateTimeFormatter ISO_NO_COLON_OFFSET_DATE_TIME = new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        // replace DateTimeFormatter.appendOffsetId() by an alternative pattern without colon as alternative to
        // in DateTimeFormatterBuilder.OffsetIdPrinterParser 's INSTANCE_ID_Z
        .appendOffset('+HHMMss','Z')
        .toFormatter()

    static Date toDate(Object iso8601) {
        toDate(iso8601, DateTimeFormatter.ISO_OFFSET_DATE_TIME) ?: toDate(iso8601, ISO_NO_COLON_OFFSET_DATE_TIME)
    }

    static Date toDate(Object iso8601, DateTimeFormatter formatter) {
        try {
            OffsetDateTime.parse(iso8601.toString(), formatter).toDate()
        }
        catch (DateTimeParseException ignored) {
            null
        }
    }

    //https://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java#3758880
    static String humanReadableByteCount(long bytes, boolean si = true) {
        int unit = si ? 1000 : 1024
        if (bytes < unit) {
            "${bytes}B"
        }
        else {
            int exp = (int) (Math.log(bytes) / Math.log(unit))

            String pre
            if(si) {
                pre = 'kMGTPE'.charAt(exp-1)
            }
            else {
                pre = "${'KMGTPE'.charAt(exp-1)}i"
            }

            String.format('%.1f %sB', bytes / Math.pow(unit, exp), pre)
        }
    }

    static String sanitizeForFileBasename(String str) {
        str.replaceAll(/[^a-zA-Z0-9-]/, '_')
    }
}
