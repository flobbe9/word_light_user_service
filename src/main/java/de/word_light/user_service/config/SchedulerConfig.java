package de.word_light.user_service.config;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import de.word_light.user_service.entities.ConfirmationToken;
import de.word_light.user_service.repositories.ConfirmationTokenRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;


/**
 * Class difining all cron jobs.
 * 
 * @since 0.0.1
 */
@Configuration
@EnableScheduling
@Log4j2
public class SchedulerConfig {

    @Autowired
    private ConfirmationTokenRepository confirmationTokenRepository;


    @PostConstruct
    void init() {

        log.info("Starting scheduler...");
    }


    /**
     * Delete {@link ConfirmationToken}s older than 7 days.
     */
    @Scheduled(cron = "0 0 23 * * *") // every day at 23:00 pm (seconds minutes hours * month of the year day of the week)
    public void deleteConfirmationTokens() {

        log.info("Deleting old confirmationTokens...");

        this.confirmationTokenRepository.deleteAllByUpdatedBefore(LocalDateTime.now().minusDays(7));
        
        log.info("Finished deleting old confirmation tokens.");
    }
}