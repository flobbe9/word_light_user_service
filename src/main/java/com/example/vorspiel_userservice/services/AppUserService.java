package com.example.vorspiel_userservice.services;

import static com.example.vorspiel_userservice.entites.AppUser.VALIDATION_NOT_NULL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.example.vorspiel_userservice.entites.AppUser;
import com.example.vorspiel_userservice.entites.ConfirmationToken;
import com.example.vorspiel_userservice.exception.ApiException;
import com.example.vorspiel_userservice.repositories.AppUserRepository;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


@Service
@Validated
public class AppUserService implements UserDetailsService {

    @Autowired
    private AppUserRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ConfirmationTokenService confirmationTokenService;


    /**
     * Save new appUser as {@code disabled} and send verification mail to user.
     * 
     * @param appUser to register
     * @return registered appUser
     * @throws ApiException if user is null
     */
    public AppUser register(@NotNull(message = VALIDATION_NOT_NULL) AppUser appUser) {

        saveNew(appUser);

        sendAccountVerificationMail(appUser);

        return appUser;
    }
    
    
    // TODO: does this update 'updated' field?
    /**
     * Call {@code save} method of jpa repo on existing {@code AppUser} or throw if does not exist.
     * 
     * @param appUser to update
     * @return updated appUser
     * @throws ApiException if user does not exist or is null
     */
    public AppUser update(@NotNull(message = VALIDATION_NOT_NULL) AppUser appUser) {

        if (!this.repository.existsByEmail(appUser.getEmail()))
            throw new ApiException("Failed to updated user. User with given email does not exist.");

            return this.repository.save(appUser);
    }


    public UserDetails loadUserByUsername(@NotBlank(message = AppUser.VALIDATION_EMAIL_NOT_BLANK) String email) {

        return repository.findByEmail(email)
                         .orElseThrow(() -> new ApiException("Failed to load user. Username '" + email + "' does not exist."));
    }


    public void confirmAccount(@NotBlank(message = AppUser.VALIDATION_EMAIL_NOT_BLANK) String email, 
                               @NotBlank(message = ConfirmationToken.VALIDATION_NOT_NULL) String token) {

        this.confirmationTokenService.confirmToken(token);

        enable(email);
    }

    
    // TODO:
    private void sendAccountVerificationMail(AppUser appUser) {

        // create confirmation token
        ConfirmationToken confirmationToken = this.confirmationTokenService.saveNew();

        
        // create mail
        
        // send mail
    }


    private void enable(String email) {

        AppUser appUser = (AppUser) loadUserByUsername(email);

        appUser.setEnabled(true);

        this.repository.save(appUser);
    }


    /**
     * Save new appUser to db or throw if email is already taken.
     * 
     * @param appUser to save
     * @return saved appUser
     * @throws ApiException if user does already exist or is null
     */
    private AppUser saveNew(AppUser appUser) {

        // case: email already taken
        if (this.repository.existsByEmail(appUser.getEmail()))
            throw new ApiException("Failed to save user. User with this email does already exist.");
            
        appUser.setPassword(passwordEncoder.encode(appUser.getPassword()));

        return this.repository.save(appUser);
    }
}