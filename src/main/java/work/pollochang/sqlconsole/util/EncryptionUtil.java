package work.pollochang.sqlconsole.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;

/** 安全加密工具類別 - 採用 AES-256-GCM 演算法 支援 Salt 機制與 PBKDF2 金鑰衍生 */
@Slf4j
public class EncryptionUtil {

  private static final String ENCRYPTION_ALGO = "AES/GCM/NoPadding";
  private static final int TAG_LENGTH_BIT = 128;
  private static final int IV_LENGTH_BYTE = 12;
  private static final int SALT_LENGTH_BYTE = 16;
  private static final int ITERATION_COUNT = 65536;
  private static final int KEY_LENGTH_BIT = 256;

  /**
   * 加密字串
   *
   * @param plainText 明文
   * @param masterPassword 系統主密鑰 (建議從環境變數讀取)
   * @return Base64 編碼的加密字串 (包含 Salt + IV + CipherText)
   */
  public static String encrypt(String plainText, String masterPassword) throws Exception {
    // 1. 生成隨機 Salt
    byte[] salt = new byte[SALT_LENGTH_BYTE];
    new SecureRandom().nextBytes(salt);

    // 2. 衍生金鑰
    SecretKey secretKey = deriveKey(masterPassword, salt);

    // 3. 生成隨機 IV
    byte[] iv = new byte[IV_LENGTH_BYTE];
    new SecureRandom().nextBytes(iv);

    // 4. 執行加密
    Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGO);
    GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
    byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

    // 5. 封裝結果: [Salt(16)] + [IV(12)] + [CipherText(n)]
    ByteBuffer byteBuffer = ByteBuffer.allocate(salt.length + iv.length + cipherText.length);
    byteBuffer.put(salt);
    byteBuffer.put(iv);
    byteBuffer.put(cipherText);

    return Base64.getEncoder().encodeToString(byteBuffer.array());
  }

  /**
   * 解密字串
   *
   * @param encryptedBase64 加密後的 Base64 字串
   * @param masterPassword 系統主密鑰
   * @return 解密後的明文
   */
  public static String decrypt(String encryptedBase64, String masterPassword) throws Exception {
    byte[] decode = Base64.getDecoder().decode(encryptedBase64);
    ByteBuffer byteBuffer = ByteBuffer.wrap(decode);

    // 1. 提取 Salt
    byte[] salt = new byte[SALT_LENGTH_BYTE];
    byteBuffer.get(salt);

    // 2. 提取 IV
    byte[] iv = new byte[IV_LENGTH_BYTE];
    byteBuffer.get(iv);

    // 3. 提取密文
    byte[] cipherText = new byte[byteBuffer.remaining()];
    byteBuffer.get(cipherText);

    // 4. 衍生金鑰
    SecretKey secretKey = deriveKey(masterPassword, salt);

    // 5. 執行解密
    Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGO);
    GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
    cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
    byte[] plainText = cipher.doFinal(cipherText);

    return new String(plainText, StandardCharsets.UTF_8);
  }

  /** 使用 PBKDF2 與 Salt 衍生高強度金鑰 */
  private static SecretKey deriveKey(String password, byte[] salt)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BIT);
    return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
  }
}
