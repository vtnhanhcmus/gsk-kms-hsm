package com.keywrap.demo.jakarta;

import com.keywrap.crypto.EnvelopeCryptoService;
import com.keywrap.crypto.LocalEnvelopeSimulation;
import java.security.GeneralSecurityException;

/**
 * Builds {@link EnvelopeCryptoService} from {@link EnvelopeRuntimeConfig.Mode} — single place for
 * mode → implementation mapping.
 */
public final class EnvelopeCryptoServiceFactory {

  private EnvelopeCryptoServiceFactory() {}

  /**
   * @param mode current value from {@link EnvelopeRuntimeConfig#mode()}
   */
  public static EnvelopeCryptoService create(EnvelopeRuntimeConfig.Mode mode)
      throws GeneralSecurityException {
    switch (mode) {
      case LOCAL:
        // No GCS/KMS — keys in RAM (Tink simulation)
        return LocalEnvelopeSimulation.newService();

      case STAGING:
      case PROD:
        // Same GCS + KMS stack; staging vs prod differ via env (GCS_DEK_URI, KMS_KEK_URI) per deployment
        return new EnvelopeCryptoService(EnvelopeRuntimeConfig.buildCloudConfig());

      default:
        throw new IllegalStateException("Unhandled mode: " + mode);
    }
  }
}
