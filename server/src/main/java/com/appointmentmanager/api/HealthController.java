package com.appointmentmanager.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping(value = "/confirmed", produces = "text/html")
    public String confirmed() {
        return """
                <!doctype html>
                <html lang="en">
                  <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Email confirmed</title>
                  </head>
                  <body>
                    <h1>Email confirmed</h1>
                    <p>You can close this tab, return to Appointment Manager, and sign in.</p>
                  </body>
                </html>
                """;
    }
}
