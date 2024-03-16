package de.word_light.user_service.entities;

import java.time.LocalDateTime;

import de.word_light.user_service.abstracts.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class ConfirmationToken extends AbstractEntity {
    
    @NotBlank(message = "'token' cannot be blank or null")
    @EqualsAndHashCode.Include
    private String token;

    @ManyToOne
    @EqualsAndHashCode.Include
    private AppUser appUser;

    private LocalDateTime confirmedAt;

    @NotNull(message = "'expiresAt' cannot be null")
    private LocalDateTime expiresAt;


    /**
     * Sets expiration time to 15 minutes.
     */
    public ConfirmationToken(String token, AppUser appUser) {

        this.token = token;
        this.appUser = appUser;
        this.expiresAt = LocalDateTime.now().plusMinutes(15);
    }


    public ConfirmationToken(String token, AppUser appUser, LocalDateTime expiresAt) {
        
        this.token = token;
        this.appUser = appUser;
        this.expiresAt = expiresAt;
    }


    public boolean isExpired() {

        return this.expiresAt.isBefore(LocalDateTime.now());
    }


    public boolean isConfirmed() {

        return this.confirmedAt != null;
    }
}