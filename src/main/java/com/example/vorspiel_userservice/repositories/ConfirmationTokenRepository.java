package com.example.vorspiel_userservice.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.vorspiel_userservice.entites.ConfirmationToken;


@Repository
public interface ConfirmationTokenRepository extends JpaRepository<ConfirmationToken, Long> {

    boolean existsByToken(String token);

    Optional<ConfirmationToken> findByToken(String token);
    
}