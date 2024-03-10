package de.word_light.user_service.repositories;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.word_light.user_service.entities.AppUser;

import java.util.Optional;


@Repository
public interface AppUserRepository extends Dao<AppUser> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    @Transactional
    void deleteByEmail(String email);
}