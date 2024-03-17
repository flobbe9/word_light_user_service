package de.word_light.user_service.abstracts;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;


/**
 * Abstract class that all classes annotated as {@code @Entity} should extend.
 * 
 * @since 0.0.1
 */
@MappedSuperclass
@Getter
@Setter
public abstract class AbstractEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    @Schema(hidden = true)
    private LocalDateTime created;

    @Schema(hidden = true)
    private LocalDateTime updated;


    @PrePersist
    @PreUpdate
    void update() {

        if (this.created == null) 
            this.created = LocalDateTime.now();

        this.updated = LocalDateTime.now();
    }
}