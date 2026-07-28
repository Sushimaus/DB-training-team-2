package com.dbtraining.reconx.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v0/trades")
public class DeprecatedTradeController {

    @Deprecated(since = "v1.4.0", forRemoval = true)
    @GetMapping
    public ResponseEntity<Void> oldTrades(HttpServletResponse response) {
        response.setHeader("Deprecation", "true");
        response.setHeader("Sunset", "Sat, 1 Jul 2026 00:00:00 GMT");
        response.setHeader("Link", "</api/v1/trades>; rel=\"successor-version\"");

        return ResponseEntity.status(HttpStatus.GONE).build();
    }
}