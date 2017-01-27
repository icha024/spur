package com.example;

import java.util.Date;

import com.clianz.spur.SpurOptions;
import com.clianz.spur.SpurServer;
import com.example.models.Pet;

import org.jboss.logging.Logger;

import io.undertow.util.StatusCodes;

public class AdvancedExample {

    //    private static final Logger LOGGER = Logger.getLogger(AdvancedExample.class.getName());
    private static final Logger LOGGER = Logger.getLogger(AdvancedExample.class.getName());

    public static void main(final String[] args) throws Exception {

        SpurServer server = new SpurServer();

        server.get("/hello", (req, res) -> res.send("Hello world!"));

        server.get("/someErrorPath", (req, res) -> res.status(StatusCodes.INTERNAL_SERVER_ERROR)
                .header("TRACE-ID", "12345")
                .send());

        server.get("/", (req, res) -> {
            // LOGGER.info("A cat call Tom was born");
            Pet johnny = new Pet("Tom");
            johnny.setBirthDate(new Date());
            johnny.setType("Cat");
            res.send(johnny);
        });

        server.put("/bb", String.class, (req, res) -> res.send("Request body was String type: " + req.body()
                .toUpperCase()));

        server.patch("/bb", null, (req, res) -> res.send(
                "Request body was not parsed: " + req.body() + ". Access/parse it manually with rawHttpServerExchange()"));

        server.post("/a", Pet.class, (req, res) -> {
            Pet pet = req.body();
            LOGGER.info("Req Pet type parsed from JSON, and validated with Bean Validator 1.1: " + pet.getName());
            LOGGER.info("Sending out an object will have it converted to JSON.");
            res.send(pet);
        });

        server.delete("/a", (req, res) -> res.send("something gone"));

        server.schedule(60, () -> LOGGER.info("This is a runnable task that triggers every 60 seconds"));

        server.websocket("/myapp", res -> {
            LOGGER.info("[OnConnectEvent] A user has connected");
            res.send("Welcome!");
        }, (msg, res) -> {
            LOGGER.info("[OnMessageEvent] User message received: " + msg);
            res.send("I heard you say: " + msg);
        });

        server.broadcastWebsockets("/myapp", "Everyone connected to the websocket path /myapp will see this");

        server.broadcastWebsockets("/myapp",
                "This message will broadcast to websocket users on the path /myapp only if the predicate operator on the key's value is true",
                "attrKey", attrVal -> attrVal != null);

        server.sse("/sse");
        server.broadcastSse("/sse", "A Server-Sent-Event (SSE) to everyone listening for events on the endpoint.");

        server.schedule(5,
                () -> server.broadcastSse("/sse", serverSentEventConnection -> serverSentEventConnection.send("Constant spam, by SSE")));

        server.preFilterRequests(req -> !req.header("deny")
                .isPresent(), res -> res.status(StatusCodes.FORBIDDEN)
                .send());
        server.preFilterRequests(req -> !req.header("block")
                .isPresent(), res -> res.status(StatusCodes.FORBIDDEN)
                .send());

        server.start(new SpurOptions().enableGzip(true)
                .enableCorsHeaders("*")
//                .enableBlockableHandlers(false)
//                .enableHttps(true)
//                .sslContext(null, null, "password")
//                .enableBasicAuth("admin", "pass")
//                .forceHttps(true)
        );
    }
}


