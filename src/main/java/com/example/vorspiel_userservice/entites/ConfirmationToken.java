package com.example.vorspiel_userservice.entites;

import java.time.LocalDateTime;

import com.example.vorspiel_userservice.abstractClasses.AbstractEntity;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Getter
@Setter
@NoArgsConstructor
public class ConfirmationToken extends AbstractEntity {
    
    private String token;

    private LocalDateTime confirmedAt;

    private LocalDateTime expiresAt;


    /**
     * Sets expiration time to 15 minutes.
     */
    public ConfirmationToken(String token) {

        this.token = token;
        this.expiresAt = LocalDateTime.now().plusMinutes(15);
    }


    /**
     * @param expiresAt time of expiration.
     */
    public ConfirmationToken(String token, LocalDateTime expiresAt) {
        
        this.token = token;
        this.expiresAt = expiresAt;
    }


    public boolean isExpired() {

        return this.expiresAt.isBefore(LocalDateTime.now());
    }


    public boolean isConfirmed() {

        return this.confirmedAt != null;
    }
}