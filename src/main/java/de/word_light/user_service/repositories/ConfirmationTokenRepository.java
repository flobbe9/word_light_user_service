package de.word_light.user_service.repositories;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import de.word_light.user_service.entities.ConfirmationToken;


@Repository
public interface ConfirmationTokenRepository extends Dao<ConfirmationToken> {

    boolean existsByToken(String token);

    Optional<ConfirmationToken> findByToken(String token);
    
}