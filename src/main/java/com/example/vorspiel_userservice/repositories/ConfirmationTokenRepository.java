package com.example.vorspiel_userservice.repositories;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.example.vorspiel_userservice.entities.ConfirmationToken;


@Repository
public interface ConfirmationTokenRepository extends Dao<ConfirmationToken> {

    boolean existsByToken(String token);

    Optional<ConfirmationToken> findByToken(String token);
    
}