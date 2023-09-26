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
import org.springframework.data.domain.Example;
import org.springframework.mail.MailException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
import jakarta.validation.ConstraintViolationException;


/**
 * Test class executing all test classes sequentially that access the db. Starts one big integration test. <p>
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


    @TestInstance(Lifecycle.PER_CLASS)
    @Nested
    @Order(1)
    public class AppUserServiceTest {

        /** Stays in db, should not be deleted by any test */
        private AppUser appUser;
        private String email = "max.mustermann@domain.com";
        private String password = "Abc123&&";
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

            assertThrows(ApiException.class, () -> appUserService.register(this.appUser));

            assertDoesNotThrow(() -> 
                expectMailingException(() -> appUserService.register(this.secondAppUser)));
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
        void update_shouldThrowOnNonExistingAppUser() {

            assertDoesNotThrow(() -> appUserService.update(this.appUser));

            assertThrows(ApiException.class, () -> appUserService.update(this.secondAppUser));
        }


        @Test
        void update_shouldValidate() {

            assertDoesNotThrow(() -> appUserService.update(this.appUser));

            assertThrows(ConstraintViolationException.class, () -> appUserService.update(null));
        }


        @AfterAll
        void cleanAllUp() {

            appUserRepository.deleteAll();
            confirmationTokenRepository.deleteAll();
        }


        /**
         * Executes given runnable. Catches {@code ApiException} and asserts that the cause was a {@link MailException}.
         * 
         * @param lambda function to execute
         */
        private void expectMailingException(Runnable lambda) {

            try {
                lambda.run();

            // expect mailing exception
            } catch (ApiException e) {
                assertTrue(e.getOriginalException() instanceof MailException);
            }
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

            assertThrows(ConstraintViolationException.class, () -> appUserService.save(null));
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

            assertThrows(ConstraintViolationException.class, () -> appUserService.getById(null));
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

        this.appUserRepository.delete(appUser);
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
}