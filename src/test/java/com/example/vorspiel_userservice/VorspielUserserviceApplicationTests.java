package com.example.vorspiel_userservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.ClassOrderer.OrderAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.UUID;

import com.example.vorspiel_userservice.entites.AppUser;
import com.example.vorspiel_userservice.entites.ConfirmationToken;
import com.example.vorspiel_userservice.enums.AppUserRole;
import com.example.vorspiel_userservice.exception.ApiException;
import com.example.vorspiel_userservice.repositories.AppUserRepository;
import com.example.vorspiel_userservice.repositories.ConfirmationTokenRepository;
import com.example.vorspiel_userservice.services.AppUserService;
import com.example.vorspiel_userservice.services.ConfirmationTokenService;
import jakarta.validation.ConstraintViolationException;


/**
 * Test class executing all test classes (sequentially if needed). <p>
 * 
 * Any new Test class has to be added here in order for the pipeline to recognize it.
 *
 * @since 0.0.1
 */
@TestClassOrder(OrderAnnotation.class)
class VorspielUserserviceApplicationTests {

    @SpringBootTest
    @AutoConfigureMockMvc
    @TestInstance(Lifecycle.PER_CLASS)
    @Nested
    @Order(1)
    public class AppUserServiceTest {

        @Autowired
        private AppUserService appUserService;

        @Autowired
        private AppUserRepository appUserRepository;
        
        @Autowired
        private ConfirmationTokenService confirmationTokenService;

        @Autowired
        private ConfirmationTokenRepository confirmationTokenRepository;

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

            this.appUser = new AppUser(this.email, 
                                    this.password, 
                                    this.role);
            this.appUser.setId(1l);

            this.secondAppUser = new AppUser("nonexisting@domain.com", this.password, AppUserRole.USER);

            this.confirmationToken = new ConfirmationToken(this.token);
            this.confirmationToken.setId(1l);

            this.appUserService.save(this.appUser);
            this.confirmationTokenService.save(this.confirmationToken);
        }

        
        @AfterEach
        void cleanUp() {
            
            // remove second appUser
            removeAppUser(this.secondAppUser);

            // reset confirmation token
            this.confirmationToken.setConfirmedAt(null);
            this.confirmationTokenService.save(this.confirmationToken);
        }


        @Test
        public void confirmAccount_shouldBeEnabled() {
            
            assertFalse(this.appUser.isEnabled());

            this.appUserService.confirmAccount(this.email, this.token);
            this.appUser = (AppUser) appUserService.loadUserByUsername(this.email);

            assertTrue(this.appUser.isEnabled());
        }


        @Test 
        void confirmAccount_shouldValidate() {

            assertDoesNotThrow(() -> this.appUserService.confirmAccount(this.email, this.token));

            // null
            assertThrows(ConstraintViolationException.class, () -> this.appUserService.confirmAccount(this.email, null));
            assertThrows(ConstraintViolationException.class, () -> this.appUserService.confirmAccount(null, this.token));

            // blank
            assertThrows(ConstraintViolationException.class, () -> this.appUserService.confirmAccount(this.email, " "));
            assertThrows(ConstraintViolationException.class, () -> this.appUserService.confirmAccount(" ", this.token));
        }


        @Test
        void loadUserByUsername_shouldFindByEmail() {

            assertThrows(ApiException.class, () -> this.appUserService.loadUserByUsername("nonexisting@domain.com"));

            assertDoesNotThrow(() -> this.appUserService.loadUserByUsername(this.email));
        }


        @Test
        void loadUserByUsername_shouldValidate() {

            assertDoesNotThrow(() -> this.appUserService.loadUserByUsername(this.email));

            assertThrows(ApiException.class, () -> this.appUserService.loadUserByUsername(null));
            assertThrows(ApiException.class, () -> this.appUserService.loadUserByUsername(" "));
        }


        @Test
        void register_shouldThrowOnDuplicateEmail() {

            assertThrows(ApiException.class, () -> this.appUserService.register(this.appUser));

            assertDoesNotThrow(() -> 
                expectMailingException(() -> this.appUserService.register(this.secondAppUser)));
        }


        @Test
        void register_shouldSave() {

            assertFalse(this.appUserRepository.existsByEmail(this.secondAppUser.getEmail()));

            expectMailingException(() -> this.appUserService.register(this.secondAppUser));

            assertTrue(this.appUserRepository.existsByEmail(this.secondAppUser.getEmail()));
        }


        @Test
        void register_shouldEncodePassword() {
            
            assertEquals(this.password, this.secondAppUser.getPassword());

            expectMailingException(() -> this.appUserService.register(this.secondAppUser));
            
            assertNotEquals(this.password, this.secondAppUser.getPassword());
        }


        @Test
        void register_shouldValidate() {

            assertThrows(ConstraintViolationException.class, () -> this.appUserService.register(null));

            assertDoesNotThrow(() -> 
                expectMailingException(() -> this.appUserService.register(this.secondAppUser)));
        }


        @Test
        void update_shouldThrowOnNonExistingAppUser() {

            assertDoesNotThrow(() -> this.appUserService.update(this.appUser));

            assertThrows(ApiException.class, () -> this.appUserService.update(this.secondAppUser));
        }


        @Test
        void update_shouldValidate() {

            assertDoesNotThrow(() -> this.appUserService.update(this.appUser));

            assertThrows(ConstraintViolationException.class, () -> this.appUserService.update(null));
        }


        @AfterAll
        void cleanAllUp() {

            this.appUserRepository.deleteAll();
            this.confirmationTokenRepository.deleteAll();
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


        /**
         * Delete given user and assert that deletion was successful.
         * 
         * @param appUser to remove
         */
        private void removeAppUser(AppUser appUser) {

            this.appUserRepository.delete(appUser);
            assertFalse(this.appUserRepository.existsByEmail(appUser.getEmail()), 
                                                            "Failed to clean up after test.");
        }
    }


    @SpringBootTest
    @AutoConfigureMockMvc
    @TestInstance(Lifecycle.PER_CLASS)
    @Nested
    @Order(2)
    public class ConfirmationTokenServiceTest {

        @Autowired
        private ConfirmationTokenService confirmationTokenService;

        @Autowired
        private ConfirmationTokenRepository confirmationTokenRepository;

        private ConfirmationToken confirmationToken;
        private String token = UUID.randomUUID().toString();


        @BeforeEach
        void setUp() {

            this.confirmationToken = new ConfirmationToken(this.token);
            this.confirmationToken.setId(1l);

            this.confirmationTokenService.save(this.confirmationToken);
            assertTrue(this.confirmationTokenRepository.existsByToken(this.token));
        }
        

        @Test
        void saveNew_shouldSave() {

            int numConfirmationTokens = this.confirmationTokenRepository.findAll().size();

            ConfirmationToken confirmationToken = this.confirmationTokenService.saveNew();

            assertTrue(this.confirmationTokenRepository.existsByToken(confirmationToken.getToken()));

            assertEquals(numConfirmationTokens + 1, this.confirmationTokenRepository.findAll().size());
        }


        @Test
        void confirmToken_shouldValidate() {

            assertDoesNotThrow(() -> this.confirmationTokenService.confirmToken(this.token));

            assertThrows(ConstraintViolationException.class, () -> this.confirmationTokenService.confirmToken(null));
            assertThrows(ConstraintViolationException.class, () -> this.confirmationTokenService.confirmToken(" "));
        }


        @Test
        void confirmToken_shouldThrowIfNotExists() {

            assertDoesNotThrow(() -> this.confirmationTokenService.confirmToken(this.token));

            assertThrows(ApiException.class, () -> this.confirmationTokenService.confirmToken("nonExistintToken"));
        }


        @Test
        void confirmToken_shouldThrowIfConfirmedAlready() {

            assertDoesNotThrow(() -> this.confirmationTokenService.confirmToken(this.token));

            assertTrue(this.confirmationTokenService.getById(1l).getConfirmedAt() != null);

            assertThrows(ApiException.class, () -> this.confirmationTokenService.confirmToken(this.token));
        }


        @Test
        void confirmToken_shouldThrowIfExpired() {

            assertDoesNotThrow(() -> this.confirmationTokenService.confirmToken(this.token));

            this.confirmationToken.setConfirmedAt(null);
            this.confirmationToken.setExpiresAt(LocalDateTime.now());
            this.confirmationTokenService.save(confirmationToken);

            assertThrows(ApiException.class, () -> this.confirmationTokenService.confirmToken(this.token));
        }


        @AfterAll
        void cleanAllUp() {

            this.confirmationTokenRepository.deleteAll();
        }
    }


    @SpringBootTest
    @AutoConfigureMockMvc
    @TestInstance(Lifecycle.PER_CLASS)
    @Nested
    @Order(3)
    // TODO: does not work, some order incorrect
    public class AbstractServiceTest {
        
        @Autowired
        private AppUserService appUserService;

        @Autowired
        private AppUserRepository appUserRepository;


        /** Stays in db, should not be deleted by any test */
        private AppUser appUser;
        private String email = "max.mustermann@domain.com";
        private String password = "Abc123..";
        private AppUserRole role = AppUserRole.USER;

        private AppUser secondAppUser;


        @BeforeEach
        void setup() {  

            this.appUser = new AppUser(this.email, 
                                    this.password, 
                                    this.role);
            this.appUser.setId(1l);

            this.secondAppUser = new AppUser("nonexisting@domain.com", this.password, AppUserRole.USER);
            this.secondAppUser.setId(2l);

            this.appUser = this.appUserService.save(this.appUser);
        }

        
        @AfterEach
        void cleanUp() {
            
            removeAppUser(this.secondAppUser);
        }


        @Test
        void save_shouldValidate() {

            assertDoesNotThrow(() -> this.appUserService.save(this.appUser));

            assertThrows(ConstraintViolationException.class, () -> this.appUserService.save(null));
        }


        @Test
        @Order(0)
        void save_shouldCreateIfNotExists() {

            assertFalse(this.appUserRepository.existsByEmail(this.secondAppUser.getEmail()));

            this.appUserService.save(this.secondAppUser);

            assertTrue(this.appUserRepository.existsByEmail(this.secondAppUser.getEmail()));

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
            this.appUserService.save(this.appUser);
            
            AppUser updatedAppUser = this.appUserRepository.findByEmail(newEmail)
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

            assertDoesNotThrow(() -> this.appUserService.getById(this.appUser.getId()));

            assertThrows(ConstraintViolationException.class, () -> this.appUserService.getById(null));
        }


        @Test
        void getById_shouldThrowIfNotFound() {

            assertDoesNotThrow(() -> this.appUserService.getById(this.appUser.getId()));

            assertThrows(ApiException.class, () -> this.appUserService.getById(this.secondAppUser.getId()));
        }


        @Test
        void delete_shouldNotExistAfterwards() {

            assertTrue(this.appUserRepository.existsByEmail(this.email));

            this.appUserService.delete(this.appUser);

            assertFalse(this.appUserRepository.existsByEmail(this.email));
        }


        /**
         * Delete given user and assert that deletion was successful.
         * 
         * @param appUser to remove
         */
        private void removeAppUser(AppUser appUser) {

            this.appUserRepository.delete(appUser);
            assertFalse(this.appUserRepository.existsByEmail(appUser.getEmail()), 
                                                            "Failed to clean up after test.");
        }


        @AfterAll
        void cleanAllUp() {

            this.appUserRepository.deleteAll();
        }
    }
}