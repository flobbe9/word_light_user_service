package com.example.vorspiel_userservice.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


/**
 * Class containing endoints with templates handled by thymeleaf. <p>
 * 
 * HTML templates must be placed in the src/resources/templates/ folder.
 * 
 * @since 0.0.1
 */
@Controller
public class ThymeleafController {
    
    @GetMapping("/")
    public String getIndex() {

        return "index";
    }
}