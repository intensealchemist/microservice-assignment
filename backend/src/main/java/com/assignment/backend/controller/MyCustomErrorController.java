package com.assignment.backend.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyCustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError() {
        return "This is an error page message.";
    }
}
