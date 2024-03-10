package de.word_light.user_service.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import de.word_light.user_service.UserServiceApplication;
import io.swagger.v3.oas.annotations.Operation;


/**
 * Class containing endoints with templates handled by thymeleaf. <p>
 * 
 * HTML templates must be placed in the src/resources/templates/ folder.
 * 
 * @since 0.0.1
 */
@Controller
public class ThymeleafController {

    @Value("${WEBSITE_NAME}")
    private String WEBSITE_NAME;

    @Value("${API_NAME}")
    private String API_NAME;

    @Value("${BASE_URL}")
    private String BASE_URL;

    @Value("${DB_VERSION}")
    private String DB_VERSION;

    
    @GetMapping("/")
    @Operation(summary = "View basic information about api.")
    public String getIndex(Model model) {

        model.addAttribute("WEBSITE_NAME", this.WEBSITE_NAME);
        model.addAttribute("API_NAME", this.API_NAME);
        model.addAttribute("BASE_URL", this.BASE_URL);
        model.addAttribute("API_VERSION", UserServiceApplication.getApiVersion());
        model.addAttribute("DB_VERSION", this.DB_VERSION);

        return "index";
    }
}