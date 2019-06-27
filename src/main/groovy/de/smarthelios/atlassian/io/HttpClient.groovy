package de.smarthelios.atlassian.io

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.apache.http.HttpHeaders
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.Credentials
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.AuthCache
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.BasicAuthCache
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils

/**
 * Base class for Atlassian service clients used to access the REST APIs by HTTP.
 */
@Slf4j
class HttpClient {
    protected final HttpHost httpHost
    protected HttpClientContext httpClientContext
    protected CloseableHttpClient httpClient

    private JsonSlurper jsonSlurper

    HttpClient(String hostname, int port = 443, String scheme = 'https') {
        this.httpHost = new HttpHost(hostname, port, scheme)
        this.httpClientContext = initHttpClientContext()
        this.httpClient = initHttpClient()
        this.jsonSlurper = new JsonSlurper()
    }

    HttpClient(String hostname, String username, String password, int port = 443, String scheme = 'https') {
        this.httpHost = new HttpHost(hostname, port, scheme)
        this.httpClientContext = initAuthenticatedHttpClientContext(httpHost, username, password)
        this.httpClient = initHttpClient()
        this.jsonSlurper = new JsonSlurper()
    }

    private static HttpClientContext initHttpClientContext() {
        HttpClientContext.create()
    }

    private static HttpClientContext initAuthenticatedHttpClientContext(HttpHost httpHost, String username, String password) {
        HttpClientContext context = HttpClientContext.create()

        AuthScope authScope = new AuthScope(httpHost)

        Credentials credentials = new UsernamePasswordCredentials(username,password)

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider()
        credentialsProvider.setCredentials(authScope,credentials)

        AuthCache authCache = new BasicAuthCache()
        authCache.put(httpHost, new BasicScheme())

        context.setCredentialsProvider(credentialsProvider)
        context.setAuthCache(authCache)

        context
    }

    private static CloseableHttpClient initHttpClient() {
        HttpClientBuilder.create().build()
    }

    String getScheme() {
        httpHost.schemeName
    }

    int getPort() {
        httpHost.getPort()
    }

    String getHostname() {
        httpHost.hostName
    }

    String getBaseUrl() {
        httpHost.toURI()
    }

    URIBuilder uriBuilder(String path = '') {
        URIBuilder builder = new URIBuilder()
                .setScheme(scheme)
                .setHost(hostname)
                .setPort(port)

        path ? builder.setPath(path) : builder
    }

    void close() {
        httpClient.close()
    }

    CloseableHttpResponse execute(HttpUriRequest request) {
        httpClient.execute(request, httpClientContext)
    }

    def doGetParsedJson(String path, Map<String, String> params) {
        String jsonText = doGet(path, params)
        log.debug 'Got JSON:\n{}', JsonOutput.prettyPrint(jsonText)

        def json = jsonSlurper.parseText(jsonText)
        json
    }

    String doGet(String path, Map<String,String> params = [:]) {
        URIBuilder uriBuilder = uriBuilder(path)
        params.each { k,v -> uriBuilder.setParameter(k, v) }

        HttpGet httpGet = new HttpGet(uriBuilder.build())

        doGet(httpGet)
    }

    String doGet(HttpGet httpGet) {
        log.info('Executing request {}', httpGet.getRequestLine())
        CloseableHttpResponse response = execute(httpGet)

        String result = null
        try {
            log.info('Response status: {}', response.getStatusLine())
            result = EntityUtils.toString(response.getEntity())
        }
        catch (IOException e) {
            log.error('IO error:', e)
            result = null
        }
        finally {
            response.close()
        }

        result
    }

    MimeTypeBytes doGetBytes(String path, Map<String,String> params = [:]) {
        URIBuilder uriBuilder = uriBuilder(path)
        params.each { k,v -> uriBuilder.setParameter(k, v) }

        HttpGet httpGet = new HttpGet(uriBuilder.build())

        doGetBytes(httpGet)
    }

    MimeTypeBytes doGetBytes(HttpGet httpGet) {
        log.info('Executing request {}', httpGet.getRequestLine())
        CloseableHttpResponse response = execute(httpGet)

        MimeTypeBytes result = new MimeTypeBytes()

        try {
            log.info('Response status: {}', response.getStatusLine())
            result.mimeType = response.getFirstHeader(HttpHeaders.CONTENT_TYPE)?.value
            result.bytes = EntityUtils.toByteArray(response.getEntity())
        }
        catch (IOException e) {
            log.error('IO error:', e)
        }
        finally {
            response.close()
        }

        result
    }

    MimeTypeBytes doGetBytesForUrl(URL url) {
        MimeTypeBytes result

        try {
            URI uri = url.toURI()
            HttpGet httpGet = new HttpGet(uri)
            result = doGetBytes(httpGet)
        }
        catch (URISyntaxException e) {
            log.error "Error in download source ${url}", e
            result = null
        }

        result
    }

    MimeTypeBytes doGetBytesForUrl(String urlStr) {
        MimeTypeBytes result

        try {
            URL url = new URL(urlStr)
            result = doGetBytesForUrl(url)
        }
        catch (MalformedURLException e) {
            log.error "Error in download source ${urlStr}", e
            result = null
        }

        result
    }

    String doPost(HttpPost httpPost) {
        log.info('Executing request {}', httpPost.getRequestLine())
        CloseableHttpResponse response = execute(httpPost)

        String result = null
        try {
            log.info('Response status: {}', response.getStatusLine())
            result = EntityUtils.toString(response.getEntity())
        }
        catch (IOException e) {
            log.error('IO error:', e)
            result = null
        }
        finally {
            response.close()
        }

        result
    }

    static boolean isURI(String str) {
        try {
            URL url = new URL(str)
            url.toURI()
            true
        }
        catch(MalformedURLException|URISyntaxException ignored) {
            false
        }
    }

}
