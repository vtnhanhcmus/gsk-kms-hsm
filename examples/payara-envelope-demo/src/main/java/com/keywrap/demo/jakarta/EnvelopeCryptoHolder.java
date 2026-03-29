package com.keywrap.demo.jakarta;

import com.keywrap.crypto.EnvelopeCryptoService;
import jakarta.enterprise.context.ApplicationScoped;
import java.security.GeneralSecurityException;

/**
 * CDI bean holding a lazy singleton {@link EnvelopeCryptoService}. Mode → implementation mapping lives in
 * {@link EnvelopeCryptoServiceFactory}.
 */
@ApplicationScoped
public class EnvelopeCryptoHolder {

  private volatile EnvelopeCryptoService crypto;

  public EnvelopeCryptoService get() throws GeneralSecurityException {
    EnvelopeCryptoService ref = crypto;
    if (ref == null) {
      synchronized (this) {
        ref = crypto;
        if (ref == null) {
          ref = EnvelopeCryptoServiceFactory.create(EnvelopeRuntimeConfig.mode());
          ref.ensureDekOnGcs();
          crypto = ref;
        }
      }
    }
    return ref;
  }
}
