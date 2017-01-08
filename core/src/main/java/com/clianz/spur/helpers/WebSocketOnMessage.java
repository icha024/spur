package com.clianz.spur.helpers;

@FunctionalInterface
public interface WebSocketOnMessage {
    void onMessage(String msg, WebSocketMessageSender res);
}
