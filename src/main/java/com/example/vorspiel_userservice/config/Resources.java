package com.example.vorspiel_userservice.config;

import java.io.File;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.vorspiel_userservice.exception.ApiException;


/**
 * Class injecting some resources for other beans to use with {@code @Resource}.
 * 
 * @since 0.0.1
 */
@Configuration
public class Resources {

    public static final String RESOURCES_FOLDER = "src/main/resources/";
    public static final String MAIL_FOLDER = RESOURCES_FOLDER + "mail/";
    public static final String IMG_FOLDER = RESOURCES_FOLDER + "img/";

    public static final String VERIFICATION_MAIL_FILE_NAME = "verificationMail.html";
    public static final String FAVICON_FILE_NAME = "favicon.png"; 
    

    @Bean
    File verificationMail() {

        return getFile(MAIL_FOLDER + VERIFICATION_MAIL_FILE_NAME);
    }


    @Bean
    File favicon() {

        return getFile(IMG_FOLDER + FAVICON_FILE_NAME);
    }


    /**
     * Retrieve file or throw {@code ApiException}.
     * 
     * @param filePath of file
     * @return file or null if filePath is null
     */
    private File getFile(String filePath) {

        if (filePath == null)
            return null;

        File verificationMail = new File(filePath);

        if (!verificationMail.exists())
            throw new ApiException("Failed to load resource: " + filePath);

        return verificationMail;
    }
}