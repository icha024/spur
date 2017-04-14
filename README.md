# Spur Framework

A Sinatra inspired library for building REST microservices

```java
package com.example;
import com.clianz.spur.SpurServer;

public class BasicExample {
    public static void main(final String[] args) {
        new SpurServer()
            .get("/", (req, res) -> res.send("Hello World"))
            .start();
   }
}
```

## Batteries included 
### JSON parsing and validator provided

Declare the model with Bean Validator tags:
```java
public class Car {
    @Min(1)
    private int doors;
    private String name;
    ...
}
```

Server will automatically parse to/from JSON with request validated:
```java
new SpurServer()
        .post("/test-drive", Car.class, (req, res) -> {
            Car car = req.body();
            log.info("Validated with Bean Validator 1.1: " + car.getName());
            log.info("Sending request body back as response.");
            res.send(car);
        })
        .start();
```