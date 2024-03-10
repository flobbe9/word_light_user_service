package de.word_light.user_service.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.word_light.user_service.entities.AppUser;
import de.word_light.user_service.exception.ApiExceptionFormat;
import de.word_light.user_service.exception.ApiExceptionHandler;
import de.word_light.user_service.services.AppUserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;


/**
 * Rest controller containing all endpoints regarding the {@link AppUser} entity.<p>
 * 
 * Listens onyl for "/api/appUser/" mappings
 * 
 * @since 0.0.1
 */
@RestController
@RequestMapping("/api/appUser")
@Tag(name = "AppUser logic")
@Validated
public class AppUserController {

    @Autowired
    private AppUserService appUserService;
    

    @PostMapping("/register")
    @Operation(summary = "Create new appUser and send verification mail", description = "Only email, password and role are required")
    public ResponseEntity<ApiExceptionFormat> register(@RequestBody @NotNull(message = "'appUser' cannot be null") @Valid AppUser appUser) {

        this.appUserService.validatePassword(appUser.getPassword());

        // save as disabled user
        this.appUserService.register(new AppUser(appUser.getEmail(), appUser.getPassword(), appUser.getRole()));

        return ResponseEntity.status(201).body(ApiExceptionHandler.returnPrettySuccess(HttpStatus.CREATED));
    }


    @PutMapping("/update")
    @Operation(summary = "Update existing appUser.")
    public ResponseEntity<ApiExceptionFormat> update(@RequestBody @NotNull(message = "'appUser' cannot be null") @Valid AppUser appUser) {

        this.appUserService.update(appUser);

        return ResponseEntity.ok().body(ApiExceptionHandler.returnPrettySuccess(HttpStatus.OK));
    }


    @GetMapping("/confirmAccount")
    @Operation(summary = "Confirm existing account of appUser.")
    public ResponseEntity<ApiExceptionFormat> confirmAccount(@RequestParam @NotBlank(message = "'email' cannot be blank or null") @Parameter(example = "max.mustermann@domain.com") String email, 
                                             @RequestParam @NotBlank(message = "'token cannot be blank or null") String token) {

        this.appUserService.confirmAccount(email, token);

        return ResponseEntity.ok().body(ApiExceptionHandler.returnPrettySuccess(HttpStatus.OK));
    }


    @GetMapping("/getByEmail")
    @Operation(summary = "Find appUser by email.")
    public AppUser getByEmail(@RequestParam @NotBlank(message = "'email' cannot be blank") @Parameter(example = "max.mustermann@domain.com") String email) {

        return this.appUserService.loadUserByUsername(email);
    }
}