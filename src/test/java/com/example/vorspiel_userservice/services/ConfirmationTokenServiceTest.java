package com.example.vorspiel_userservice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.vorspiel_userservice.entities.ConfirmationToken;
import com.example.vorspiel_userservice.exception.ApiException;
import com.example.vorspiel_userservice.repositories.ConfirmationTokenRepository;

import jakarta.validation.ConstraintViolationException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;


// @SpringBootTest
// @AutoConfigureMockMvc
// @TestInstance(Lifecycle.PER_CLASS)
// public class ConfirmationTokenServiceTest {

//     @Autowired
//     private ConfirmationTokenService confirmationTokenService;

//     @Autowired
//     private ConfirmationTokenRepository confirmationTokenRepository;

//     private ConfirmationToken confirmationToken;
//     private String token = UUID.randomUUID().toString();


//     @BeforeEach
//     void setUp() {

//         this.confirmationToken = new ConfirmationToken(this.token);
//         this.confirmationToken.setId(1l);

//         this.confirmationTokenService.save(this.confirmationToken);
//         assertTrue(this.confirmationTokenRepository.existsByToken(this.token));
//     }
    

//     @Test
//     void saveNew_shouldSave() {

//         int numConfirmationTokens = this.confirmationTokenRepository.findAll().size();

//         ConfirmationToken confirmationToken = this.confirmationTokenService.saveNew();

//         assertTrue(this.confirmationTokenRepository.existsByToken(confirmationToken.getToken()));

//         assertEquals(numConfirmationTokens + 1, this.confirmationTokenRepository.findAll().size());
//     }


//     @Test
//     void confirmToken_shouldValidate() {

//         assertDoesNotThrow(() -> this.confirmationTokenService.confirmToken(this.token));

//         assertThrows(ConstraintViolationException.class, () -> this.confirmationTokenService.confirmToken(null));
//         assertThrows(ConstraintViolationException.class, () -> this.confirmationTokenService.confirmToken(" "));
//     }


//     @Test
//     void confirmToken_shouldThrowIfNotExists() {

//         assertDoesNotThrow(() -> this.confirmationTokenService.confirmToken(this.token));

//         assertThrows(ApiException.class, () -> this.confirmationTokenService.confirmToken("nonExistintToken"));
//     }


//     @Test
//     void confirmToken_shouldThrowIfConfirmedAlready() {

//         assertDoesNotThrow(() -> this.confirmationTokenService.confirmToken(this.token));

//         assertTrue(this.confirmationTokenService.getById(1l).getConfirmedAt() != null);

//         assertThrows(ApiException.class, () -> this.confirmationTokenService.confirmToken(this.token));
//     }


//     @Test
//     void confirmToken_shouldThrowIfExpired() {

//         assertDoesNotThrow(() -> this.confirmationTokenService.confirmToken(this.token));

//         this.confirmationToken.setConfirmedAt(null);
//         this.confirmationToken.setExpiresAt(LocalDateTime.now());
//         this.confirmationTokenService.save(confirmationToken);

//         assertThrows(ApiException.class, () -> this.confirmationTokenService.confirmToken(this.token));
//     }


//     @AfterAll
//     void cleanAllUp() {

//         this.confirmationTokenRepository.deleteAll();
//     }
// }