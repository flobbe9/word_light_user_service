package de.word_light.user_service.services;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;

import java.time.LocalDateTime;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import de.word_light.user_service.abstracts.AbstractService;
import de.word_light.user_service.entities.AppUser;
import de.word_light.user_service.entities.ConfirmationToken;
import de.word_light.user_service.exception.ApiException;
import de.word_light.user_service.repositories.ConfirmationTokenRepository;


@Service
public class ConfirmationTokenService extends AbstractService<ConfirmationToken, ConfirmationTokenRepository> {

    private static final int CREATE_UNIQUE_UUID_TRIES = 1000;
    
    @Autowired
    private ConfirmationTokenRepository repository;


    /**
     * Generate non existing token and save to db with default expiration date.
     * 
     * @param appUser related to this token
     * @return save confirmation token
     */
    public ConfirmationToken saveNew(AppUser appUser) {

        if (appUser == null)
            throw new ApiException("Failed to save confirmation token. 'appUser' is null.");

        String token = createNonExistingToken();

        return super.save(new ConfirmationToken(token, appUser));
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
     * @return the appUser related to the token
     */
    public AppUser confirmToken(String token) {

        if (StringUtils.isBlank(token))
            throw new ApiException("Failed to confirm token. 'token' is blank or null.");

        ConfirmationToken confirmationToken = getByToken(token);

        if (confirmationToken.isConfirmed())
            throw new ApiException(HttpStatus.IM_USED, "Failed to confirm token. 'token' is confirmed already.");

        if (confirmationToken.isExpired())
            throw new ApiException(CONFLICT, "Failed to confirm token. 'token' is expired.");

        // set confirmed
        confirmationToken.setConfirmedAt(LocalDateTime.now());

        // save token
        save(confirmationToken);

        return confirmationToken.getAppUser();
    }


    public ConfirmationToken getByToken(String token) {

        return this.repository.findByToken(token)
                              .orElseThrow(() -> 
                                new ApiException(NOT_ACCEPTABLE, "Failed to find ConfirmationToken with 'token' " + token));
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

        throw new ApiException(CONFLICT, "Failed to create unique confirmation token. Reached maximum tries.");
    }


    private boolean existsByToken(String token) {

        return this.repository.existsByToken(token);
    }
}