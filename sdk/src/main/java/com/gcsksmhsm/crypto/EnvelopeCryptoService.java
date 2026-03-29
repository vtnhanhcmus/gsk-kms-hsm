package com.gcsksmhsm.crypto;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.TinkJsonProtoKeysetFormat;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.PredefinedAeadParameters;
import java.security.GeneralSecurityException;
import java.util.Objects;

/**
 * Envelope encryption: DEK là keyset Tink (AES-GCM), file JSON đã mã hóa nằm trên GCS; KEK là khóa
 * Cloud KMS (nên dùng HSM protection level khi tạo khóa để khớp mô hình KMS → HSM).
 *
 * <p>Để test không có GCP: dùng {@link EnvelopeCryptoService#EnvelopeCryptoService(EncryptedKeysetStore,
 * Aead)} với {@link InMemoryEncryptedKeysetStore} và KEK cục bộ, hoặc {@link LocalEnvelopeSimulation}.
 */
public final class EnvelopeCryptoService {

  private static final byte[] EMPTY_AAD = new byte[0];

  private final EncryptedKeysetStore store;
  /** Nếu khác null: dùng KEK cố định (mô phỏng / inject). Nếu null: lấy KEK từ Cloud KMS qua config. */
  private final Aead fixedKekAead;

  private final EnvelopeEncryptionConfig config;

  public EnvelopeCryptoService(EnvelopeEncryptionConfig config) {
    this.config = Objects.requireNonNull(config, "config");
    this.store = new GcsEncryptedKeysetStore(config.gcsBucket(), config.gcsObjectName());
    this.fixedKekAead = null;
  }

  /**
   * @param store kho keyset (GCS, in-memory, …)
   * @param kekAead AEAD bọc DEK — thường là {@link com.google.crypto.tink.integration.gcpkms.GcpKmsAead}
   *     hoặc primitive từ keyset Tink cục bộ khi giả lập
   */
  public EnvelopeCryptoService(EncryptedKeysetStore store, Aead kekAead) {
    this.store = Objects.requireNonNull(store, "store");
    this.fixedKekAead = Objects.requireNonNull(kekAead, "kekAead");
    this.config = null;
  }

  /** Cho test: inject cả store GCS (mock) và giữ nhánh KMS qua config. */
  EnvelopeCryptoService(EnvelopeEncryptionConfig config, EncryptedKeysetStore store) {
    this.config = Objects.requireNonNull(config, "config");
    this.store = Objects.requireNonNull(store, "store");
    this.fixedKekAead = null;
  }

  static {
    try {
      AeadConfig.register();
    } catch (GeneralSecurityException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private Aead kekAead() throws GeneralSecurityException {
    if (fixedKekAead != null) {
      return fixedKekAead;
    }
    if (config == null) {
      throw new IllegalStateException("Thiếu config KMS");
    }
    return KmsKekAeadFactory.create(config.kmsKekUri(), config.gcpCredentialsPath());
  }

  /**
   * Đảm bảo file DEK trên kho tồn tại: nếu chưa có thì tạo keyset DEK mới, bọc bằng KEK, ghi vào store.
   */
  public void ensureDekOnGcs() throws GeneralSecurityException {
    if (store.exists()) {
      return;
    }
    Aead kek = kekAead();
    KeysetHandle handle = KeysetHandle.generateNew(PredefinedAeadParameters.AES128_GCM);
    String encryptedJson =
        TinkJsonProtoKeysetFormat.serializeEncryptedKeyset(handle, kek, EMPTY_AAD);
    store.writeEncryptedKeysetJson(encryptedJson);
  }

  public byte[] encrypt(byte[] plaintext) throws GeneralSecurityException {
    Objects.requireNonNull(plaintext, "plaintext");
    Aead kek = kekAead();
    KeysetHandle handle =
        TinkJsonProtoKeysetFormat.parseEncryptedKeyset(
            store.readEncryptedKeysetJson(), kek, EMPTY_AAD);
    Aead aead = handle.getPrimitive(Aead.class);
    return aead.encrypt(plaintext, EMPTY_AAD);
  }

  public byte[] decrypt(byte[] ciphertext) throws GeneralSecurityException {
    Objects.requireNonNull(ciphertext, "ciphertext");
    Aead kek = kekAead();
    KeysetHandle handle =
        TinkJsonProtoKeysetFormat.parseEncryptedKeyset(
            store.readEncryptedKeysetJson(), kek, EMPTY_AAD);
    Aead aead = handle.getPrimitive(Aead.class);
    return aead.decrypt(ciphertext, EMPTY_AAD);
  }
}
