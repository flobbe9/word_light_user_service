package com.example.vorspiel_userservice.services;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.example.vorspiel_userservice.abstractClasses.AbstractService;
import com.example.vorspiel_userservice.entites.ConfirmationToken;
import com.example.vorspiel_userservice.exception.ApiException;
import com.example.vorspiel_userservice.repositories.ConfirmationTokenRepository;

import jakarta.validation.constraints.NotBlank;


@Service
@Validated
public class ConfirmationTokenService extends AbstractService<ConfirmationToken, ConfirmationTokenRepository> {

    private static final int CREATE_UNIQUE_UUID_TRIES = 1000;
    
    public static final String VALIDATION_NOT_NULL = "'confirmationToken' cannot be null.";
    public static final String VALIDATION_TOKEN_NOT_BLANK = "'token' cannot be blank or null.";

    @Autowired
    private ConfirmationTokenRepository repository;


    public ConfirmationToken saveNew() {

        String token = createNonExistingToken();

        ConfirmationToken confirmationToken = new ConfirmationToken(token);

        return save(confirmationToken);
    }
    

    public void confirmToken(@NotBlank(message = VALIDATION_TOKEN_NOT_BLANK) String token) {

        if (token == null)
            throw new ApiException("Failed to confirm token. 'token' cannot be null.");

        ConfirmationToken confirmationToken = getByToken(token);

        if (confirmationToken.isConfirmed())
            throw new ApiException("Failed to confirm token. 'confirmationToken' is confirmed already.");

        if (confirmationToken.isExpired())
            throw new ApiException("Failed to confirm token. 'confirmationToken' is expired.");

        // set confirmed
        confirmationToken.setConfirmedAt(LocalDateTime.now());

        // save token
        save(confirmationToken);
    }


    private ConfirmationToken getByToken(String token) {

        if (token == null)
            throw new ApiException("Failed to confirm token. 'token' cannot be null.");

        return this.repository.findByToken(token)
                              .orElseThrow(() -> new ApiException("Failed to find confirmation token with 'token': " + token + "."));
    }


    private String createNonExistingToken() {

        int count = 0;

        while (true) {
            if (count == CREATE_UNIQUE_UUID_TRIES)
                throw new ApiException("Failed to create confirmation token that does not exist in db. Reached maximum tries.");
                
            String token = UUID.randomUUID().toString();
            if (!existsByToken(token))
                return token;
            else 
                count++;
        }
    }


    private boolean existsByToken(String token) {

        return this.repository.existsByToken(token);
    }
}