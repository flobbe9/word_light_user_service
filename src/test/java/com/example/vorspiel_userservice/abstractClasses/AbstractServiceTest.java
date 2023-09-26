package com.example.vorspiel_userservice.abstractClasses;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.vorspiel_userservice.entities.AppUser;
import com.example.vorspiel_userservice.enums.AppUserRole;
import com.example.vorspiel_userservice.exception.ApiException;
import com.example.vorspiel_userservice.repositories.AppUserRepository;
import com.example.vorspiel_userservice.services.AppUserService;

import jakarta.validation.ConstraintViolationException;


// @SpringBootTest
// @AutoConfigureMockMvc
// @TestMethodOrder(OrderAnnotation.class)
// @TestInstance(Lifecycle.PER_CLASS)
// public class AbstractServiceTest {
    
//     @Autowired
//     private AppUserService appUserService;

//     @Autowired
//     private AppUserRepository appUserRepository;


//     /** Stays in db, should not be deleted by any test */
//     private AppUser appUser;
//     private String email = "max.mustermann@domain.com";
//     private String password = "Abc123..";
//     private AppUserRole role = AppUserRole.USER;

//     private AppUser secondAppUser;


//     @BeforeEach
//     void setup() {  

//         this.appUser = new AppUser(this.email, 
//                                    this.password, 
//                                    this.role);
//         this.appUser.setId(1l);

//         this.secondAppUser = new AppUser("nonexisting@domain.com", this.password, AppUserRole.USER);
//         this.secondAppUser.setId(2l);

//         this.appUser = this.appUserService.save(this.appUser);
//     }

    
//     @AfterEach
//     void cleanUp() {
        
//         removeAppUser(this.secondAppUser);
//     }


//     @Test
//     void save_shouldValidate() {

//         assertDoesNotThrow(() -> this.appUserService.save(this.appUser));

//         assertThrows(ConstraintViolationException.class, () -> this.appUserService.save(null));
//     }


//     @Test
//     @Order(0)
//     void save_shouldCreateIfNotExists() {

//         assertFalse(this.appUserRepository.existsByEmail(this.secondAppUser.getEmail()));

//         this.appUserService.save(this.secondAppUser);

//         assertTrue(this.appUserRepository.existsByEmail(this.secondAppUser.getEmail()));

//         assertNotNull(this.secondAppUser.getCreated());
//         assertNotNull(this.secondAppUser.getUpdated());
//     }


//     @Test 
//     void save_shouldUpdateIfExists() {

//         LocalDateTime created = this.appUser.getCreated();
//         assertNotNull(created);

//         LocalDateTime updated = this.appUser.getUpdated();
//         assertNotNull(updated);

//         assertEquals(this.email, this.appUser.getEmail());

//         // change email
//         String newEmail = "newEmail@domain.com";
//         this.appUser.setEmail(newEmail);

//         // update
//         this.appUserService.save(this.appUser);
        
//         AppUser updatedAppUser = this.appUserRepository.findByEmail(newEmail)
//                                                        .orElseThrow(() -> new ApiException("Failed to update appUser. Test failed."));

//         // created should be the same
//         assertNotNull(updatedAppUser.getCreated());
//         assertEquals(created.truncatedTo(ChronoUnit.MILLIS), 
//                      updatedAppUser.getCreated().truncatedTo(ChronoUnit.MILLIS));

//         // updated should have changed
//         assertNotNull(updatedAppUser.getUpdated());
//         assertNotEquals(updated.truncatedTo(ChronoUnit.MILLIS),
//                         updatedAppUser.getUpdated().truncatedTo(ChronoUnit.MILLIS));

//         // email should have changed
//         assertNotEquals(this.email, updatedAppUser.getEmail());
//     }


//     @Test
//     void getById_shouldValidate() {

//         assertDoesNotThrow(() -> this.appUserService.getById(this.appUser.getId()));

//         assertThrows(ConstraintViolationException.class, () -> this.appUserService.getById(null));
//     }


//     @Test
//     void getById_shouldThrowIfNotFound() {

//         assertDoesNotThrow(() -> this.appUserService.getById(this.appUser.getId()));

//         assertThrows(ApiException.class, () -> this.appUserService.getById(this.secondAppUser.getId()));
//     }


//     @Test
//     void delete_shouldNotExistAfterwards() {

//         assertTrue(this.appUserRepository.existsByEmail(this.email));

//         this.appUserService.delete(this.appUser);

//         assertFalse(this.appUserRepository.existsByEmail(this.email));
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


//     @AfterAll
//     void cleanAllUp() {

//         this.appUserRepository.deleteAll();
//     }
// }