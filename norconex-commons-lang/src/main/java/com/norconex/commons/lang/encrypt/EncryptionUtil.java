/* Copyright 2015-2017 Norconex Inc.
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

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import com.norconex.commons.lang.encrypt.EncryptionKey.Source;
import com.norconex.commons.lang.io.ByteArrayOutputStream;

/**
 * <p>Simplified encryption and decryption methods using the
 * "PBEWithMD5AndDES" algorithm with a supplied encryption key (which you
 * can also think of as a passphrase, or password).
 * The "salt" and iteration count used by this class are hard-coded. To have
 * more control and ensure a more secure approach, you should rely on another
 * implementation or create your own.
 * </p>
 * <p>
 * To use on the command prompt, use the following command to print usage
 * options:
 * </p>
 * <pre>
 * java -cp norconex-commons-lang-[version].jar com.norconex.commons.lang.encrypt.EncryptionUtil
 * </pre>
 * <p>
 * For example, to use a encryption key store in a file to encrypt some text,
 * add the following arguments to the above command:
 * </p>
 * <pre>
 * &lt;above_command&gt; encrypt -f "/path/to/key.txt" "Encrypt this text"
 * </pre>
 * <p>
 * As of 1.13.0, you can also use the <code>encrypt.[sh|bat]</code> and
 * <code>decrypt.[sh|bat]</code> files distributed with this library.
 * </p>
 *
 * @author Pascal Essiembre
 * @since 1.9.0
 */
public class EncryptionUtil {

    private EncryptionUtil() {
        super();
    }

    public static void main(String[] args) {
        if (args.length != 4) {
            printUsage();
        }
        String cmdArg = args[0];
        String typeArg = args[1];
        String keyArg = args[2];
        String textArg = args[3];

        Source type = null;
        if ("-k".equalsIgnoreCase(typeArg)) {
            type = Source.KEY;
        } else if ("-f".equalsIgnoreCase(typeArg)) {
            type = Source.FILE;
        } else if ("-e".equalsIgnoreCase(typeArg)) {
            type = Source.ENVIRONMENT;
        } else if ("-p".equalsIgnoreCase(typeArg)) {
            type = Source.PROPERTY;
        } else {
            System.err.println("Unsupported type of key: " + type);
            printUsage();
        }

        EncryptionKey key = new EncryptionKey(keyArg, type);
        if ("encrypt".equalsIgnoreCase(cmdArg)) {
            System.out.println(encrypt(textArg, key));
        } else if ("decrypt".equalsIgnoreCase(cmdArg)) {
            System.out.println(decrypt(textArg, key));
        } else {
            System.err.println("Unsupported command: " + cmdArg);
            printUsage();
        }
        System.exit(0);
    }
    private static void printUsage() {
        PrintStream out = System.out;
        out.println("<appName> encrypt|decrypt -k|-f|-e|-p key text");
        out.println();
        out.println("Where:");
        out.println("  encrypt  encrypt the text with the given key");
        out.println("  decrypt  decrypt the text with the given key");
        out.println("  -k       key is the encryption key");
        out.println("  -f       key is the file containing the encryption key");
        out.println("  -e       key is the environment variable holding the "
                + "encryption key");
        out.println("  -p       key is the system property holding the "
                + "encryption key");
        out.println("  key      the encryption key (or file, or env. "
                + "variable, etc.)");
        out.println("  text     text to encrypt or decrypt");
        System.exit(-1);
    }

    /**
     * <p>Encrypts the given text with the encryption key supplied. If the
     * encryption key is <code>null</code> or resolves to blank key,
     * the text to encrypt will be returned unmodified.</p>
     * @param textToEncrypt text to be encrypted
     * @param encryptionKey encryption key which must resolve to the same
     *        value to encrypt and decrypt the supplied text.
     * @return encrypted text or <code>null</code> if
     * <code>textToEncrypt</code> is <code>null</code>.
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static String encrypt(
            String textToEncrypt, EncryptionKey encryptionKey) {
        // 8-byte Salt
        byte[] salt = {
            (byte)0xE3, (byte)0x03, (byte)0x9B, (byte)0xA9,
            (byte)0xC8, (byte)0x16, (byte)0x35, (byte)0x56
        };
        if (textToEncrypt == null) {
            return null;
        }
        if (encryptionKey == null) {
            return textToEncrypt;
        }
        String key = encryptionKey.resolve();
        if (key == null) {
            return textToEncrypt;
        }

        // Iteration count
        int iterationCount = 1000;
        int keySize = 128;
        Cipher ecipher;

        try {
            // Create the key
            KeySpec keySpec = new PBEKeySpec(
                    key.trim().toCharArray(), salt, iterationCount, keySize);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            SecretKey secretKeyTemp = factory.generateSecret(keySpec);
            SecretKey secretKey = new SecretKeySpec(secretKeyTemp.getEncoded(), "AES");

            ecipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            ecipher.init(Cipher.ENCRYPT_MODE, secretKey);

            AlgorithmParameters params = ecipher.getParameters();

            byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
            byte[] utf8 = textToEncrypt.trim().getBytes(StandardCharsets.UTF_8);
            byte[] cipherBytes = ecipher.doFinal(utf8);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.write(iv);
            bos.write(cipherBytes);
            bos.close();
            byte[] cryptMessage = bos.toByteArray();

            return DatatypeConverter.printBase64Binary(cryptMessage);
        } catch (Exception e) {
            throw new EncryptionException("Encryption failed.", e);
        }
    }

    /**
     * <p>Decrypts the given encrypted text with the encryption key supplied.
     * </p>
     * @param encryptedText text to be decrypted
     * @param encryptionKey encryption key which must resolve to the same
     *        value to encrypt and decrypt the supplied text.
     * @return decrypted text or <code>null</code> if one of
     * <code>encryptedText</code> or <code>key</code> is <code>null</code>.
     */
    public static String decrypt(
            String encryptedText, EncryptionKey encryptionKey) {
        // 8-byte Salt
        byte[] salt = {
            (byte)0xE3, (byte)0x03, (byte)0x9B, (byte)0xA9,
            (byte)0xC8, (byte)0x16, (byte)0x35, (byte)0x56
        };
        if (encryptedText == null) {
            return null;
        }
        if (encryptionKey == null) {
            return encryptedText;
        }
        String key = encryptionKey.resolve();
        if (key == null) {
            return encryptedText;
        }

        // Iteration count
        int iterationCount = 1000;
        int keySize = 128;
        Cipher dcipher;

        try {
            // Separate the encrypted data into the salt and the encrypted message
            byte[] cryptMessage = DatatypeConverter.parseBase64Binary(encryptedText.trim());
            byte[] iv = Arrays.copyOf(cryptMessage, 16);
            byte[] cryptBytes = Arrays.copyOfRange(cryptMessage, 16, cryptMessage.length);

            // Create the key
            KeySpec keySpec = new PBEKeySpec(
                    key.trim().toCharArray(), salt, iterationCount, keySize);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            SecretKey secretKeyTemp = factory.generateSecret(keySpec);
            SecretKey secretKey = new SecretKeySpec(secretKeyTemp.getEncoded(), "AES");

            IvParameterSpec ivParamSpec = new IvParameterSpec(iv);

            dcipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            dcipher.init(Cipher.DECRYPT_MODE, secretKey, ivParamSpec);

            byte[] utf8 = dcipher.doFinal(cryptBytes);
            return new String(utf8, StandardCharsets.UTF_8);
        } catch (Exception original) {
            try {
                return decryptLegacy(encryptedText, key);
            } catch (GeneralSecurityException subsequent) {
                throw new EncryptionException("Decryption failed.", original);
            }
        }
    }

    private static String decryptLegacy(String encryptedText, String key) throws GeneralSecurityException {
        // 8-byte Salt
        byte[] salt = {
            (byte)0xE3, (byte)0x03, (byte)0x9B, (byte)0xA9,
            (byte)0xC8, (byte)0x16, (byte)0x35, (byte)0x56
        };
        // Iteration count
        int iterationCount = 19;
        Cipher dcipher;

        // Create the key
        KeySpec keySpec = new PBEKeySpec(
                key.trim().toCharArray(), salt, iterationCount);
        SecretKey secretKey = SecretKeyFactory.getInstance(
            "PBEWithMD5AndDES").generateSecret(keySpec);
        dcipher = Cipher.getInstance(secretKey.getAlgorithm());

        // Prepare the parameter to the ciphers
        AlgorithmParameterSpec paramSpec =
                new PBEParameterSpec(salt, iterationCount);

        // Create the ciphers
        dcipher.init(Cipher.DECRYPT_MODE, secretKey, paramSpec);

        byte[] dec =
                DatatypeConverter.parseBase64Binary(encryptedText.trim());
        byte[] utf8 = dcipher.doFinal(dec);
        return new String(utf8, StandardCharsets.UTF_8);
    }

}
