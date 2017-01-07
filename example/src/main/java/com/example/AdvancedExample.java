package com.example;

import static com.clianz.spur.SpurServer.broadcastWebsockets;
import static com.clianz.spur.SpurServer.delete;
import static com.clianz.spur.SpurServer.get;
import static com.clianz.spur.SpurServer.post;
import static com.clianz.spur.SpurServer.put;
import static com.clianz.spur.SpurServer.spurOptions;
import static com.clianz.spur.SpurServer.start;
import static com.clianz.spur.SpurServer.websocket;

import java.util.logging.Logger;

import com.example.models.Pet;

public class AdvancedExample {

    private static final Logger LOGGER = Logger.getLogger(AdvancedExample.class.getName());

    public static void main(final String[] args) throws Exception {

        get("/hello", (req, res) -> res.send("Hello world!"));

        get("/", (req, res) -> {
            LOGGER.info("Call GET on root");
            res.send(new Pet("Johnny"));
        });

        put("/bb", String.class, (req, res) -> res.send(req.body()));

        post("/a", String.class, (req, res) -> res.send(req.body()));

        delete("/a", (req, res) -> res.send("something gone"));

        websocket("/myapp", sender -> {
            LOGGER.info("A user has connected");
            sender.send("Welcome!");
        }, (msg, sender) -> {
            LOGGER.info("User message received: " + msg);
            sender.send("I got your message: " + msg);
        });

        broadcastWebsockets("/myapp",
                "This message will broadcast to all websocket users on the path, if the value store for the key returns is true", "attrKey",
                attrVal -> attrVal != null);

        start(spurOptions.enableGzip(true)
                .enableCorsHeaders("*")
                .enableBlockableHandlers(false)
                .enableHttps(true)
                .sslContext(null, null, "password")
                .forceHttps(true));
    }
}


