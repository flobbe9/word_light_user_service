package com.example.vorspiel_userservice.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.vorspiel_userservice.entites.AppUser;
import com.example.vorspiel_userservice.exception.ApiExceptionFormat;
import com.example.vorspiel_userservice.exception.ApiExceptionHandler;
import com.example.vorspiel_userservice.services.AppUserService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


@RestController
@RequestMapping("/api/appUser")
@Validated
public class AppUserController {

    @Autowired
    private AppUserService appUserService;
    

    @PostMapping("/register")
    public ApiExceptionFormat register(@RequestBody @Validated @NotNull(message = "'appUser' cannot be null") AppUser appUser) {

        // save as disabled user
        this.appUserService.register(null);

        return ApiExceptionHandler.returnPrettySuccess(HttpStatus.CREATED);
    }


    @PutMapping("/update")
    public ApiExceptionFormat update(@RequestBody @Validated @NotNull(message = "'appUser' cannot be null") AppUser appUser) {

        this.appUserService.update(appUser);

        return ApiExceptionHandler.returnPrettySuccess(HttpStatus.OK);
    }


    @GetMapping("/confirmAccount")
    public ApiExceptionFormat confirmAccount(@RequestParam @NotBlank(message = "'email' cannot be blank or null") String email, 
                                             @RequestParam @NotBlank(message = "'token cannot be blank or null") String token) {

        this.appUserService.confirmAccount(email, token);

        return ApiExceptionHandler.returnPrettySuccess(HttpStatus.OK);
    }


    @GetMapping("/getByEmail")
    public AppUser getByEmail(@RequestParam @NotBlank(message = "'email' cannot be blank") String email) {

        return (AppUser) this.appUserService.loadUserByUsername(email);
    }
}