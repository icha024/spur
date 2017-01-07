package com.example;

import static com.clianz.spur.SpurServer.*;

public class HelloWorld {

    public static void main(final String[] args) throws Exception {

        get("/", (req, res) -> res.send("Hello world!"));

        start();
    }
}
