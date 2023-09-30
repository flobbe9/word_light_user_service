package com.example.vorspiel_userservice.repositories;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import com.example.vorspiel_userservice.entities.AppUser;


@Repository
public interface AppUserRepository extends Dao<AppUser> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    @Transactional
    void deleteByEmail(String email);
}