package com.example.vorspiel_userservice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.vorspiel_userservice.entites.AppUser;
import com.example.vorspiel_userservice.exception.ApiException;
import com.example.vorspiel_userservice.repositories.AppUserRepository;


@Service
public class AppUserService implements UserDetailsService {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    public UserDetails loadUserByUsername(String email) {

        return appUserRepository.findByEmail(email)
                                .orElseThrow(() -> new ApiException("Failed to load user. Username '" + email + "' does not exist."));
    }


    public AppUser save(AppUser appUser) {

        if (appUser == null)
            throw new ApiException("Failed to save user. 'appUser' cannot be null.");

        // case: email already taken
        if (this.appUserRepository.existsByEmail(appUser.getEmail()))
            throw new ApiException("Failed to save user. User with this email does already exist.");

        appUser.setPassword(passwordEncoder.encode(appUser.getPassword()));

        return this.appUserRepository.save(appUser);
    }
}