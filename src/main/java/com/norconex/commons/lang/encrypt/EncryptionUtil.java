/* Copyright 2015-2023 Norconex Inc.
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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.norconex.commons.lang.encrypt.EncryptionKey.Source;
import com.norconex.commons.lang.security.Credentials;

/**
 * <p>Simplified encryption and decryption methods using the
 * <a href="https://en.wikipedia.org/wiki/Advanced_Encryption_Standard">
 * Advanced Encryption Standard (AES)</a> (since 1.15.0) with a supplied
 * encryption key (which you can also think of as a passphrase, or password).
 * </p>
 * <p>
 * The "salt" and iteration count used by this class are hard-coded. To use
 * a different encryption or have more control over its creation,
 * you should rely on another implementation or create your own.
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
 * @since 1.9.0
 */
public class EncryptionUtil {

    private static final int ITER_CNT = 65536;
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;
    private static final int SALT_LENGTH_BYTE = 16;
    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
    private static final String SECRET_ALGO = "PBKDF2WithHmacSHA256";

    private EncryptionUtil() {
    }

    public static void main(String[] args) {
        if (args.length != 4) {
            printUsage();
            return;
        }
        var cmdArg = args[0];
        var typeArg = args[1];
        var keyArg = args[2];
        var textArg = args[3];

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
            System.err.println("Unsupported type of key: " + type); //NOSONAR
            printUsage();
            return;
        }

        var key = new EncryptionKey(keyArg, type);
        if ("encrypt".equalsIgnoreCase(cmdArg)) {
            System.out.println(encrypt(textArg, key)); //NOSONAR
        } else if ("decrypt".equalsIgnoreCase(cmdArg)) {
            System.out.println(decrypt(textArg, key)); //NOSONAR
        } else {
            System.err.println("Unsupported command: " + cmdArg); //NOSONAR
            printUsage();
        }
    }

    private static void printUsage() {
        var out = System.out; //NOSONAR
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
     */
    public static String encrypt(
            String textToEncrypt, EncryptionKey encryptionKey) {
        if (textToEncrypt == null) {
            return null;
        }
        if (encryptionKey == null) {
            return textToEncrypt;
        }
        var encKey = encryptionKey.resolve();
        if (encKey == null) {
            return textToEncrypt;
        }

        try {
            // 16 bytes salt
            var salt = new byte[SALT_LENGTH_BYTE];
            new SecureRandom().nextBytes(salt);

            // GCM recommended 12 bytes iv?
            var iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(salt);

            // secret key from password
            var factory = SecretKeyFactory.getInstance(SECRET_ALGO);
            // iterationCount = 65536
            // keyLength = 256
            KeySpec spec = new PBEKeySpec(encKey.toCharArray(),
                    salt, ITER_CNT, encryptionKey.getSize());
            SecretKey secretKey = new SecretKeySpec(
                    factory.generateSecret(spec).getEncoded(), "AES");

            var cipher = Cipher.getInstance(CIPHER_ALGO);

            // ASE-GCM needs GCMParameterSpec
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            var cipherText = cipher.doFinal(textToEncrypt.getBytes(UTF_8));

            // prefix IV and Salt to cipher text
            var cipherTextWithIvSalt = ByteBuffer.allocate(
                    iv.length + salt.length + cipherText.length)
                    .put(iv)
                    .put(salt)
                    .put(cipherText)
                    .array();

            // string representation, base64, send this string to other
            // for decryption.
            return Base64.getEncoder().encodeToString(cipherTextWithIvSalt);
        } catch (Exception e) {
            throw new EncryptionException("Encryption failed.", e);
        }
    }

    /**
     * <p>Decrypts the given credentials password.</p>
     * @param credentials credentials from which to decrypt the password
     * @return decrypted password.
     */
    public static String decryptPassword(Credentials credentials) {
        if (credentials == null) {
            return null;
        }
        return decrypt(credentials.getPassword(), credentials.getPasswordKey());
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
        if (encryptedText == null) {
            return null;
        }
        if (encryptionKey == null) {
            return encryptedText;
        }
        var encKey = encryptionKey.resolve();
        if (encKey == null) {
            return encryptedText;
        }

        try {
            var decode = Base64.getDecoder().decode(
                    encryptedText.getBytes(UTF_8));

            // get back the iv and salt from the cipher text
            var bb = ByteBuffer.wrap(decode);

            var iv = new byte[IV_LENGTH_BYTE];
            bb.get(iv);

            var salt = new byte[SALT_LENGTH_BYTE];
            bb.get(salt);

            var cipherText = new byte[bb.remaining()];
            bb.get(cipherText);

            // get back the aes key from the same password and salt
            var factory = SecretKeyFactory.getInstance(SECRET_ALGO);
            // iterationCount = 65536
            // keyLength = 256
            KeySpec spec = new PBEKeySpec(encKey.toCharArray(),
                    salt, ITER_CNT, encryptionKey.getSize());
            SecretKey secretKey = new SecretKeySpec(
                    factory.generateSecret(spec).getEncoded(), "AES");
            var cipher = Cipher.getInstance(CIPHER_ALGO);

            cipher.init(Cipher.DECRYPT_MODE, secretKey,
                    new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            var plainText = cipher.doFinal(cipherText);

            return new String(plainText, UTF_8);

        } catch (Exception original) {
            throw new EncryptionException("Decryption failed.", original);
        }
    }
}
