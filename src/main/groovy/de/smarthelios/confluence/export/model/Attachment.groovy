package de.smarthelios.confluence.export.model

import de.smarthelios.atlassian.io.MimeTypeFileExt
import org.apache.commons.lang3.StringUtils

import java.util.regex.Pattern

/**
 * An attachment to a Confluence page.
 */
class Attachment {
    static final Pattern EXPORTABLE_TITLE_REGEXP = ~/^(\w| |-|\.)+$/ //word characters, dots and spaces allowed

    String id
    String mediaType
    long fileSize
    String comment
    String downloadUrl
    String title

    //TODO: this could be an issue for many or big attachments
    byte[] bytes

    String getExportFileName() {
        String ext = MimeTypeFileExt.instance.matchingExt(title)

        String withoutExt
        if(ext) {
            withoutExt = title.substring(0, title.length() - '.'.length() - ext.length())
        }
        else {
            withoutExt = title
        }

        String sanitized
        if(exportableTitle) {
            sanitized = StringUtils.replaceChars(withoutExt, ' .', '__') + "_${id}"
        }
        else {
            sanitized = id
        }

        String exportFileName
        if(ext) {
            exportFileName = "$sanitized.$ext"
        }
        else {
            String extForMimeType = MimeTypeFileExt.instance.extFor(mediaType)
            if(extForMimeType) {
                exportFileName = "$sanitized.$extForMimeType"
            }
            else {
                exportFileName = sanitized
            }
        }

        exportFileName
    }

    boolean isExportableTitle() {
        title ? EXPORTABLE_TITLE_REGEXP.matcher(title).matches() : false
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        Attachment that = (Attachment) o

        if (fileSize != that.fileSize) return false
        if (!Arrays.equals(bytes, that.bytes)) return false
        if (comment != that.comment) return false
        if (downloadUrl != that.downloadUrl) return false
        if (id != that.id) return false
        if (mediaType != that.mediaType) return false
        if (title != that.title) return false

        return true
    }

    int hashCode() {
        int result
        result = (id != null ? id.hashCode() : 0)
        result = 31 * result + (mediaType != null ? mediaType.hashCode() : 0)
        result = 31 * result + (int) (fileSize ^ (fileSize >>> 32))
        result = 31 * result + (comment != null ? comment.hashCode() : 0)
        result = 31 * result + (downloadUrl != null ? downloadUrl.hashCode() : 0)
        result = 31 * result + (title != null ? title.hashCode() : 0)
        result = 31 * result + (bytes != null ? Arrays.hashCode(bytes) : 0)
        return result
    }
}
