package com.gcsksmhsm.demo.jakarta;

import com.gcsksmhsm.crypto.EnvelopeCryptoService;
import com.gcsksmhsm.crypto.LocalEnvelopeSimulation;
import jakarta.enterprise.context.ApplicationScoped;
import java.security.GeneralSecurityException;

/**
 * Giữ một {@link EnvelopeCryptoService} theo {@link EnvelopeRuntimeConfig}: local (Tink in-memory) hoặc
 * GCP (GCS + KMS). Khởi tạo lười để CDI không fail ở startup.
 */
@ApplicationScoped
public class EnvelopeCryptoHolder {

  private volatile EnvelopeCryptoService crypto;

  public EnvelopeCryptoService get() throws GeneralSecurityException {
    EnvelopeCryptoService local = crypto;
    if (local == null) {
      synchronized (this) {
        local = crypto;
        if (local == null) {
          local = createService();
          local.ensureDekOnGcs();
          crypto = local;
        }
      }
    }
    return local;
  }

  private static EnvelopeCryptoService createService() throws GeneralSecurityException {
    if (EnvelopeRuntimeConfig.mode() == EnvelopeRuntimeConfig.Mode.GCP) {
      return new EnvelopeCryptoService(EnvelopeRuntimeConfig.buildGcpConfig());
    }
    return LocalEnvelopeSimulation.newService();
  }
}
