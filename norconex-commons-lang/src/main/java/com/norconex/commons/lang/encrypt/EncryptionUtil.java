/* Copyright 2015 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.norconex.commons.lang.encrypt;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;

/**
 * Simplified encryption and decryption methods using the 
 * "PBEWithMD5AndDES" algorithm with a supplied security key.
 * The "salt" and iteration count used by this class are hard-coded. To have
 * more control and ensure a more secure approach, you should rely on another 
 * implementation or create your own.
 * @author Pascal Essiembre
 * @since 1.9.0
 */
public class EncryptionUtil {
    
    public static void main(String[] args) {
        if (args.length != 4) {
            printUsage();
        }
        String cmd = args[0];
        String type = args[1];
        String key = args[2];
        String text = args[3];
        if (cmd.equalsIgnoreCase("encrypt")) {
            if (type.equalsIgnoreCase("-k")) {
                System.out.println(encrypt(text, key));
            } else if (type.equalsIgnoreCase("-f")) {
                System.out.println(encryptWithKeyFile(text, new File(key)));
            } else if (type.equalsIgnoreCase("-e")) {
                System.out.println(encryptWithKeyEnv(text, key));
            } else {
                System.err.println("Unsupported type of key: " + type);
                printUsage();
            }
        } else if (cmd.equalsIgnoreCase("decrypt")) {
            if (type.equalsIgnoreCase("-k")) {
                System.out.println(decrypt(text, key));
            } else if (type.equalsIgnoreCase("-f")) {
                System.out.println(decryptWithKeyFile(text, new File(key)));
            } else if (type.equalsIgnoreCase("-e")) {
                System.out.println(decryptWithKeyEnv(text, key));
            } else {
                System.err.println("Unsupported type of key: " + type);
                printUsage();
            }
        } else {
            System.err.println("Unsupported command: " + cmd);
            printUsage();
        }
        System.exit(0);
    }
    private static void printUsage() {
        PrintStream out = System.out;
        out.println("<appName> encrypt|decrypt -k|-f|-e key text");
        out.println();
        out.println("Where:");
        out.println("  encrypt  encrypt the text with the given key.");
        out.println("  decrypt  decrypt the text with the given key.");
        out.println("  -k       key is the secret key.");
        out.println("  -f       key is the file containing the secret key.");
        out.println("  -e       key is the environment variable holding the "
                + "secret key.");
        out.println("  key      the secret key (or file, or env. variable).");
        out.println("  text     text to encrypt or decrypt");
        System.exit(-1);
    }
    
    /**
     * <p>Encrypts the given text with the encryption key supplied.</p>
     * @param textToEncrypt text to be encrypted
     * @param key security key which must be the same to encrypt and decrypt.
     * @return encrypted text or <code>null</code> if one of 
     * <code>textToEncrypt</code> or <code>key</code> is <code>null</code>.
     */
    public static String encrypt(String textToEncrypt, String key) {
        if (textToEncrypt == null || key == null) {
            return null;
        }
        
        // 8-byte Salt
        byte[] salt = {
            (byte)0xE3, (byte)0x03, (byte)0x9B, (byte)0xA9, 
            (byte)0xC8, (byte)0x16, (byte)0x35, (byte)0x56
        };
        // Iteration count
        int iterationCount = 19;
        Cipher ecipher;

        try {
            // Create the key
            KeySpec keySpec = new PBEKeySpec(
                    key.toCharArray(), salt, iterationCount);
            SecretKey secretKey = SecretKeyFactory.getInstance(
                "PBEWithMD5AndDES").generateSecret(keySpec);
            ecipher = Cipher.getInstance(secretKey.getAlgorithm());

            // Prepare the parameter to the ciphers
            AlgorithmParameterSpec paramSpec = 
                    new PBEParameterSpec(salt, iterationCount);

            // Create the ciphers
            ecipher.init(Cipher.ENCRYPT_MODE, secretKey, paramSpec);
            
            byte[] utf8 = textToEncrypt.getBytes("UTF8");
            byte[] enc = ecipher.doFinal(utf8);
            
            return DatatypeConverter.printBase64Binary(enc);
        } catch (Exception e) {
            throw new EncryptionException("Encryption failed.", e);
        }
    }
    /**
     * <p>Encrypts the given text with the encryption key found in the 
     * supplied environment variable name.</p>
     * @param textToEncrypt text to be encrypted
     * @param keyEnv environment variable name.
     * @return encrypted text or <code>null</code> if the 
     * <code>textToEncrypt</code> is <code>null</code>.
     */
    public static String encryptWithKeyEnv(
            String textToEncrypt, String keyEnv) {
        if (StringUtils.isBlank(keyEnv)) {
            throw new EncryptionException("Environment variable name "
                    + "cannot be null or blank.");
        }
        String key = System.getenv(keyEnv);
        if (StringUtils.isBlank(key)) {
            throw new EncryptionException("No key was found under the "
                    + "environment variable named \"" + keyEnv + "\".");
        }
        return encrypt(textToEncrypt, key);
    }
    /**
     * <p>Encrypts the given text with the encryption key found in the 
     * supplied file. The text in the file is expected to be UTF-8.</p>
     * @param textToEncrypt text to be encrypted
     * @param keyFile file containing security key which must be the same 
     *        to encrypt and decrypt.
     * @return encrypted text or <code>null</code> if the 
     * <code>textToEncrypt</code> is <code>null</code>.
     */
    public static String encryptWithKeyFile(
            String textToEncrypt, File keyFile) {
        if (keyFile == null ) {
            throw new EncryptionException("Key file cannot be null.");
        }
        if (!keyFile.isFile()) {
            throw new EncryptionException("Key file is not a file or does not "
                    + "exists: " + keyFile);
        }
        try {
            String key = FileUtils.readFileToString(
                    keyFile, CharEncoding.UTF_8);
            if (StringUtils.isBlank(key)) {
                throw new EncryptionException(
                        "No key was found in file: " + keyFile);
            }
            return encrypt(textToEncrypt, key);
        } catch (IOException e) {
            throw new EncryptionException(
                    "Could not read key file.", e);
        }
    }
 
    /**
     * <p>Decrypts the given encrypted text with the encryption key supplied.
     * </p>
     * @param encryptedText text to be decrypted
     * @param key security key which must be the same to encrypt and decrypt.
     * @return decrypted text or <code>null</code> if one of 
     * <code>encryptedText</code> or <code>key</code> is <code>null</code>.
     */   
    public static String decrypt(String encryptedText, String key) {
        if (encryptedText == null || key == null) {
            return null;
        }
        // 8-byte Salt
        byte[] salt = {
            (byte)0xE3, (byte)0x03, (byte)0x9B, (byte)0xA9, 
            (byte)0xC8, (byte)0x16, (byte)0x35, (byte)0x56
        };
        // Iteration count
        int iterationCount = 19;
        Cipher dcipher;

        try {
            // Create the key
            KeySpec keySpec = new PBEKeySpec(
                    key.toCharArray(), salt, iterationCount);
            SecretKey secretKey = SecretKeyFactory.getInstance(
                "PBEWithMD5AndDES").generateSecret(keySpec);
            dcipher = Cipher.getInstance(secretKey.getAlgorithm());

            // Prepare the parameter to the ciphers
            AlgorithmParameterSpec paramSpec = 
                    new PBEParameterSpec(salt, iterationCount);

            // Create the ciphers
            dcipher.init(Cipher.DECRYPT_MODE, secretKey, paramSpec);
            
            byte[] dec = DatatypeConverter.parseBase64Binary(encryptedText);
            byte[] utf8 = dcipher.doFinal(dec);
            return new String(utf8, CharEncoding.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException("Decryption failed.", e);
        }
    }

    /**
     * <p>Decrypts the given encrypted text with the encryption key found in the 
     * supplied environment variable name.</p>
     * @param encryptedText text to be decrypted
     * @param keyEnv environment variable name.
     * @return decrypted text or <code>null</code> if the 
     * <code>encryptedText</code> is <code>null</code>.
     */
    public static String decryptWithKeyEnv(
            String encryptedText, String keyEnv) {
        if (StringUtils.isBlank(keyEnv)) {
            throw new EncryptionException("Environment variable name "
                    + "cannot be null or blank.");
        }
        String key = System.getenv(keyEnv);
        if (StringUtils.isBlank(key)) {
            throw new EncryptionException("No key was found under the "
                    + "environment variable named \"" + keyEnv + "\".");
        }
        return decrypt(encryptedText, key);
    }
    
    /**
     * <p>Encrypts the given encrypted text with the encryption key found in the 
     * supplied file. The text in the file is expected to be UTF-8.</p>
     * @param encryptedText text to be decrypted
     * @param keyFile file containing security key which must be the same 
     *        to encrypt and decrypt.
     * @return decrypted text or <code>null</code> if the 
     * <code>encryptedText</code> is <code>null</code>.
     */
    public static String decryptWithKeyFile(
            String encryptedText, File keyFile) {
        if (keyFile == null ) {
            throw new EncryptionException("Key file cannot be null.");
        }
        if (!keyFile.isFile()) {
            throw new EncryptionException("Key file is not a file or does not "
                    + "exists: " + keyFile);
        }
        try {
            String key = FileUtils.readFileToString(
                    keyFile, CharEncoding.UTF_8);
            if (StringUtils.isBlank(key)) {
                throw new EncryptionException(
                        "No key was found in file: " + keyFile);
            }
            return decrypt(encryptedText, key);
        } catch (IOException e) {
            throw new EncryptionException(
                    "Could not read key file.", e);
        }
    }    
}
