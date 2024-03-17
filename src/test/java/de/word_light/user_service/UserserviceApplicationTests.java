package de.word_light.user_service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.ClassOrderer.OrderAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;

import de.word_light.user_service.entities.AppUser;
import de.word_light.user_service.entities.ConfirmationToken;
import de.word_light.user_service.enums.AppUserRole;
import de.word_light.user_service.exception.ApiException;
import de.word_light.user_service.repositories.AppUserRepository;
import de.word_light.user_service.repositories.ConfirmationTokenRepository;
import de.word_light.user_service.services.AppUserService;
import de.word_light.user_service.services.ConfirmationTokenService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.IM_USED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.mail.MessagingException;


/**
 * Test class executing all test classes sequentially that access the db. Starts one big integration test. <p>
 * 
 * Don't run classes parallel, use {@link Order}.
 * 
 * @since 0.0.1
 */
@SpringBootTest
@TestClassOrder(OrderAnnotation.class)
class UserserviceApplicationTests {

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private AppUserRepository appUserRepository;
    
    @Autowired
    private ConfirmationTokenService confirmationTokenService;

    @Autowired
    private ConfirmationTokenRepository confirmationTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    /**
     * Includes tests for {@link AppUserService} and {@link AppUser}.
     * 
     * @since 0.0.1
     */
    @TestInstance(Lifecycle.PER_CLASS)
    @Nested
    @Order(1)
    @TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
    public class AppUserServiceTest {

        /** Stays in db, should not be deleted by any test */
        private AppUser appUser;
        private String email = "max.mustermann@domain.com";
        private String password = "Abc123,.";
        private AppUserRole role = AppUserRole.USER;

        /** Will be removed after each test in order to be saved again */
        private AppUser secondAppUser;

        private ConfirmationToken confirmationToken;
        private String token = UUID.randomUUID().toString();


        @BeforeEach
        void setup() {

            this.appUser = new AppUser(this.email, this.password, this.role);
            this.appUser.setId(getExistingAppUserId(this.email));
            this.appUser = appUserService.save(this.appUser);

            this.secondAppUser = new AppUser("nonexisting@domain.com", this.password, AppUserRole.USER);

            this.confirmationToken = new ConfirmationToken(this.token, this.appUser);
            this.confirmationToken.setId(getExistingConfirmationTokenId(this.token));
            this.confirmationToken = confirmationTokenService.save(this.confirmationToken);
        }

        
        @AfterEach
        void cleanUp() {
            
            // remove second appUser
            removeAppUser(this.secondAppUser);

            // reset confirmation token
            this.confirmationToken.setConfirmedAt(null);
            confirmationTokenService.save(this.confirmationToken);
        }

        @Test
        void test() {

            this.appUser = new AppUser(this.email, this.password, this.role);
            this.appUser.setId(getExistingAppUserId(this.email));
            this.appUser = appUserService.save(this.appUser);

            this.secondAppUser = new AppUser("nonexisting@domain.com", this.password, AppUserRole.USER);

            this.confirmationToken = new ConfirmationToken(this.token, this.appUser);
            this.confirmationToken.setId(getExistingConfirmationTokenId(this.token));
            this.confirmationToken = confirmationTokenService.save(this.confirmationToken);
        }


// --------- confirmAccount()
        @Test
        public void confirmAccount_shouldBeEnabled() {
            
            assertFalse(this.appUser.isEnabled());
            assertFalse(this.confirmationToken.isConfirmed());

            appUserService.confirmAccount(this.token);
            this.appUser = appUserService.loadUserByUsername(this.email);

            assertTrue(this.appUser.isEnabled());
            this.confirmationToken = confirmationTokenService.getByToken(this.token);
            assertTrue(this.confirmationToken.isConfirmed());        
        }


        @Test 
        void confirmAccount_shouldValidate() {

            assertDoesNotThrow(() -> appUserService.confirmAccount(this.token));

            assertApiExceptionAndStatus(() -> appUserService.confirmAccount(null), INTERNAL_SERVER_ERROR);
            assertApiExceptionAndStatus(() -> appUserService.confirmAccount(""), INTERNAL_SERVER_ERROR);
            assertApiExceptionAndStatus(() -> appUserService.confirmAccount(" "), INTERNAL_SERVER_ERROR);
        }


// --------- loadUserByUsername()
        @Test
        void loadUserByUsername_shouldFindByEmail() {

            assertApiExceptionAndStatus(() -> appUserService.loadUserByUsername("nonexisting@domain.com"), NOT_ACCEPTABLE);

            AppUser appUser = appUserService.loadUserByUsername(this.email);
            assertEquals(this.email, appUser.getEmail());
        }


        @Test
        void loadUserByUsername_shouldValidate() {

            assertDoesNotThrow(() -> appUserService.loadUserByUsername(this.email));

            assertApiExceptionAndStatus(() -> appUserService.loadUserByUsername(null), INTERNAL_SERVER_ERROR);
            assertApiExceptionAndStatus(() -> appUserService.loadUserByUsername(""), INTERNAL_SERVER_ERROR);
            assertApiExceptionAndStatus(() -> appUserService.loadUserByUsername(" "), INTERNAL_SERVER_ERROR);
        }


// --------- register()
        @Test
        void register_shouldThrowOnDuplicateEmail() {

            assertApiExceptionAndStatus(() -> appUserService.register(this.appUser), CONFLICT);
        }


        @Test
        void register_shouldSave() {

            assertFalse(appUserRepository.existsByEmail(this.secondAppUser.getEmail()));

            appUserService.register(this.secondAppUser);

            assertTrue(appUserRepository.existsByEmail(this.secondAppUser.getEmail()));
        }


        @Test
        void register_shouldEncodePassword() {
            
            assertEquals(this.password, this.secondAppUser.getPassword());

            appUserService.register(this.secondAppUser);
            
            assertNotEquals(this.password, this.secondAppUser.getPassword());
        }


        @Test
        void register_shouldValidate() {

            assertApiExceptionAndStatus(() -> appUserService.register(null), INTERNAL_SERVER_ERROR);

            String secondAppUserEmail = this.secondAppUser.getEmail();

            this.secondAppUser.setEmail("invalidEmail");
            assertApiExceptionAndStatus(() -> 
                appUserService.register(this.secondAppUser), BAD_REQUEST);
            this.secondAppUser.setEmail(secondAppUserEmail);

            this.secondAppUser.setPassword("invalidPasword");
            assertApiExceptionAndStatus(() -> 
                appUserService.register(this.secondAppUser), BAD_REQUEST);
            this.secondAppUser.setPassword(this.password);

            assertDoesNotThrow(() -> appUserService.register(this.secondAppUser));
        }


// -------- resendConfirmationMailByToken
        @Test
        void resendConfirmationMailByToken_shouldNotThrow() {

            assertDoesNotThrow(() -> appUserService.resendConfirmationMailByToken(this.token));
        }


        @Test
        void resendConfirmationMailByToken_shouldThrow() {

            assertApiExceptionAndStatus(() ->
                appUserService.resendConfirmationMailByToken(null), INTERNAL_SERVER_ERROR);

            assertApiExceptionAndStatus(() ->
                appUserService.resendConfirmationMailByToken(""), INTERNAL_SERVER_ERROR);

            assertApiExceptionAndStatus(() ->
                appUserService.resendConfirmationMailByToken("nonExistingToken"), NOT_ACCEPTABLE);
        }


        // -------- resendConfirmationMailByEmail
        @Test
        void resendConfirmationMailByEmail_shouldNotThrow() {

            assertDoesNotThrow(() -> appUserService.resendConfirmationMailByEmail(this.email));
        }


        @Test
        void resendConfirmationMailByEmail_shouldThrow() {

            assertApiExceptionAndStatus(() ->
                appUserService.resendConfirmationMailByEmail(null), INTERNAL_SERVER_ERROR);

            assertApiExceptionAndStatus(() ->
                appUserService.resendConfirmationMailByEmail(""), INTERNAL_SERVER_ERROR);

            assertApiExceptionAndStatus(() ->
                appUserService.resendConfirmationMailByEmail("nonExistingEmail"), NOT_ACCEPTABLE);
        }


// -------- update()
        @Test
        void update_shouldUpdateAllFields() {
            
            // try to update allthough wont work
            Long newId = this.appUser.getId() + 1;
            LocalDateTime newCreated = LocalDateTime.now();
            String newEmail = "newEmail@domain.com";

            String newPassword = "differenPassword2!";
            AppUserRole newRole = AppUserRole.ADMIN;
            boolean isAccountNonExpired = false;
            boolean isAccountNonLocked = false;
            boolean isCredentialsNonExpired = false;
            boolean isEnabled = true;

            // all fields should be different
            assertNotEquals(newId, this.appUser.getId());
            assertNotEquals(newCreated, this.appUser.getCreated());
            assertNotEquals(newEmail, this.appUser.getEmail());

            assertFalse(doPasswordsMatch(newPassword, this.appUser.getPassword()));
            assertNotEquals(newRole, this.appUser.getRole());
            assertNotEquals(isAccountNonExpired, this.appUser.isAccountNonExpired());
            assertNotEquals(isAccountNonLocked, this.appUser.isAccountNonLocked());
            assertNotEquals(isCredentialsNonExpired, this.appUser.isCredentialsNonExpired());
            assertNotEquals(isEnabled, this.appUser.isEnabled());

            this.appUser.setPassword(newPassword);
            this.appUser.setRole(newRole);
            this.appUser.setAccountNonExpired(isAccountNonExpired);
            this.appUser.setAccountNonLocked(isAccountNonLocked);
            this.appUser.setCredentialsNonExpired(isCredentialsNonExpired);
            this.appUser.setEnabled(isEnabled);

            appUserService.update(this.appUser);

            // retrieve updated user from db
            AppUser updatedAppUser = appUserService.loadUserByUsername(this.email);

            // all fields should be updated except for email, created and id
            assertNotEquals(newCreated, this.appUser.getCreated());
            assertNotEquals(newId, this.appUser.getId());
            assertNotEquals(newEmail, this.appUser.getEmail());

            assertEquals(this.email, updatedAppUser.getEmail());
            assertTrue(doPasswordsMatch(newPassword, updatedAppUser.getPassword()));
            assertEquals(newRole, updatedAppUser.getRole());
            assertEquals(isAccountNonExpired, updatedAppUser.isAccountNonExpired());
            assertEquals(isAccountNonLocked, updatedAppUser.isAccountNonLocked());
            assertEquals(isCredentialsNonExpired, updatedAppUser.isCredentialsNonExpired());
            assertEquals(isEnabled, updatedAppUser.isEnabled());
        }


        @Test
        void update_shouldThrowOnUpdateMail() {

            this.appUser.setEmail("newEmail@domain.com");
            assertApiExceptionAndStatus(() -> appUserService.update(this.appUser), NOT_ACCEPTABLE);
            
            this.appUser.setEmail(this.email);
            assertDoesNotThrow(() -> appUserService.update(this.appUser));
        }


        @Test
        void update_shouldKeepPasswordIfNotChanged() {

            String password = this.appUser.getPassword();

            appUserService.update(this.appUser);

            AppUser updatedAppUser = appUserRepository.findByEmail(this.email).get();

            assertEquals(password, updatedAppUser.getPassword());
        }


        @Test
        void update_shouldChangePasswordIfChanged() {

            String password = this.appUser.getPassword();
            String newPassword = this.password + "&&";

            this.appUser.setPassword(newPassword);

            appUserService.update(this.appUser);

            AppUser updatedAppUser = appUserService.loadUserByUsername(this.email);

            assertNotEquals(password, updatedAppUser.getPassword());

            // should be encrypted
            assertTrue(doPasswordsMatch(newPassword, updatedAppUser.getPassword()));
        }


        @Test
        void update_shouldThrowOnNonExistingAppUser() {

            assertDoesNotThrow(() -> appUserService.update(this.appUser));

            assertApiExceptionAndStatus(() -> appUserService.update(this.secondAppUser), NOT_ACCEPTABLE);
        }


        @Test
        void update_shouldValidate() {

            assertDoesNotThrow(() -> appUserService.update(this.appUser));

            assertApiExceptionAndStatus(() -> appUserService.update(null), INTERNAL_SERVER_ERROR);
        }


// -------- validatePassword()
        @Test
        void validatePassword_shouldBeValidPassword() {

            List<String> validPasswords = List.of("&Abc123.,",
                                                    "üöAbc123$&",
                                                    "11238974Fa3#",
                                                    "123456789012345678901234567Ab,");

            validPasswords.forEach(password -> 
                assertDoesNotThrow(() -> 
                    appUserService.validatePassword(password), "Falsy input: " + password));
        }


        @Test
        void validatePassword_shouldBeInvalidPassword() {

            List<String> invalidPasswords = List.of(" ",
                                                 "Abc1,", // shorter than 8
                                                 "abc123#&", // no uppercase
                                                 "ABC123^+", // no lowercase
                                                 "Abcdef$§", // no number
                                                 "Abcde123", // no special char
                                                 "123456,_", // no alpha char
                                                 "123456789012345678901234567890aA.", // longer than 30
                                                 "password");

            invalidPasswords.forEach(password -> 
                assertApiExceptionAndStatus(() -> appUserService.validatePassword(password), BAD_REQUEST, "Falsy input: " + password));
        
            assertApiExceptionAndStatus(() -> appUserService.validateEmail(null), BAD_REQUEST, "Falsy input: " + password);
        }


// -------- validateEmail()
        @Test
        void validateEmail_shouldBeValidEmail() {

            List<String> validEmails = List.of("name.879secondName@domain.com",
                                                 "name12-89secondName@domain.net",
                                                 "name@domain.name.de");

            validEmails.forEach(email -> 
                assertDoesNotThrow(() -> 
                    appUserService.validateEmail(email), "Falsy input: " + email));
        }
        

        @Test
        void validateEmail_shouldBeInvalidEmail() {

            List<String> invalidEmails = List.of(" ",
                                                 "invalidMaildomain.com", // non @
                                                 "invalidMail@.com", // no domain
                                                 "invalidMail@domain.", // no suffix
                                                 "invalidMail@domain.weirdSuffix"); // invalid suffix

            invalidEmails.forEach(email -> 
                assertApiExceptionAndStatus(() -> appUserService.validateEmail(email), BAD_REQUEST, "Falsy input: " + email));

            assertApiExceptionAndStatus(() -> appUserService.validateEmail(null), BAD_REQUEST, "Falsy input: " + email);
        }


        @AfterAll
        void cleanAllUp() {

            confirmationTokenRepository.deleteAll();
            appUserRepository.deleteAll();
        }
    }
    
    
    @TestInstance(Lifecycle.PER_CLASS)
    @Nested
    @Order(2)
    @TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
    public class ConfirmationTokenServiceTest {

        private ConfirmationToken confirmationToken;
        private long id;
        private String token = UUID.randomUUID().toString();

        private AppUser appUser;


        @BeforeEach
        void setup() {

            this.appUser = new AppUser("max.mustermann@domain.com", "Abc123,.", AppUserRole.USER);
            this.appUser.setId(getExistingAppUserId("max.mustermann@domain.com"));
            this.appUser = appUserService.save(this.appUser);

            this.confirmationToken = new ConfirmationToken(this.token, this.appUser);
            this.confirmationToken.setId(getExistingConfirmationTokenId(this.token));
            this.confirmationToken = confirmationTokenService.save(this.confirmationToken);

            this.id = this.confirmationToken.getId();    
        }
        

// --------- saveNew()
        @Test
        void saveNew_shouldSave() {

            int numConfirmationTokens = confirmationTokenRepository.findAll().size();

            ConfirmationToken confirmationToken = confirmationTokenService.saveNew(this.appUser);

            assertTrue(confirmationTokenRepository.existsByToken(confirmationToken.getToken()));

            assertEquals(numConfirmationTokens + 1, confirmationTokenRepository.findAll().size());
        }


        @Test
        void saveNew_shouldThrowNull() {

            assertDoesNotThrow(() -> confirmationTokenService.saveNew(this.appUser));

            assertApiExceptionAndStatus(() -> confirmationTokenService.saveNew(null), INTERNAL_SERVER_ERROR);
        }


// --------- confirmToken()    
        @Test
        @Order(1)
        void confirmToken_shouldValidate() {

            assertDoesNotThrow(() -> confirmationTokenService.confirmToken(this.token));

            assertApiExceptionAndStatus(() -> confirmationTokenService.confirmToken(null), INTERNAL_SERVER_ERROR);
            assertApiExceptionAndStatus(() -> confirmationTokenService.confirmToken(" "), INTERNAL_SERVER_ERROR);
            assertApiExceptionAndStatus(() -> confirmationTokenService.confirmToken(""), INTERNAL_SERVER_ERROR);
        }


        @Test
        @Order(2)
        void confirmToken_shouldThrowIfNotExists() {

            assertDoesNotThrow(() -> confirmationTokenService.confirmToken(this.token));

            assertApiExceptionAndStatus(() -> confirmationTokenService.confirmToken("nonExistintToken"), NOT_ACCEPTABLE);
        }


        @Test
        @Order(3)
        void confirmToken_shouldThrowIfConfirmedAlready() {

            assertDoesNotThrow(() -> confirmationTokenService.confirmToken(this.token));

            assertTrue(confirmationTokenService.getById(this.id).getConfirmedAt() != null);

            assertApiExceptionAndStatus(() -> confirmationTokenService.confirmToken(this.token), IM_USED);
        }


        @Test
        @Order(4)
        void confirmToken_shouldThrowIfExpired() {

            assertDoesNotThrow(() -> confirmationTokenService.confirmToken(this.token));

            this.confirmationToken.setConfirmedAt(null);
            this.confirmationToken.setExpiresAt(LocalDateTime.now());
            confirmationTokenService.save(confirmationToken);

            assertApiExceptionAndStatus(() -> confirmationTokenService.confirmToken(this.token), CONFLICT);
        }


// --------- getByToken()
        @Test
        void getByToken_shouldFindConfirmationToken() {

            assertDoesNotThrow(() -> confirmationTokenService.getByToken(this.token));
        }


        @Test
        void getByToken_shouldThrowNotAcceptable() {

            assertDoesNotThrow(() -> confirmationTokenService.getByToken(this.token));

            assertApiExceptionAndStatus(() ->
                confirmationTokenService.getByToken("nonExistingToken"), NOT_ACCEPTABLE);
        }


        @AfterAll
        void cleanAllUp() {

            confirmationTokenRepository.deleteAll();
        }
    }


    @TestInstance(Lifecycle.PER_CLASS)
    @Nested
    @Order(3)
    @TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
    public class AbstractServiceTest {

        /** Stays in db, should not be deleted by any test */
        private AppUser appUser;
        private String email = "max.mustermann@domain.com";
        private String password = "Abc123..";
        private AppUserRole role = AppUserRole.USER;

        private AppUser secondAppUser;


        @BeforeEach
        void setup() {  

            this.appUser = new AppUser(this.email, this.password, this.role);
            this.appUser.setId(getExistingAppUserId(this.email));
            this.appUser = appUserService.save(this.appUser);

            String secondAppUserEmail = "nonexisting@domain.com";
            this.secondAppUser = new AppUser(secondAppUserEmail, this.password, AppUserRole.USER);
            this.secondAppUser.setId(getExistingAppUserId(secondAppUserEmail));
        }

        
        @AfterEach
        void cleanUp() {

            removeAppUser(this.secondAppUser);
        }


// --------- save()
        @Test
        void save_shouldValidate() {

            assertDoesNotThrow(() -> appUserService.save(this.appUser));

            assertApiExceptionAndStatus(() -> appUserService.save(null), INTERNAL_SERVER_ERROR);
        }


        @Test
        @Order(0)
        void save_shouldCreateIfNotExists() {

            assertFalse(appUserRepository.existsByEmail(this.secondAppUser.getEmail()));

            this.secondAppUser = appUserService.save(this.secondAppUser);

            assertTrue(appUserRepository.existsByEmail(this.secondAppUser.getEmail()));

            assertNotNull(this.secondAppUser.getCreated());
            assertNotNull(this.secondAppUser.getUpdated());
        }


        @Test 
        @Order(1)
        void save_shouldUpdateIfExists() {

            assertNotNull(this.appUser.getCreated());

            assertNotNull(this.appUser.getUpdated());

            assertEquals(this.role, this.appUser.getRole());

            // change role
            AppUserRole newRole = AppUserRole.ADMIN;
            this.appUser.setRole(newRole);

            // update
            AppUser updatedAppUser = appUserService.save(this.appUser);
            
            // created should be the same
            assertNotNull(updatedAppUser.getCreated());
            assertEquals(this.appUser.getCreated(), updatedAppUser.getCreated());

            // updated should have changed
            assertNotEquals(this.appUser.getUpdated(), updatedAppUser.getUpdated());

            // role should have changed
            assertEquals(newRole, updatedAppUser.getRole());
        }


// --------- getById()
        @Test
        void getById_shouldValidate() {

            assertDoesNotThrow(() -> appUserService.getById(this.appUser.getId()));

            assertApiExceptionAndStatus(() -> appUserService.getById(null), INTERNAL_SERVER_ERROR);
        }


        @Test
        void getById_shouldThrowIfNotFound() {

            assertDoesNotThrow(() -> appUserService.getById(this.appUser.getId()));

            assertApiExceptionAndStatus(() -> appUserService.getById(this.secondAppUser.getId()), NOT_ACCEPTABLE);
        }


// --------- delete()
        @Test
        void delete_shouldValidate() {

            assertDoesNotThrow(() -> appUserService.delete(this.appUser));

            assertApiExceptionAndStatus(() -> appUserService.delete(null), INTERNAL_SERVER_ERROR);
        }


        @Test
        void delete_shouldNotExistAfterwards() {

            assertTrue(appUserRepository.existsByEmail(this.email));

            appUserService.delete(this.appUser);

            assertFalse(appUserRepository.existsByEmail(this.email));
        }


        @AfterAll
        void cleanAllUp() {

            confirmationTokenRepository.deleteAll();
            appUserRepository.deleteAll();
        }
    }


    /**
     * Delete given user and assert that deletion was successful.
     * 
     * @param appUser to remove
     */
    private void removeAppUser(AppUser appUser) {

        this.confirmationTokenRepository.deleteAllByAppUserEmail(appUser.getEmail());

        this.appUserRepository.deleteByEmail(appUser.getEmail());
        assertFalse(this.appUserRepository.existsByEmail(appUser.getEmail()), "Failed to clean up after test.");
    }


    /**
     * Find {@link ConfirmationToken} id by given {@code token} or return 1 if does not exist.
     * 
     * @param token of the ConfirmationToken
     * @return id of the ConfirmationToken or 1 if not exists
     */
    private Long getExistingConfirmationTokenId(String token) {

        Optional<ConfirmationToken> confirmationTokens = this.confirmationTokenRepository.findByToken(token);
        if (confirmationTokens.isPresent())
            return confirmationTokens.get().getId();

        return 1l;
    }


    /**
     * Find {@link AppUser} id by given {@code email} or return 1 if does not exist.
     * 
     * @param email of the AppUser
     * @return id of the AppUser or 1 if not exists
     */
    private Long getExistingAppUserId(String email) {

        Optional<AppUser> appUsers = this.appUserRepository.findByEmail(email);
        if (appUsers.isPresent())
            return appUsers.get().getId();

        return 1l;
    }


    /**
     * Catch {@link ApiException} and check if http status is {@code BAD_REQUEST}.
     * 
     * @param lambda function to call inside the try catch block
     * @param httpStatus to expect from exception
     * @param message to display with thrown exception
     * @return true if status of exception is 400, else false
     */
    private void assertApiExceptionAndStatus(Runnable lambda, HttpStatus httpStatus, String message) {

        ApiException exception = assertThrows(ApiException.class, () -> lambda.run(), message);
        assertEquals(httpStatus, exception.getStatus());
    }


    private void assertApiExceptionAndStatus(Runnable lambda, HttpStatus httpStatus) {

        assertApiExceptionAndStatus(lambda, httpStatus, "");
    }


    /**
     * Executes given runnable. Catches {@code ApiException} and asserts that the cause was a {@link MailException} or a {@link MessagingException}.
     * 
     * @param lambda function to execute
     */
    private void expectMailingException(Runnable lambda) {

        try {
            lambda.run();

        // expect mailing exception
        } catch (ApiException e) {
            Exception originalException = e.getOriginalException();
            assertTrue(originalException instanceof MailException || originalException instanceof MessagingException);
            assertEquals(e.getStatus(), INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * Check if encoded password matches the raw one.
     * 
     * @param decodedPassword raw password to check
     * @param encodedPassword encrypted password to compare to
     * @return true if ecoded password matches the decoded one
     */
    private boolean doPasswordsMatch(String decodedPassword, String encodedPassword) {

        return passwordEncoder.matches(decodedPassword, encodedPassword);  
    }
}