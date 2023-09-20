package com.example.vorspiel_userservice.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;

import com.example.vorspiel_userservice.exception.ApiException;


/**
 * Util class holding static helper methods.
 * 
 * @since 0.0.1
 */
public class Utils {

    /**
     * Convert file into String using {@link BufferedReader}.
     * 
     * @param file to convert
     * @return converted string or null, if file is null
     * @throws ApiException
     */
    // TODO: replace odd chars
    public static String fileToString(File file) {
        
        // read to string
        try (Reader fis = new FileReader(file);
             BufferedReader br = new BufferedReader(fis)) {
            StringBuilder stringBuilder = new StringBuilder();

            String line = null;
            while ((line = br.readLine()) != null)
                stringBuilder.append(line);

            return stringBuilder.toString();
            
        } catch (Exception e) {
            throw new ApiException("Failed to read file to String.", e);
        }
    }


    /**
     * Write given string to given file.
     * 
     * @param str to write to file
     * @param file to write the string to
     * @return the file
     * @throws ApiException
     */
    public static File stringToFile(String str, File file) {

        try (BufferedWriter br = new BufferedWriter(new FileWriter(file))) {
            br.write(str);

            return file;

        } catch (Exception e) {
            throw new ApiException("Failed to write String to File.", e);
        }
    }
}