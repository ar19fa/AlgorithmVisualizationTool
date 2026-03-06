package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
* Application main method.
* Starts the Spring Boot runtime and embedded server.
*/
@Controller
public class PageController {

    /**
     * Renders the main index page.
     *
     * @return template name "index" (resolved to src/main/resources/templates/index.html)
     */
    @GetMapping("/")
    public String index() {
        return "index"; // renders src/main/resources/templates/index.html
    }
}
