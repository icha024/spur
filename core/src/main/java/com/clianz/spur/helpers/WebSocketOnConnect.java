package com.clianz.spur.helpers;

@FunctionalInterface
public interface WebSocketOnConnect {
    void onConnect(WebSocketMessageSender res);
}
