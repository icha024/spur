package com.example;

import com.clianz.spur.SpurOptions;
import com.clianz.spur.SpurServer;

public class BasicExample {

    public static void main(final String[] args) throws Exception {

        SpurServer server = new SpurServer();
        server.get("/", (req, res) -> res.send("Hello world! Serving from default port 8080"));
        server.start();

        SpurServer server2 = new SpurServer();
        server2.get("/", (req, res) -> res.send("Hello world from another server!"));
        server2.start(new SpurOptions().port(7070));
    }
}
