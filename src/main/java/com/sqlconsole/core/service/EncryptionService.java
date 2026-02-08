package com.sqlconsole.core.service;

import com.sqlconsole.core.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
/**
 * Service for encryption and decryption operations.
 */
public class EncryptionService {

  @Value("${app.security.master-key}")
  private String masterKey;

  /**
   * Encrypts the given plain text.
   *
   * @param plainText the text to encrypt
   * @return the encrypted text, or null/empty if input is null/empty
   */
  public String encrypt(String plainText) {
    if (plainText == null || plainText.isEmpty()) {
      return plainText;
    }
    try {
      return EncryptionUtil.encrypt(plainText, masterKey);
    } catch (Exception e) {
      throw new RuntimeException("Encryption failed", e);
    }
  }

  /**
   * Decrypts the given encrypted text.
   *
   * @param encryptedText the text to decrypt
   * @return the decrypted text, or null/empty if input is null/empty
   */
  public String decrypt(String encryptedText) {
    if (encryptedText == null || encryptedText.isEmpty()) {
      return encryptedText;
    }
    try {
      return EncryptionUtil.decrypt(encryptedText, masterKey);
    } catch (Exception e) {
      throw new RuntimeException("Decryption failed", e);
    }
  }
}
