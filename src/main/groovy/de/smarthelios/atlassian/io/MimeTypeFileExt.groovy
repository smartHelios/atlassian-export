package de.smarthelios.atlassian.io

import groovy.util.logging.Slf4j

/**
 * Utility class which uses the Apache HTTPD mime type list to build a mapping between MIME types and file extensions.
 * Eases the querying for file extensions for MIME types and vice versa.
 */
@Slf4j
class MimeTypeFileExt {

    @Lazy
    static instance = new MimeTypeFileExt()

    private Map<String, List<String>> mimeTypeToFileExtensions = [:]
    private List<String> allExtensions = []

    private MimeTypeFileExt() {
        String mapping = getClass().getResourceAsStream('/apache-httpd-mime-types/mime.types').text
        mapping.eachLine { line ->
            if(!line.startsWith('#')) {
                List<String> tuple = line.tokenize()
                String mimeType = tuple.pop()
                mimeTypeToFileExtensions[mimeType] = tuple
                allExtensions.addAll(tuple)
            }
        }
    }

    boolean matches(String mimeType, String filename) {
        List<String> extensions = mimeType ? mimeTypeToFileExtensions[mimeType.toLowerCase()] : []
        if(!extensions) {
            log.warn 'No extensions for requested mime type "{}" found.', mimeType ?: ''
        }

        String matchingExtension
        if(filename) {
            def filenameCheck = { filename.endsWith(".$it") || filename.endsWith(".${it.toUpperCase()}") }
            matchingExtension = filename ? extensions.find(filenameCheck) : ''
        }
        else {
            log.error 'Empty filename given.'
            matchingExtension = ''
        }
        matchingExtension ? true : false
    }

    String extFor(String mimeType) {
        List<String> extensions
        if(mimeType) {
            int semicolonPos = mimeType.indexOf(';')
            String cleanMimeType = semicolonPos > -1 ? mimeType.substring(0, semicolonPos) : mimeType
            extensions = mimeTypeToFileExtensions[cleanMimeType.toLowerCase()]
        }
        else {
            extensions = []
        }

        String fileExt
        if(extensions) {
            // out of nostalgic reasons we prefer 3 letter extensions, else take first one
            fileExt = extensions.find { it.length() == 3 } ?: extensions.first()
        }
        else {
            log.warn 'No extensions for requested mime type "{}" found.', mimeType ?: ''
            fileExt = ''
        }

        fileExt
    }

    String matchingExt(String filename) {
        String matchingExt

        if(filename) {
            String lowerCaseMatch = allExtensions.find { filename.endsWith(".$it") }
            if(lowerCaseMatch) {
                matchingExt = lowerCaseMatch
            }
            else {
                String upperCaseMatch = allExtensions.find { filename.endsWith(".${it.toUpperCase()}") }
                if(upperCaseMatch) {
                    matchingExt = upperCaseMatch.toUpperCase()
                }
                else {
                    matchingExt = ''
                }
            }
        }
        else {
            log.error 'Empty filename given.'
            matchingExt = ''
        }

        matchingExt
    }
}
