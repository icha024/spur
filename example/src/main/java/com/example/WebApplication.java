package com.example;

import static com.clianz.spur.SpurServer.delete;
import static com.clianz.spur.SpurServer.get;
import static com.clianz.spur.SpurServer.post;
import static com.clianz.spur.SpurServer.spurOptions;
import static com.clianz.spur.SpurServer.start;
import static com.clianz.spur.SpurServer.websocket;

import java.util.logging.Logger;

import com.example.models.Pet;

public class WebApplication {

    private static final Logger LOGGER = Logger.getLogger(WebApplication.class.getName());

    public static void main(final String[] args) throws Exception {
        get("/hello", (req, res) -> res.send("Hello world!"));

        get("/", (req, res) -> {
            LOGGER.info("Call GET on root");
            res.send(new Pet("Johnny"));
        });

        post("/a", String.class, (req, res) -> res.send(req.body()));

        delete("/a", (req, res) -> res.send("something gone"));

        websocket("/myapp", sender -> {
            LOGGER.info("A user has connected");
            sender.send("Welcome!");
        }, (msg, sender) -> {
            sender.send("I got your message: " + msg);
            LOGGER.info("User message received: " + msg);
        });

        start(spurOptions.enableGzip(true)
                .enableCorsHeaders("*")
                .enableBlockableHandlers(false)
                .enableHttps(true)
                .sslContext(null, null, "password")
                .forceHttps(true));
    }
}


