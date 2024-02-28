package com.example.vorspiel_userservice.services;

import java.io.File;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.example.vorspiel_userservice.abstractClasses.AbstractService;
import com.example.vorspiel_userservice.entities.AppUser;
import com.example.vorspiel_userservice.entities.ConfirmationToken;
import com.example.vorspiel_userservice.exception.ApiException;
import com.example.vorspiel_userservice.repositories.AppUserRepository;
import com.example.vorspiel_userservice.utils.Utils;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


/**
 * Class handling most logic for {@link AppUser} entity.
 * 
 * @since 0.0.1
 * @see AppUserRepository
 */
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
    // TODO: consider sending email first, sothat the second try will not throw duplicate
    public AppUser register(@NotNull(message = VALIDATION_NOT_NULL) @Valid AppUser appUser) {

        validatePassword(appUser.getPassword());

        saveNew(appUser);

        sendAccountVerificationMail(appUser);

        return appUser;
    }
    
    
    /**
     * Call {@code save} method of jpa repo on existing {@link AppUser} or throw if does not exist.
     * 
     * @param appUser to update
     * @param newPassword true if password has been changed
     * @return updated appUser
     * @throws ApiException if user does not exist
     */
    public AppUser update(@NotNull(message = VALIDATION_NOT_NULL) @Valid AppUser appUser) {

        AppUser oldAppUser = getById(appUser.getId());

        // case: changed password
        if (!oldAppUser.getPassword().equals(appUser.getPassword())) {
            String password = appUser.getPassword();
            validatePassword(password);
            appUser.setPassword(this.passwordEncoder.encode(password));
        }
                    
        return save(appUser);
    }


    @Override
    public AppUser loadUserByUsername(String email) {

        if (email == null || email.isBlank())
            throw new ApiException("Failed to load user. " + VALIDATION_EMAIL_NOT_BLANK);

        return repository.findByEmail(email)
                         .orElseThrow(() -> new ApiException("Failed to load user. Username '" + email + "' does not exist."));
    }


    /**
     * Confirm given {@code ConfirmationToken} and enable given {@link AppUser} account.
     * 
     * @param email of appUser
     * @param token to confirm
     */
    public void confirmAccount(@NotBlank(message = VALIDATION_EMAIL_NOT_BLANK) String email, 
                               @NotBlank(message = ConfirmationTokenService.VALIDATION_NOT_NULL) String token) {

        this.confirmationTokenService.confirmToken(token);

        enable(email);
    }
    

    /**
     * Validate given password and throw {@link ApiException} if invalid. Regex wont work with encrypted passwords.
     * 
     * @param password to check
     */
    public void validatePassword(@NotNull(message = VALIDATION_NOT_NULL) String password) {

        if (!Utils.isPasswordValid(password))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Failed to save user. 'password' pattern invalid.");
    }

    
    /**
     * Create new {@link ConfirmationToken}, save it and send a standard registration mail with confirmation link to 
     * given {@link AppUser}.
     * 
     * @param appUser to send the mail to
     */
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

        AppUser appUser = loadUserByUsername(email);

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