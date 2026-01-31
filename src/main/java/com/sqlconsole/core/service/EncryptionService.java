package com.sqlconsole.core.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.sqlconsole.core.util.EncryptionUtil;

@Service
public class EncryptionService {

  @Value("${app.security.master-key}")
  private String masterKey;

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
