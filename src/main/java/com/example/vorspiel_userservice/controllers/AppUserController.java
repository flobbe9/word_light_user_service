package com.example.vorspiel_userservice.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.vorspiel_userservice.entites.AppUser;
import com.example.vorspiel_userservice.exception.ApiExceptionFormat;
import com.example.vorspiel_userservice.exception.ApiExceptionHandler;
import com.example.vorspiel_userservice.services.AppUserService;


@RestController
@RequestMapping("/api/appUser")
@Validated
public class AppUserController {

    @Autowired
    private AppUserService appUserService;
    

    @PostMapping("/save")
    public ApiExceptionFormat save(@RequestBody @Validated AppUser appUser, BindingResult bindingResult) {

        this.appUserService.save(new AppUser(appUser.getEmail(), appUser.getPassword(), appUser.getRole()));

        return ApiExceptionHandler.returnPrettySuccess(HttpStatus.CREATED);
    }
}