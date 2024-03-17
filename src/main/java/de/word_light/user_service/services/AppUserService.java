package de.word_light.user_service.services;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;

import java.io.File;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import de.word_light.user_service.abstracts.AbstractService;
import de.word_light.user_service.entities.AppUser;
import de.word_light.user_service.entities.ConfirmationToken;
import de.word_light.user_service.exception.ApiException;
import de.word_light.user_service.repositories.AppUserRepository;
import de.word_light.user_service.utils.Utils;

import jakarta.annotation.Resource;


/**
 * Class handling most logic for {@link AppUser} entity.
 * 
 * @since 0.0.1
 * @see AppUserRepository
 */
@Service
// TODO: add user service to docker-compose all
// TODO: 
    // login
    // logout
    // getcsrf but protected this time
public class AppUserService extends AbstractService<AppUser, AppUserRepository> implements UserDetailsService {

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
    public AppUser register(AppUser appUser) {

        if (appUser == null)
            throw new ApiException("Failed to register user. 'appUser' is null.");

        validatePassword(appUser.getPassword());
        validateEmail(appUser.getEmail());
            
        saveNew(appUser);

        sendConfirmationMail(appUser);

        return appUser;
    }
    
    
    /**
     * Call {@code save} method of jpa repo on existing {@link AppUser} or throw if does not exist. <p>
     * 
     * Don't allow to update {@code email, created or id} fields.
     * 
     * @param appUser to update
     * @return updated appUser
     * @throws ApiException if user does not exist or email has changed
     */
    // NOTE: don't use this method directly, check that logged in user is same as given user first
    public AppUser update(AppUser appUser) {

        if (appUser == null)
            throw new ApiException("Failed to update user. 'appUser' is null.");

        AppUser oldAppUser = loadUserByUsername(appUser.getEmail());
        appUser.setId(oldAppUser.getId());
        appUser.setCreated(oldAppUser.getCreated());

        // case: changed password
        if (!oldAppUser.getPassword().equals(appUser.getPassword())) {
            String password = appUser.getPassword();
            validatePassword(password);
            appUser.setPassword(this.passwordEncoder.encode(password));
        }

        return super.save(appUser);
    }


    @Override
    public AppUser loadUserByUsername(String email) {

        if (StringUtils.isBlank(email))
            throw new ApiException("Failed to load user. 'email' is blank or null.");

        return this.repository.findByEmail(email)
                              .orElseThrow(() -> 
                                new ApiException(NOT_ACCEPTABLE, "Failed to load user. 'email' does not exist."));
    }


    /**
     * Confirm given {@code ConfirmationToken} and enable given {@link AppUser} account.
     * 
     * @param email of appUser
     * @param token to confirm
     */
    public void confirmAccount(String token) {

        if (StringUtils.isBlank(token))
            throw new ApiException("Failed to confirm account. 'token' is blank or null.");

        AppUser appUser = this.confirmationTokenService.confirmToken(token);

        enable(appUser.getEmail());
    }
    

    /**
     * Validate given password and throw {@link ApiException} if invalid. Regex wont work with encrypted passwords.
     * 
     * @param password to check
     * @throws ApiException if given {@code password} is invalid
     */
    public void validatePassword(String password) {

        if (!Utils.isPasswordValid(password))
            throw new ApiException(BAD_REQUEST, "'password' pattern invalid.");
    }

    
    /**
     * Validate given email and throw {@link ApiException} if invalid. Regex wont work with encrypted emails.
     * 
     * @param email to check
     * @throws ApiException if given {@code email} is invalid
     */
    public void validateEmail(String email) {

        if (!Utils.isEmailValid(email))
            throw new ApiException(BAD_REQUEST, "'email' pattern invalid.");
    }


    /**
     * Find {@code appUser} related to given {@code token}, create new {@code confirmationToken} and resend confirmation mail.
     * 
     * @param token old token to find appUser by
     */
    public void resendConfirmationMailByToken(String token) {

        if (StringUtils.isBlank(token))
            throw new ApiException("Failed to resend confirmation mail. 'token' is blank or null.");

        ConfirmationToken confirmationToken = this.confirmationTokenService.getByToken(token);
        AppUser appUser = confirmationToken.getAppUser();
        
        // should not happen
        if (appUser == null)
            throw new ApiException("Failed to resend confirmation mail. 'appUser' is null (should not happen).");

        sendConfirmationMail(appUser);
    }

    
    /**
     * Create new {@code confirmationToken} and resend confirmation mail.
     * 
     * @param email of appUser to send the mail to
     */
    public void resendConfirmationMailByEmail(String email) {

        if (StringUtils.isBlank(email))
            throw new ApiException("Failed to resend confirmation mail. 'email' is blank or null.");

        AppUser appUser = loadUserByUsername(email);
        sendConfirmationMail(appUser);
    }

    
    /**
     * Create new {@link ConfirmationToken}, save it and send a standard registration mail with confirmation link to 
     * given {@link AppUser}.
     * 
     * @param appUser to send the mail to
     * @throws ApiException if given {@code appUser} is enabled already
     */
    private void sendConfirmationMail(AppUser appUser) {

        // case: appUser confirmed already
        if (appUser.isEnabled())
            throw new ApiException(HttpStatus.IM_USED, "Did not resend confirmation mail. Account is already confirmed.");

        // create confirmation token
        ConfirmationToken confirmationToken = this.confirmationTokenService.saveNew(appUser);
        
        // send mail
        mailService.sendMail(appUser.getEmail(), 
                            "Word light Account bestätigen", 
                            createVerificationMail(confirmationToken), 
                            true, 
                            Map.of("favicon", this.favicon));
    }


    /**
     * Convert verificationMail.html template to String and fill placeholders
     * 
     * @param confirmationToken to append to confirmation link
     * @return formatted String of html template
     */
    private String createVerificationMail(ConfirmationToken confirmationToken) {

        // read to string
        String htmlText = Utils.fileToString(this.verificationMail);

        String confirmationLink = this.frontendBaseUrl + "/confirmAccount?token=" + confirmationToken.getToken();

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
            throw new ApiException(HttpStatus.CONFLICT, "Failed to save user. User with this email does already exist.");
           
        appUser.setPassword(passwordEncoder.encode(appUser.getPassword()));

        return save(appUser);
    }
}