package de.smarthelios.atlassian.io

/**
 * HttpClient response result containing received bytes and Content-Type header
 */
class MimeTypeBytes {

    static final MimeTypeBytes EMPTY = new MimeTypeBytes()

    String mimeType
    byte[] bytes

    boolean isEmpty() {
        null == bytes
    }
}
