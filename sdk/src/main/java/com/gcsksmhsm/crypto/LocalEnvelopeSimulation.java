package com.gcsksmhsm.crypto;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.PredefinedAeadParameters;
import java.security.GeneralSecurityException;

/**
 * Giả lập stack DEK + KEK + “GCS” không cần tài khoản GCP: KEK là keyset Tink AES-GCM cục bộ, kho là
 * {@link InMemoryEncryptedKeysetStore}. Luồng mã hóa giống production (envelope), chỉ thay KMS/GCS bằng
 * primitive Tink + RAM.
 */
public final class LocalEnvelopeSimulation {

  static {
    try {
      AeadConfig.register();
    } catch (GeneralSecurityException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private LocalEnvelopeSimulation() {}

  /**
   * Tạo KEK cục bộ (AES-128-GCM) và service dùng kho in-memory.
   *
   * @param store có thể dùng chung nhiều lần nếu giữ cùng tham chiếu KEK; mỗi lần gọi factory này với
   *     store mới sẽ tách hoàn toàn trạng thái.
   */
  public static EnvelopeCryptoService newService(InMemoryEncryptedKeysetStore store)
      throws GeneralSecurityException {
    KeysetHandle kekHandle = KeysetHandle.generateNew(PredefinedAeadParameters.AES128_GCM);
    Aead kekAead = kekHandle.getPrimitive(Aead.class);
    return new EnvelopeCryptoService(store, kekAead);
  }

  /** Kho in-memory mới + service tương ứng. */
  public static EnvelopeCryptoService newService() throws GeneralSecurityException {
    return newService(new InMemoryEncryptedKeysetStore());
  }
}
