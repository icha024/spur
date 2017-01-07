package com.clianz.spur.helpers;

public class WebSocketHandler {
    private String path;
    private WebSocketOnConnect webSocketOnConnect;
    private WebSocketOnMessage webSocketOnMessage;

    public WebSocketHandler(String path, WebSocketOnConnect webSocketOnConnect, WebSocketOnMessage webSocketOnMessage) {
        this.path = path;
        this.webSocketOnConnect = webSocketOnConnect;
        this.webSocketOnMessage = webSocketOnMessage;
    }

    public String getPath() {
        return path;
    }

    public WebSocketOnConnect getWebSocketOnConnect() {
        return webSocketOnConnect;
    }

    public WebSocketOnMessage getWebSocketOnMessage() {
        return webSocketOnMessage;
    }
}
