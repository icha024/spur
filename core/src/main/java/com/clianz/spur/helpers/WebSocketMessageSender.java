package com.clianz.spur.helpers;

import java.nio.ByteBuffer;

import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

public class WebSocketMessageSender {
    WebSocketChannel channel;

    public WebSocketMessageSender(WebSocketChannel channel) {
        this.channel = channel;
    }

    public void send(String msg) {
        if (msg != null) {
            WebSockets.sendText(msg, channel, null);
        }
    }

    public void send(ByteBuffer byteBuffer) {
        if (byteBuffer != null) {
            WebSockets.sendBinary(byteBuffer, channel, null);
        }
    }

    public boolean setChannelAttribute(String key, Object value) {
        return channel.setAttribute(key, value);
    }

    public Object getChannelAttribute(String key) {
        return channel.getAttribute(key);
    }
}
