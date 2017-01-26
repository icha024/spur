package com.clianz.spur;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.jboss.logging.Logger;

public class SpurOptions {

    private static final Logger LOGGER = Logger.getLogger(SpurOptions.class.getName());

    protected boolean gzipEnabled = getEnvProperty("ENABLE_GZIP", false);
    protected boolean blockableHandlersEnabled = false;
    protected String host = getEnvProperty("HOST", "localhost");
    protected Integer port = getEnvProperty("PORT", 8080);
    protected Integer httpsPort = getEnvProperty("HTTPS_PORT", 8443);
    protected Boolean http2Enabled = getEnvProperty("ENABLE_HTTP2", true);
    protected int requestParseTimeOut = 2000;
    protected long maxEntitySize = 1024L * 1024L;
    protected long gzipMaxSize = 860L;
    protected List<String> corsHeaders = parseCorsString(getEnvProperty("CORS", ""));
    protected boolean httpsEnabled = getEnvProperty("ENABLE_HTTPS", false);
    protected String keystorePath = getEnvProperty("KEYSTORE", "");
    protected String truststorePath = getEnvProperty("TRUSTSTORE", "");
    protected String keystorePassword = getEnvProperty("KEYSTORE_PASSWORD", "password");
    protected boolean forceHttps = getEnvProperty("FORCE_HTTPS", false);
    protected String basicAuthUser = getEnvProperty("BASIC_AUTH_USER", "");
    protected String basicAuthPassword = getEnvProperty("BASIC_AUTH_PASS", "");

    public SpurOptions() {
    }

    public SpurOptions forceHttps(boolean forceHttps) {
        this.forceHttps = forceHttps;
        return this;
    }

    public SpurOptions enableCorsHeaders(String corsHeaders) {
        this.corsHeaders = parseCorsString(corsHeaders);
        return this;
    }

    private static List<String> parseCorsString(String corsString) {
        return Arrays.stream(corsString.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    public SpurOptions enableBlockableHandlers(boolean blockableEnabled) {
        this.blockableHandlersEnabled = blockableEnabled;
        return this;
    }

    public SpurOptions enableGzip(boolean gzipEnabled) {
        this.gzipEnabled = gzipEnabled;
        return this;
    }

    public SpurOptions enableBasicAuth(String user, String pass) {
        this.basicAuthUser = user;
        this.basicAuthPassword = pass;
        return this;
    }

    public SpurOptions host(String host) {
        this.host = host;
        return this;
    }

    public SpurOptions port(Integer port) {
        this.port = port;
        return this;
    }

    public SpurOptions httpsPort(Integer httpsPort) {
        this.httpsPort = httpsPort;
        return this;
    }

    public SpurOptions enableHttp2(Boolean http2Enabled) {
        this.http2Enabled = http2Enabled;
        return this;
    }

    public SpurOptions requestParseTimeOut(int requestParseTimeOut) {
        this.requestParseTimeOut = requestParseTimeOut;
        return this;
    }

    public SpurOptions maxEntitySize(long maxEntitySize) {
        this.maxEntitySize = maxEntitySize;
        return this;
    }

    public SpurOptions gzipMaxSize(long gzipMaxSize) {
        this.gzipMaxSize = gzipMaxSize;
        return this;
    }

    public SpurOptions enableHttps(boolean httpsEnabled) {
        this.httpsEnabled = httpsEnabled;
        return this;
    }

    public SpurOptions sslContext(String keystorePath, String truststorePath, String password) throws Exception {
        this.keystorePath = keystorePath;
        this.keystorePassword = password;
        this.truststorePath = truststorePath;
        return this;
    }

    protected SSLContext getSslContext() throws Exception {
        return createSSLContext(loadStore("server.keystore", keystorePath, keystorePassword),
                loadStore("server.truststore", truststorePath, keystorePassword), keystorePassword);
    }

    // https://github.com/undertow-io/undertow/blob/master/examples/src/main/java/io/undertow/examples/http2/Http2Server.java
    private static SSLContext createSSLContext(final KeyStore keyStore, final KeyStore trustStore, String password) throws Exception {
        KeyManager[] keyManagers;
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password.toCharArray());
        keyManagers = keyManagerFactory.getKeyManagers();

        TrustManager[] trustManagers;
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        trustManagers = trustManagerFactory.getTrustManagers();

        SSLContext sslContext;
        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, null);

        return sslContext;
    }

    private static KeyStore loadStore(String defaultResourceName, String storeLoc, String password) throws Exception {
        InputStream stream = null;
        try {
            if (storeLoc == null || storeLoc.isEmpty()) {
                LOGGER.info("Get keystore from resource: " + defaultResourceName);
                stream = SpurServer.class.getClassLoader()
                        .getResourceAsStream(defaultResourceName);
            } else {
                stream = Files.newInputStream(Paths.get(storeLoc));
            }

            if (stream == null) {
                throw new RuntimeException("Could not load keystore");
            }
            try (InputStream is = stream) {
                KeyStore loadedKeystore = KeyStore.getInstance("JKS");
                loadedKeystore.load(is, password.toCharArray());
                return loadedKeystore;
            }
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    private static Integer getEnvProperty(String propName, Integer defaultVal) {
        String propVal = System.getenv(propName);
        return (propVal == null) ? Integer.getInteger(propName, defaultVal) : Integer.parseInt(propVal);
    }

    private static String getEnvProperty(String propName, String defaultVal) {
        String propVal = System.getenv(propName);
        return (propVal == null) ? System.getProperty(propName, defaultVal) : propVal;
    }

    private static Boolean getEnvProperty(String propName, boolean defaultVal) {
        String propVal = System.getenv(propName);
        String finalVal = (propVal == null) ? System.getProperty(propName, String.valueOf(defaultVal)) : propVal;
        return Boolean.valueOf(finalVal);
    }
}
