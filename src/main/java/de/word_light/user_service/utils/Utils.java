package de.word_light.user_service.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;

import de.word_light.user_service.exception.ApiException;


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
    public static String fileToString(File file) {
        
        // read to string
        try (Reader fis = new FileReader(file);
             BufferedReader br = new BufferedReader(fis)) {
            StringBuilder stringBuilder = new StringBuilder();

            String line = null;
            while ((line = br.readLine()) != null)
                stringBuilder.append(line);

            String str = stringBuilder.toString();
            return replaceOddChars(str);
            
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


    /**
     * Replace odd characters that java uses for special chars like 'ä, ö, ü, ß' etc. with original chars. <p>
     * 
     * Does not alter given String.
     * 
     * @param str to fix
     * @return fixed string
     */
    public static String replaceOddChars(String str) {

        // alphabetic
        str = str.replace("Ã?", "Ä");
        str = str.replace("Ã¤", "ä");
        str = str.replace("Ã¶", "ö");
        str = str.replace("Ã¼", "ü");
        str = str.replace("ÃŸ", "ß");

        // special chars
        str = str.replace("â?¬", "€");

        return str;
    }


    /** 
     * At least <p>
     * - eight characters, <p>
     * - one uppercase letter, <p>
     * - one lowercase letter,  <p>
     * - one number and <p>
     * - one of given special characters. <p>
     * - maximum 30 characters, 
     */
    public static boolean isPasswordValid(String password) {

        if (password == null)
            throw new ApiException("Failed to validate password. 'password' cannot be null");
        
        String regex = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[,.;_!#$%&’*+/=?`{|}~^-]).{8,30}$";

        return password.matches(regex);
    }
}