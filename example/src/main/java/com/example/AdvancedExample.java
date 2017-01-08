package com.example;

import static com.clianz.spur.SpurServer.broadcastSse;
import static com.clianz.spur.SpurServer.broadcastWebsockets;
import static com.clianz.spur.SpurServer.delete;
import static com.clianz.spur.SpurServer.get;
import static com.clianz.spur.SpurServer.patch;
import static com.clianz.spur.SpurServer.post;
import static com.clianz.spur.SpurServer.preFilterRequests;
import static com.clianz.spur.SpurServer.put;
import static com.clianz.spur.SpurServer.schedule;
import static com.clianz.spur.SpurServer.spurOptions;
import static com.clianz.spur.SpurServer.sse;
import static com.clianz.spur.SpurServer.start;
import static com.clianz.spur.SpurServer.websocket;

import java.util.Date;
import java.util.logging.Logger;

import com.example.models.Pet;

import io.undertow.util.StatusCodes;

public class AdvancedExample {

    private static final Logger LOGGER = Logger.getLogger(AdvancedExample.class.getName());

    public static void main(final String[] args) throws Exception {

        get("/hello", (req, res) -> res.send("Hello world!"));

        get("/someErrorPath", (req, res) -> res.status(500)
                .header("TRACE-ID", "12345")
                .send());

        get("/", (req, res) -> {
            // LOGGER.info("A cat call Tom was born");
            Pet johnny = new Pet("Tom");
            johnny.setBirthDate(new Date());
            johnny.setType("Cat");
            res.send(johnny);
        });

        put("/bb", String.class, (req, res) -> res.send("Request body was String type: " + req.body()
                .toUpperCase()));

        patch("/bb", null, (req, res) -> res.send(
                "Request body was not parsed: " + req.body() + ". Access/parse it manually with rawHttpServerExchange()"));

        post("/a", Pet.class, (req, res) -> {
            Pet pet = req.body();
            LOGGER.info("Req Pet type parsed from JSON, and validated with Bean Validator 1.1: " + pet.getName());
            LOGGER.info("Sending out an object will have it converted to JSON.");
            res.send(pet);
        });

        delete("/a", (req, res) -> res.send("something gone"));

        schedule(60, () -> LOGGER.info("This is a runnable task that starts every 60 seconds"));

        websocket("/myapp", sender -> {
            LOGGER.info("[OnConnectEvent] A user has connected");
            sender.send("Welcome!");
        }, (msg, sender) -> {
            LOGGER.info("[OnMessageEvent] User message received: " + msg);
            sender.send("I heard you say: " + msg);
        });

        broadcastWebsockets("/myapp", "Everyone connected to the websocket path /myapp will see this");

        broadcastWebsockets("/myapp",
                "This message will broadcast to websocket users on the path /myapp only if the predicate operator on the key's value is true",
                "attrKey", attrVal -> attrVal != null);

        sse("/sse");
        broadcastSse("/sse", "A Server-Sent-Event (SSE) to everyone listening for events on the endpoint.");

        schedule(5, () -> broadcastSse("/sse", serverSentEventConnection -> serverSentEventConnection.send("Constant spam, by SSE")));

        preFilterRequests(httpServerExchange -> httpServerExchange.getRequestHeaders()
                .get("deny") == null, httpServerExchange -> httpServerExchange.setStatusCode(StatusCodes.REQUEST_ENTITY_TOO_LARGE)
                .endExchange());

//        preFilterRequests(httpServerExchange -> httpServerExchange.getRequestHeaders()
//                .get("block") == null, httpServerExchange -> httpServerExchange.setStatusCode(StatusCodes.REQUEST_ENTITY_TOO_LARGE)
//                .endExchange());

        start(spurOptions.enableGzip(true)
                .enableCorsHeaders("*")
                .enableBlockableHandlers(false)
                .enableHttps(true)
                .sslContext(null, null, "password")
                .enableBasicAuth("admin", "pass")
                .forceHttps(true));
    }
}


