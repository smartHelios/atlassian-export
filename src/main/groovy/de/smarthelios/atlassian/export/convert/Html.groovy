package de.smarthelios.atlassian.export.convert

import java.time.format.DateTimeFormatter

/**
 * Shared formats for HTML outputs.
 */
class Html {
    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss (Z)')

    String dateClass = 'date'
    String hBytesClass = 'hBytes'

    String format(Date date, String cssClass = '') {
        "<span class=\"${cssClass ?: dateClass}\">${DATE_FORMAT.format(date.toOffsetDateTime())}</span>"
    }

    String humanBytes(long size, String cssClass = '') {
        "<span class=\"${cssClass ?: hBytesClass}\">${Convert.humanReadableByteCount(size)}</span>"
    }
}
