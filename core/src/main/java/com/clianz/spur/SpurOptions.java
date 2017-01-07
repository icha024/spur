package com.clianz.spur;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.sun.org.apache.xpath.internal.operations.Bool;

public class SpurOptions {

    private static final Logger LOGGER = Logger.getLogger(SpurOptions.class.getName());
    private static SpurOptions option = new SpurOptions();

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

    protected SpurOptions() {}

    public static SpurOptions forceHttps(boolean forceHttps) {
        option.forceHttps = forceHttps;
        return option;
    }

    public static SpurOptions enableCorsHeaders(String corsHeaders) {
        option.corsHeaders = parseCorsString(corsHeaders);
        return option;
    }

    private static List<String> parseCorsString(String corsString) {
        return Arrays.stream(corsString.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    public static SpurOptions enableBlockableHandlers(boolean blockableEnabled) {
        option.blockableHandlersEnabled = blockableEnabled;
        return option;
    }

    public static SpurOptions enableGzip(boolean gzipEnabled) {
        option.gzipEnabled = gzipEnabled;
        return option;
    }

    public static SpurOptions host(String host) {
        option.host = host;
        return option;
    }

    public static SpurOptions port(Integer port) {
        option.port = port;
        return option;
    }

    public static SpurOptions httpsPort(Integer httpsPort) {
        option.httpsPort = httpsPort;
        return option;
    }

    public static SpurOptions enableHttp2(Boolean http2Enabled) {
        option.http2Enabled = http2Enabled;
        return option;
    }

    public static SpurOptions requestParseTimeOut(int requestParseTimeOut) {
        option.requestParseTimeOut = requestParseTimeOut;
        return option;
    }

    public static SpurOptions maxEntitySize(long maxEntitySize) {
        option.maxEntitySize = maxEntitySize;
        return option;
    }

    public static SpurOptions gzipMaxSize(long gzipMaxSize) {
        option.gzipMaxSize = gzipMaxSize;
        return option;
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

    public static SpurOptions enableHttps(boolean httpsEnabled) {
        option.httpsEnabled = httpsEnabled;
        return option;
    }

    public static SpurOptions sslContext(String keystorePath, String truststorePath, String password) throws Exception {
        option.keystorePath = keystorePath;
        option.keystorePassword = password;
        option.truststorePath = truststorePath;
        return option;
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
}
