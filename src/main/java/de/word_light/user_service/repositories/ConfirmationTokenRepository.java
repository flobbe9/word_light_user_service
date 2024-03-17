package de.word_light.user_service.repositories;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import de.word_light.user_service.entities.ConfirmationToken;
import jakarta.transaction.Transactional;


@Repository
public interface ConfirmationTokenRepository extends Dao<ConfirmationToken> {

    boolean existsByToken(String token);

    Optional<ConfirmationToken> findByToken(String token);

    @Transactional
    void deleteAllByUpdatedBefore(LocalDateTime minusDays);
}