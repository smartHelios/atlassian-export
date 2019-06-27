package de.smarthelios.atlassian.export.model

import groovy.util.logging.Slf4j
import de.smarthelios.atlassian.io.MimeTypeBytes
import de.smarthelios.atlassian.io.MimeTypeFileExt

import java.util.regex.Pattern

/**
 * An image models an attached image to Confluence and JIRA content.
 */
@Slf4j
class Image {
    //lowercase word characters, hyphen and underscore allowed
    static final Pattern EXPORTABLE_URL_FILENAME_WITHOUT_EXT = ~/^([a-zA-Z0-9]|-|_)+$/

    // https://stackoverflow.com/questions/163360/regular-expression-to-match-urls-in-java
    static final Pattern URL_REGEXP = ~/\\b(https?|ftp|file):\/\/[-a-zA-Z0-9+&@#\/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#\/%=~_|]/

    String mimeType
    String downloadUrl
    byte[] bytes

    // can be used to make the exportFilename more human friendly
    String namingHint

    // if replacement is attached export will probably use this
    ImageExportReplacement replacement

    String getExportFilename() {
        String hash
        if(bytes) {
            hash = bytes.md5()
        }
        else {
            log.error 'Cannot generate image hash name without content bytes for image with downlodUrl {}', downloadUrl
            hash = 'no-bytes'
        }

        String urlFilename
        boolean urlHadQuery
        try {
            String sanitizedUrl = URL_REGEXP.matcher(downloadUrl).matches() ? downloadUrl : 'http://x.y/' + downloadUrl
            URL url = new URL(sanitizedUrl)
            String path = url.path
            urlFilename = path.split('/').last()
            urlHadQuery = url.query
        }
        catch (MalformedURLException ignored) {
            urlFilename = ''
            urlHadQuery = false
        }

        MimeTypeFileExt ext = MimeTypeFileExt.instance
        String urlFileBaseName
        String fileExt
        if(urlFilename) {
            fileExt = ext.matchingExt(urlFilename)
            if(fileExt) {
                urlFileBaseName = urlFilename.substring(0, urlFilename.length() - '.'.length() - fileExt.length())
            }
            else {
                fileExt = ext.extFor(mimeType)
                urlFileBaseName = urlFilename
            }
        }
        else {
            fileExt = ext.extFor(mimeType)
            urlFileBaseName = urlFilename
        }

        String baseName
        if(EXPORTABLE_URL_FILENAME_WITHOUT_EXT.matcher(urlFileBaseName).matches()) {
            baseName = urlFileBaseName
        }
        else {
            baseName = ''
        }

        List<String> exportBasename = []
        if(namingHint) {
            exportBasename << namingHint
        }
        if(baseName) {
            exportBasename << baseName
            // even if there is an exportable basename, if image is generated dynamically we have to avoid naming collisions
            if(urlHadQuery) {
                exportBasename << hash
            }
        }
        else {
            exportBasename << hash
        }

        exportBasename.join('_') + (fileExt ? ".$fileExt" : '')
    }

    void setMimeTypeBytes(MimeTypeBytes mimeTypeBytes) {
        this.bytes = mimeTypeBytes.bytes
        this.mimeType = mimeTypeBytes.mimeType
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        Image image = (Image) o

        if (!Arrays.equals(bytes, image.bytes)) return false
        if (downloadUrl != image.downloadUrl) return false

        return true
    }

    int hashCode() {
        int result
        result = (downloadUrl != null ? downloadUrl.hashCode() : 0)
        result = 31 * result + (bytes != null ? Arrays.hashCode(bytes) : 0)
        return result
    }
}
