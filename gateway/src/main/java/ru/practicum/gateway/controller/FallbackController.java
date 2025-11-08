package ru.practicum.gateway.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping(value = "/{service}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String fallbackGet(@PathVariable String service) {
        return "Service '" + service + "' temporarily unavailable. Please try later.";
    }

    @PostMapping(value = "/{service}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String fallbackPost(@PathVariable String service) {
        return fallbackGet(service);
    }
}
