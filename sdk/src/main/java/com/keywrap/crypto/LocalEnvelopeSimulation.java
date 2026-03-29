package com.keywrap.crypto;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.PredefinedAeadParameters;
import java.security.GeneralSecurityException;

/**
 * Simulates DEK + KEK + “GCS” without a GCP account: KEK is a local Tink AES-GCM keyset, store is {@link
 * InMemoryEncryptedKeysetStore}. Crypto flow matches production (envelope); only KMS/GCS are replaced with
 * Tink primitives + RAM.
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
   * Creates a local KEK (AES-128-GCM) and a service using an in-memory store.
   *
   * @param store may be shared across calls if you keep the same KEK reference; each call to this factory
   *     with a new store is fully isolated.
   */
  public static EnvelopeCryptoService newService(InMemoryEncryptedKeysetStore store)
      throws GeneralSecurityException {
    KeysetHandle kekHandle = KeysetHandle.generateNew(PredefinedAeadParameters.AES128_GCM);
    Aead kekAead = kekHandle.getPrimitive(Aead.class);
    return new EnvelopeCryptoService(store, kekAead);
  }

  /** New in-memory store and matching service. */
  public static EnvelopeCryptoService newService() throws GeneralSecurityException {
    return newService(new InMemoryEncryptedKeysetStore());
  }
}
