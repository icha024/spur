package com.clianz.spur;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SpurOptions {

    private static final String DEFAULT_HOST = "0.0.0.0";
    private static final int DEFAULT_PORT = 8080;
    private static SpurOptions option = new SpurOptions();

    protected boolean gzipEnabled = false;
    protected String host = getEnvProperty("HOST", DEFAULT_HOST);
    protected Integer port = getEnvProperty("PORT", DEFAULT_PORT);
    protected Boolean http2Enabled = true;
    protected int requestParseTimeOut = 2000;
    protected long maxEntitySize = 1024L * 1024L;
    protected long gzipMaxSize = 860L;
    protected List<String> corsHeaders = parseCorsString(getEnvProperty("CORS", ""));

    public static SpurOptions enableCorsHeaders(String corsHeaders) {
        option.corsHeaders = parseCorsString(corsHeaders);
        return option;
    }

    private static List<String> parseCorsString(String corsString) {
        return Arrays.stream(corsString.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
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
}
