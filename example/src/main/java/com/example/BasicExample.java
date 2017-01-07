package com.example;

import static com.clianz.spur.SpurServer.*;

public class BasicExample {

    public static void main(final String[] args) throws Exception {

        get("/", (req, res) -> res.send("Hello world!"));

        start();
    }
}
