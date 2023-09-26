package com.example.vorspiel_userservice.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailException;

import com.example.vorspiel_userservice.entities.AppUser;
import com.example.vorspiel_userservice.entities.ConfirmationToken;
import com.example.vorspiel_userservice.enums.AppUserRole;
import com.example.vorspiel_userservice.exception.ApiException;
import com.example.vorspiel_userservice.repositories.AppUserRepository;
import com.example.vorspiel_userservice.repositories.ConfirmationTokenRepository;

import jakarta.validation.ConstraintViolationException;


// @SpringBootTest
// @AutoConfigureMockMvc
// @TestInstance(Lifecycle.PER_CLASS)
// public class AppUserServiceTest {

//     @Autowired
//     private AppUserService appUserService;

//     @Autowired
//     private AppUserRepository appUserRepository;
    
//     @Autowired
//     private ConfirmationTokenService confirmationTokenService;

//     @Autowired
//     private ConfirmationTokenRepository confirmationTokenRepository;

//     /** Stays in db, should not be deleted by any test */
//     private AppUser appUser;
//     private String email = "max.mustermann@domain.com";
//     private String password = "Abc123&&";
//     private AppUserRole role = AppUserRole.USER;

//     /** Will be removed after each test in order to be saved again */
//     private AppUser secondAppUser;

//     private ConfirmationToken confirmationToken;
//     private String token = UUID.randomUUID().toString();


//     @BeforeEach
//     void setup() {

//         this.appUser = new AppUser(this.email, 
//                                    this.password, 
//                                    this.role);
//         this.appUser.setId(1l);

//         this.secondAppUser = new AppUser("nonexisting@domain.com", this.password, AppUserRole.USER);

//         this.confirmationToken = new ConfirmationToken(this.token);
//         this.confirmationToken.setId(1l);

//         this.appUserService.save(this.appUser);
//         this.confirmationTokenService.save(this.confirmationToken);
//     }

    
//     @AfterEach
//     void cleanUp() {
        
//         // remove second appUser
//         removeAppUser(this.secondAppUser);

//         // reset confirmation token
//         this.confirmationToken.setConfirmedAt(null);
//         this.confirmationTokenService.save(this.confirmationToken);
//     }


//     @Test
//     public void confirmAccount_shouldBeEnabled() {
        
//         assertFalse(this.appUser.isEnabled());

//         this.appUserService.confirmAccount(this.email, this.token);
//         this.appUser = (AppUser) appUserService.loadUserByUsername(this.email);

//         assertTrue(this.appUser.isEnabled());
//     }


//     @Test 
//     void confirmAccount_shouldValidate() {

//         assertDoesNotThrow(() -> this.appUserService.confirmAccount(this.email, this.token));

//         // null
//         assertThrows(ConstraintViolationException.class, () -> this.appUserService.confirmAccount(this.email, null));
//         assertThrows(ConstraintViolationException.class, () -> this.appUserService.confirmAccount(null, this.token));

//         // blank
//         assertThrows(ConstraintViolationException.class, () -> this.appUserService.confirmAccount(this.email, " "));
//         assertThrows(ConstraintViolationException.class, () -> this.appUserService.confirmAccount(" ", this.token));
//     }


//     @Test
//     void loadUserByUsername_shouldFindByEmail() {

//         assertThrows(ApiException.class, () -> this.appUserService.loadUserByUsername("nonexisting@domain.com"));

//         assertDoesNotThrow(() -> this.appUserService.loadUserByUsername(this.email));
//     }


//     @Test
//     void loadUserByUsername_shouldValidate() {

//         assertDoesNotThrow(() -> this.appUserService.loadUserByUsername(this.email));

//         assertThrows(ApiException.class, () -> this.appUserService.loadUserByUsername(null));
//         assertThrows(ApiException.class, () -> this.appUserService.loadUserByUsername(" "));
//     }


//     @Test
//     void register_shouldThrowOnDuplicateEmail() {

//         assertThrows(ApiException.class, () -> this.appUserService.register(this.appUser));

//         assertDoesNotThrow(() -> 
//             expectMailingException(() -> this.appUserService.register(this.secondAppUser)));
//     }


//     @Test
//     void register_shouldSave() {

//         assertFalse(this.appUserRepository.existsByEmail(this.secondAppUser.getEmail()));

//         expectMailingException(() -> this.appUserService.register(this.secondAppUser));

//         assertTrue(this.appUserRepository.existsByEmail(this.secondAppUser.getEmail()));
//     }


//     @Test
//     void register_shouldEncodePassword() {
        
//         assertEquals(this.password, this.secondAppUser.getPassword());

//         expectMailingException(() -> this.appUserService.register(this.secondAppUser));
        
//         assertNotEquals(this.password, this.secondAppUser.getPassword());
//     }


//     @Test
//     void register_shouldValidate() {

//         assertThrows(ConstraintViolationException.class, () -> this.appUserService.register(null));

//         assertDoesNotThrow(() -> 
//             expectMailingException(() -> this.appUserService.register(this.secondAppUser)));
//     }


//     @Test
//     void update_shouldThrowOnNonExistingAppUser() {

//         assertDoesNotThrow(() -> this.appUserService.update(this.appUser));

//         assertThrows(ApiException.class, () -> this.appUserService.update(this.secondAppUser));
//     }


//     @Test
//     void update_shouldValidate() {

//         assertDoesNotThrow(() -> this.appUserService.update(this.appUser));

//         assertThrows(ConstraintViolationException.class, () -> this.appUserService.update(null));
//     }


//     @AfterAll
//     void cleanAllUp() {

//         this.appUserRepository.deleteAll();
//         this.confirmationTokenRepository.deleteAll();
//     }


//     /**
//      * Executes given runnable. Catches {@code ApiException} and asserts that the cause was a {@link MailException}.
//      * 
//      * @param lambda function to execute
//      */
//     private void expectMailingException(Runnable lambda) {

//         try {
//             lambda.run();

//         // expect mailing exception
//         } catch (ApiException e) {
//             assertTrue(e.getOriginalException() instanceof MailException);
//         }
//     }


//     /**
//      * Delete given user and assert that deletion was successful.
//      * 
//      * @param appUser to remove
//      */
//     private void removeAppUser(AppUser appUser) {

//         this.appUserRepository.delete(appUser);
//         assertFalse(this.appUserRepository.existsByEmail(appUser.getEmail()), 
//                                                         "Failed to clean up after test.");
//     }
// }