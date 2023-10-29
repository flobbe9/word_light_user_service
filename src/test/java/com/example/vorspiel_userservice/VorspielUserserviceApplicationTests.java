package com.example.vorspiel_userservice;

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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.vorspiel_userservice.entities.AppUser;
import com.example.vorspiel_userservice.entities.ConfirmationToken;
import com.example.vorspiel_userservice.enums.AppUserRole;
import com.example.vorspiel_userservice.exception.ApiException;
import com.example.vorspiel_userservice.repositories.AppUserRepository;
import com.example.vorspiel_userservice.repositories.ConfirmationTokenRepository;
import com.example.vorspiel_userservice.services.AppUserService;
import com.example.vorspiel_userservice.services.ConfirmationTokenService;

import jakarta.mail.MessagingException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;


/**
 * Test class executing all test classes sequentially that access the db. Starts one big integration test. <p>
 * 
 * Don't run classes parallel, use {@link Order}.
 * 
 * @since 0.0.1
 */
@SpringBootTest
@TestClassOrder(OrderAnnotation.class)
class VorspielUserserviceApplicationTests {

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private AppUserRepository appUserRepository;
    
    @Autowired
    private ConfirmationTokenService confirmationTokenService;

    @Autowired
    private ConfirmationTokenRepository confirmationTokenRepository;

    @Autowired
    private Validator validator;

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

            this.confirmationToken = new ConfirmationToken(this.token);
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
        public void confirmAccount_shouldBeEnabled() {
            
            assertFalse(this.appUser.isEnabled());

            appUserService.confirmAccount(this.email, this.token);
            this.appUser = (AppUser) appUserService.loadUserByUsername(this.email);

            assertTrue(this.appUser.isEnabled());
        }


        @Test 
        void confirmAccount_shouldValidate() {

            assertDoesNotThrow(() -> appUserService.confirmAccount(this.email, this.token));

            // null
            assertThrows(ConstraintViolationException.class, () -> appUserService.confirmAccount(this.email, null));
            assertThrows(ConstraintViolationException.class, () -> appUserService.confirmAccount(null, this.token));

            // blank
            assertThrows(ConstraintViolationException.class, () -> appUserService.confirmAccount(this.email, " "));
            assertThrows(ConstraintViolationException.class, () -> appUserService.confirmAccount(" ", this.token));
        }


        @Test
        void loadUserByUsername_shouldFindByEmail() {

            assertThrows(ApiException.class, () -> appUserService.loadUserByUsername("nonexisting@domain.com"));

            assertDoesNotThrow(() -> appUserService.loadUserByUsername(this.email));
        }


        @Test
        void loadUserByUsername_shouldValidate() {

            assertDoesNotThrow(() -> appUserService.loadUserByUsername(this.email));

            assertThrows(ApiException.class, () -> appUserService.loadUserByUsername(null));
            assertThrows(ApiException.class, () -> appUserService.loadUserByUsername(" "));
        }


        @Test
        void register_shouldThrowOnDuplicateEmail() {

            assertDoesNotThrow(() -> 
                expectMailingException(() -> appUserService.register(this.secondAppUser)));

            assertThrows(ApiException.class, () -> appUserService.register(this.secondAppUser));
        }


        @Test
        void register_shouldSave() {
            assertFalse(appUserRepository.existsByEmail(this.secondAppUser.getEmail()));

            expectMailingException(() -> appUserService.register(this.secondAppUser));

            assertTrue(appUserRepository.existsByEmail(this.secondAppUser.getEmail()));
        }


        @Test
        void register_shouldEncodePassword() {
            
            assertEquals(this.password, this.secondAppUser.getPassword());

            expectMailingException(() -> appUserService.register(this.secondAppUser));
            
            assertNotEquals(this.password, this.secondAppUser.getPassword());
        }


        @Test
        void register_shouldValidate() {

            assertThrows(ConstraintViolationException.class, () -> appUserService.register(null));

            assertDoesNotThrow(() -> 
                expectMailingException(() -> appUserService.register(this.secondAppUser)));
        }


        @Test
        @Order(1)
        void update_shouldUpdateAllFields() {
            
            String newEmail = "differentEmail@domain.com";
            String newPassword = "differenPassword2!";
            AppUserRole newRole = AppUserRole.ADMIN;
            boolean isAccountNonExpired = false;
            boolean isAccountNonLocked = false;
            boolean isCredentialsNonExpired = false;
            boolean isEnabled = true;

            // all fields should be new
            assertNotEquals(newEmail, this.appUser.getEmail());
            assertFalse(doPasswordsMatch(newPassword, this.appUser.getPassword()));
            assertNotEquals(newRole, this.appUser.getRole());
            assertNotEquals(isAccountNonExpired, this.appUser.isAccountNonExpired());
            assertNotEquals(isAccountNonLocked, this.appUser.isAccountNonLocked());
            assertNotEquals(isCredentialsNonExpired, this.appUser.isCredentialsNonExpired());
            assertNotEquals(isEnabled, this.appUser.isEnabled());

            this.appUser.setEmail(newEmail);
            this.appUser.setPassword(newPassword);
            this.appUser.setRole(newRole);
            this.appUser.setAccountNonExpired(isAccountNonExpired);
            this.appUser.setAccountNonLocked(isAccountNonLocked);
            this.appUser.setCredentialsNonExpired(isCredentialsNonExpired);
            this.appUser.setEnabled(isEnabled);

            appUserService.update(this.appUser);

            // retrieve updated user from db
            AppUser updatedAppUser = appUserRepository.findByEmail(newEmail)
                                                      .orElseThrow(() -> new ApiException("Failed to find updated user with email: " + newEmail));

            // all fields should be updated
            assertEquals(newEmail, updatedAppUser.getEmail());
            assertTrue(doPasswordsMatch(newPassword, updatedAppUser.getPassword()));
            assertEquals(newRole, updatedAppUser.getRole());
            assertEquals(isAccountNonExpired, updatedAppUser.isAccountNonExpired());
            assertEquals(isAccountNonLocked, updatedAppUser.isAccountNonLocked());
            assertEquals(isCredentialsNonExpired, updatedAppUser.isCredentialsNonExpired());
            assertEquals(isEnabled, updatedAppUser.isEnabled());
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

            AppUser updatedAppUser = appUserRepository.findByEmail(this.email).get();

            assertNotEquals(password, updatedAppUser.getPassword());

            // should be encrypted
            assertTrue(doPasswordsMatch(newPassword, updatedAppUser.getPassword()));
        }


        @Test
        void update_shouldThrowOnIdNull() {

            this.appUser.setId(null);
            assertTrue(isApiExceptionBadRequest(() -> appUserService.update(this.appUser)));
        }


        @Test
        void update_shouldThrowOnNonExistingAppUser() {

            assertDoesNotThrow(() -> appUserService.update(this.appUser));

            assertThrows(ApiException.class, () -> appUserService.update(this.secondAppUser));
        }


        @Test
        void update_shouldValidate() {

            assertDoesNotThrow(() -> appUserService.update(this.appUser));

            assertThrows(ConstraintViolationException.class, () -> appUserService.update(null));
        }


        @Test
        void validatePassword_shouldBeValidPassword() {

            List<String> invalidPasswords = List.of("&Abc123.,",
                                                    "üöAbc123$&",
                                                    "11238974Fa3#",
                                                    "123456789012345678901234567Ab,");

            invalidPasswords.forEach(password -> 
                assertDoesNotThrow(() -> appUserService.validatePassword(password), "Falsy input: " + password));
        }


        @Test
        void validatePassword_shouldBeInvalidPassword() {

            List<String> invalidPasswords = List.of(" ",
                                                 "Abc1,", // shorter than 8
                                                 "abc123#&", // no uppercase
                                                 "ABC123^+", // no lowercase
                                                 "Abcde$§", // no number
                                                 "Abcde12", // no special char
                                                 "1234,_", // no alpha char
                                                 "123456789012345678901234567890aA.", // longer than 30
                                                 "password");

            invalidPasswords.forEach(password -> 
                assertTrue(isApiExceptionBadRequest(() -> appUserService.validatePassword(password)), "Falsy input: " + password));

            assertThrows(ConstraintViolationException.class, () -> appUserService.validatePassword(null));
        }


        @Test
        void appUser_shouldBeValidEmail() {

            List<String> invalidEmails = List.of("name.879secondName@domain.com",
                                                 "name12-89secondName@domain.net",
                                                 "name@domain.name.de");

            invalidEmails.forEach(email -> {
                this.secondAppUser.setEmail(email);
                assertTrue(validator.validate(this.secondAppUser).isEmpty(), "Falsy input: " + email);
            });
        }
        

        @Test
        void appUser_shouldBeInvalidEmail() {

            List<String> invalidEmails = List.of(" ",
                                                 "invalidMaildomain.com",
                                                 "invalidMail@.com",
                                                 "invalidMail@domain.",
                                                 "invalidMail@domain",
                                                 "invalidMail@domain.weirdSuffix");

            invalidEmails.forEach(email -> {
                this.secondAppUser.setEmail(email);
                assertEquals(1, validator.validate(this.secondAppUser).size(), "Falsy input: " + email);
            });

            this.secondAppUser.setEmail(null);
            assertEquals(1, validator.validate(this.secondAppUser).size(), "Falsy input: " + null);
        }


        @Test
        void appUser_shouldValidateRole() {

            assertDoesNotThrow(() -> expectMailingException(() -> appUserService.register(this.secondAppUser)));

            this.secondAppUser.setRole(null);
            assertThrows(ConstraintViolationException.class, () -> appUserService.register(this.secondAppUser));
        }


        @AfterAll
        void cleanAllUp() {

            appUserRepository.deleteAll();
            confirmationTokenRepository.deleteAll();
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

        
        @BeforeEach
        void setUp() {
            
            this.confirmationToken = new ConfirmationToken(this.token);
            this.confirmationToken.setId(getExistingConfirmationTokenId(this.token));
            this.confirmationToken = confirmationTokenService.save(this.confirmationToken);

            this.id = this.confirmationToken.getId();    
        }
        

        @Test
        void saveNew_shouldSave() {

            int numConfirmationTokens = confirmationTokenRepository.findAll().size();

            ConfirmationToken confirmationToken = confirmationTokenService.saveNew();

            assertTrue(confirmationTokenRepository.existsByToken(confirmationToken.getToken()));

            assertEquals(numConfirmationTokens + 1, confirmationTokenRepository.findAll().size());
        }


        @Test
        @Order(1)
        void confirmToken_shouldValidate() {

            assertDoesNotThrow(() -> confirmationTokenService.confirmToken(this.token));

            assertThrows(ConstraintViolationException.class, () -> confirmationTokenService.confirmToken(null));
            assertThrows(ConstraintViolationException.class, () -> confirmationTokenService.confirmToken(" "));
        }


        @Test
        @Order(2)
        void confirmToken_shouldThrowIfNotExists() {

            assertDoesNotThrow(() -> confirmationTokenService.confirmToken(this.token));

            assertThrows(ApiException.class, () -> confirmationTokenService.confirmToken("nonExistintToken"));
        }


        @Test
        @Order(3)
        void confirmToken_shouldThrowIfConfirmedAlready() {

            assertDoesNotThrow(() -> confirmationTokenService.confirmToken(this.token));

            assertTrue(confirmationTokenService.getById(this.id).getConfirmedAt() != null);

            assertThrows(ApiException.class, () -> confirmationTokenService.confirmToken(this.token));
        }


        @Test
        @Order(4)
        void confirmToken_shouldThrowIfExpired() {

            assertDoesNotThrow(() -> confirmationTokenService.confirmToken(this.token));

            this.confirmationToken.setConfirmedAt(null);
            this.confirmationToken.setExpiresAt(LocalDateTime.now());
            confirmationTokenService.save(confirmationToken);

            assertThrows(ApiException.class, () -> confirmationTokenService.confirmToken(this.token));
        }


        @Test
        void confirmationToken_shouldValidateToken() {

            assertTrue(validator.validate(this.confirmationToken).isEmpty());

            this.confirmationToken.setToken(" ");
            assertEquals(1, validator.validate(this.confirmationToken).size());
            
            this.confirmationToken.setToken(null);
            assertEquals(1, validator.validate(this.confirmationToken).size());
        }


        @Test
        void confirmationToken_shouldValidateExpiresAt() {

            assertTrue(validator.validate(this.confirmationToken).isEmpty());

            this.confirmationToken.setExpiresAt(null);
            assertEquals(1, validator.validate(this.confirmationToken).size());
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


        @Test
        void save_shouldValidate() {

            assertDoesNotThrow(() -> appUserService.save(this.appUser));

            assertTrue(isApiExceptionBadRequest(() -> appUserService.save(null)));
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
        void save_shouldUpdateIfExists() {

            LocalDateTime created = this.appUser.getCreated();
            assertNotNull(created);

            LocalDateTime updated = this.appUser.getUpdated();
            assertNotNull(updated);

            assertEquals(this.email, this.appUser.getEmail());

            // change email
            String newEmail = "newEmail@domain.com";
            this.appUser.setEmail(newEmail);

            // update
            appUserService.save(this.appUser);
            
            AppUser updatedAppUser = appUserRepository.findByEmail(newEmail)
                                                        .orElseThrow(() -> new ApiException("Failed to update appUser. Test failed."));

            // created should be the same
            assertNotNull(updatedAppUser.getCreated());
            assertEquals(created.truncatedTo(ChronoUnit.MILLIS), 
                        updatedAppUser.getCreated().truncatedTo(ChronoUnit.MILLIS));

            // updated should have changed
            assertNotNull(updatedAppUser.getUpdated());
            assertNotEquals(updated.truncatedTo(ChronoUnit.MILLIS),
                            updatedAppUser.getUpdated().truncatedTo(ChronoUnit.MILLIS));

            // email should have changed
            assertNotEquals(this.email, updatedAppUser.getEmail());
        }


        @Test
        void getById_shouldValidate() {

            assertDoesNotThrow(() -> appUserService.getById(this.appUser.getId()));

            assertTrue(isApiExceptionBadRequest(() -> appUserService.getById(null)));
        }


        @Test
        void getById_shouldThrowIfNotFound() {

            assertDoesNotThrow(() -> appUserService.getById(this.appUser.getId()));

            assertThrows(ApiException.class, () -> appUserService.getById(this.secondAppUser.getId()));
        }


        @Test
        void delete_shouldNotExistAfterwards() {

            assertTrue(appUserRepository.existsByEmail(this.email));

            appUserService.delete(this.appUser);

            assertFalse(appUserRepository.existsByEmail(this.email));
        }


        @AfterAll
        void cleanAllUp() {

            appUserRepository.deleteAll();
        }
    }


    /**
     * Delete given user and assert that deletion was successful.
     * 
     * @param appUser to remove
     */
    private void removeAppUser(AppUser appUser) {

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
     * @return true if status of exception is 400, else false
     */
    private boolean isApiExceptionBadRequest(Runnable lambda) {

        try {
            lambda.run();

        } catch (ApiException e) {
            return e.getStatus().equals(HttpStatus.BAD_REQUEST);
        }

        return false;
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