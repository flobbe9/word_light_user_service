package com.example.vorspiel_userservice.services;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.example.vorspiel_userservice.abstractClasses.AbstractService;
import com.example.vorspiel_userservice.entities.ConfirmationToken;
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


    /**
     * Generate non existing token and save to db with default expiration date.
     * 
     * @return save confirmation token
     */
    public ConfirmationToken saveNew() {

        String token = createNonExistingToken();

        return save(new ConfirmationToken(token));
    }
    

    /**
     * Find confirmation token and set confirmedAt date. <p>
     * 
     * Throws {@link ApiException} if <p>
     * - token does not exist <p>
     * - token is confirmed already <p>
     * - token is expired
     * 
     * @param token to confirm
     */
    public void confirmToken(@NotBlank(message = VALIDATION_TOKEN_NOT_BLANK) String token) {

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


    /**
     * Generate a random String with {@link UUID} {@code randomUUID()} and check that it's unique in db. If it's not unique try again
     * until it is or the max number of tries is reached.
     * 
     * @return random String
     */
    private String createNonExistingToken() {

        int count = 0;

        while (count < CREATE_UNIQUE_UUID_TRIES) {
            String token = UUID.randomUUID().toString();
            if (!existsByToken(token))
                return token;
            else 
                count++;
        }

        throw new ApiException("Failed to create unique confirmation token. Reached maximum tries.");
    }


    private boolean existsByToken(String token) {

        return this.repository.existsByToken(token);
    }
}