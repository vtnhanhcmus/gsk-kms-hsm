package com.keywrap.crypto;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.TinkJsonProtoKeysetFormat;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.PredefinedAeadParameters;
import java.security.GeneralSecurityException;
import java.util.Objects;

/**
 * Envelope encryption: DEK is a Tink keyset (AES-GCM); encrypted JSON lives on GCS; KEK is a Cloud KMS key
 * (use HSM protection level when creating the key to align with the KMS → HSM model).
 *
 * <p>For tests without GCP: use {@link EnvelopeCryptoService#EnvelopeCryptoService(EncryptedKeysetStore,
 * Aead)} with {@link InMemoryEncryptedKeysetStore} and a local KEK, or {@link LocalEnvelopeSimulation}.
 */
public final class EnvelopeCryptoService {

  private static final byte[] EMPTY_AAD = new byte[0];

  private final EncryptedKeysetStore store;
  /** If non-null: fixed KEK (simulation / injection). If null: KEK from Cloud KMS via config. */
  private final Aead fixedKekAead;

  private final EnvelopeEncryptionConfig config;

  public EnvelopeCryptoService(EnvelopeEncryptionConfig config) {
    this.config = Objects.requireNonNull(config, "config");
    this.store =
        new GcsEncryptedKeysetStore(
            config.gcsBucket(), config.gcsObjectName(), config.gcpCredentialsPath());
    this.fixedKekAead = null;
  }

  /**
   * @param store keyset backing store (GCS, in-memory, …)
   * @param kekAead AEAD wrapping the DEK — usually {@link com.google.crypto.tink.integration.gcpkms.GcpKmsAead}
   *     or a primitive from a local Tink keyset when simulating
   */
  public EnvelopeCryptoService(EncryptedKeysetStore store, Aead kekAead) {
    this.store = Objects.requireNonNull(store, "store");
    this.fixedKekAead = Objects.requireNonNull(kekAead, "kekAead");
    this.config = null;
  }

  /** For tests: inject a GCS store (e.g. mock) while keeping the KMS path via config. */
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
      throw new IllegalStateException("Missing KMS config");
    }
    return KmsKekAeadFactory.create(config.kmsKekUri(), config.gcpCredentialsPath());
  }

  /**
   * On <strong>GCS</strong>: if the DEK object is missing, throws {@link IllegalStateException} — the
   * encrypted keyset must be provisioned beforehand (no auto-create).
   *
   * <p>On other stores (e.g. {@link InMemoryEncryptedKeysetStore} for local simulation), if empty,
   * generates a DEK, wraps with the KEK, and writes.
   */
  public void ensureDekOnGcs() throws GeneralSecurityException {
    if (store.exists()) {
      return;
    }
    if (store instanceof GcsEncryptedKeysetStore) {
      GcsEncryptedKeysetStore gcs = (GcsEncryptedKeysetStore) store;
      throw new IllegalStateException(
          "DEK not found in GCS at "
              + gcs.gsUri()
              + "; upload the encrypted keyset first (bootstrap is not performed for GCS).");
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
