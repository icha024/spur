package com.example;

import com.clianz.spur.SpurServer;

public class BasicExample {

    public static void main(final String[] args) {

        new SpurServer()
                .get("/", (req, res) -> res.send("Hello World"))
                .start();
    }
}
