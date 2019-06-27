package de.smarthelios.atlassian.io

/**
 * A very basic cache class which helps to prevent unnecessary duplicate downloads.
 */
class ImageCache {
    Map<String, byte[]> cache = [:]

    boolean contains(String downloadUrl) {
        cache.containsKey(downloadUrl)
    }

    byte[] getAt(String downloadUrl) {
        cache[downloadUrl]
    }

    void putAt(String downloadUrl, byte[] bytes) {
        cache[downloadUrl] = bytes
    }
}
