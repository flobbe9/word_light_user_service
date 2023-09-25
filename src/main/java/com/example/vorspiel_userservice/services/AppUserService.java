package com.example.vorspiel_userservice.services;

import java.io.File;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.example.vorspiel_userservice.abstractClasses.AbstractService;
import com.example.vorspiel_userservice.entites.AppUser;
import com.example.vorspiel_userservice.entites.ConfirmationToken;
import com.example.vorspiel_userservice.exception.ApiException;
import com.example.vorspiel_userservice.repositories.AppUserRepository;
import com.example.vorspiel_userservice.utils.Utils;

import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


@Service
@Validated
public class AppUserService extends AbstractService<AppUser, AppUserRepository> implements UserDetailsService {

    public static final String VALIDATION_NOT_NULL = "'appUser' cannot be null.";
    public static final String VALIDATION_EMAIL_NOT_BLANK = "'email' cannot be blank or null.";


    @Autowired
    private AppUserRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ConfirmationTokenService confirmationTokenService;

    @Autowired
    private MailService mailService;

    @Resource(name = "verificationMail")
    private File verificationMail;
    
    @Resource(name = "favicon")
    private File favicon;

    @Value("${BASE_URL}")
    private String baseUrl;

    @Value("${FRONTEND_BASE_URL}")
    private String frontendBaseUrl;


    /**
     * Save new appUser as {@code disabled} and send verification mail to user.
     * 
     * @param appUser to register
     * @return registered appUser
     */
    public AppUser register(@NotNull(message = VALIDATION_NOT_NULL) @Validated AppUser appUser) {

        saveNew(appUser);

        sendAccountVerificationMail(appUser);

        return appUser;
    }
    
    
    /**
     * Call {@code save} method of jpa repo on existing {@code AppUser} or throw if does not exist.
     * 
     * @param appUser to update
     * @return updated appUser
     * @throws ApiException if user does not exist
     */
    public AppUser update(@NotNull(message = VALIDATION_NOT_NULL) @Validated AppUser appUser) {

        if (!this.repository.existsByEmail(appUser.getEmail()))
            throw new ApiException("Failed to update user. User does not exist.");
            
        return save(appUser);
    }


    @Override
    public UserDetails loadUserByUsername(String email) {

        if (email == null || email.isBlank())
            throw new ApiException("Failed to load user. " + VALIDATION_EMAIL_NOT_BLANK);

        return repository.findByEmail(email)
                         .orElseThrow(() -> new ApiException("Failed to load user. Username '" + email + "' does not exist."));
    }


    /**
     * Confirm given {@code ConfirmationToken} and enable given {@code AppUser} account.
     * 
     * @param email of appUser
     * @param token to confirm
     */
    public void confirmAccount(@NotBlank(message = VALIDATION_EMAIL_NOT_BLANK) String email, 
                               @NotBlank(message = ConfirmationTokenService.VALIDATION_NOT_NULL) String token) {

        this.confirmationTokenService.confirmToken(token);

        enable(email);
    }

    
    private void sendAccountVerificationMail(AppUser appUser) {

        // create confirmation token
        ConfirmationToken confirmationToken = this.confirmationTokenService.saveNew();
        
        // send mail
        mailService.sendMail(appUser.getEmail(), 
                            "DocumentBuilder Account bestätigen", 
                            createVerificationMail(appUser, confirmationToken), 
                            true, 
                            Map.of("favicon", this.favicon));
    }


    /**
     * Convert verificationMail.html template to String and fill placeholders
     * 
     * @param appUser to send the mail to
     * @param confirmationToken to append to confirmation link
     * @return formatted String with html template
     */
    private String createVerificationMail(AppUser appUser, ConfirmationToken confirmationToken) {

        // read to string
        String htmlText = Utils.fileToString(verificationMail);

        String confirmationLink = this.frontendBaseUrl + "/confirmAccount?" + 
                                  "email=" + appUser.getEmail() + "&" +
                                  "token=" + confirmationToken.getToken();

        // replace placeholders
        String text = String.format(htmlText, this.frontendBaseUrl, confirmationLink);
        text = Utils.replaceOddChars(text);

        return text;
    }


    /**
     * Enable given appUser if exists.
     * 
     * @param email of appUser to enable
     */
    private void enable(String email) {

        AppUser appUser = (AppUser) loadUserByUsername(email);

        appUser.setEnabled(true);

        save(appUser);
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

        return save(appUser);
    }
}