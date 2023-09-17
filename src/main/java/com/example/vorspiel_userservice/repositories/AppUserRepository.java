package com.example.vorspiel_userservice.repositories;

import org.springframework.stereotype.Repository;
import java.util.Optional;

import com.example.vorspiel_userservice.entites.AppUser;


@Repository
public interface AppUserRepository extends Dao<AppUser> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);
}