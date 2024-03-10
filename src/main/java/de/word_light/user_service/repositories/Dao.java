package de.word_light.user_service.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import de.word_light.user_service.abstracts.AbstractEntity;


@NoRepositoryBean
public interface Dao<E extends AbstractEntity> extends JpaRepository<E, Long> {
    
}