package com.gcsksmhsm.demo.jakarta;

import com.gcsksmhsm.crypto.EnvelopeCryptoService;
import com.gcsksmhsm.crypto.LocalEnvelopeSimulation;
import jakarta.enterprise.context.ApplicationScoped;
import java.security.GeneralSecurityException;

/**
 * Giữ {@link EnvelopeCryptoService} (giả lập). Khởi tạo lười (lazy) để CDI không fail ở startup nếu Tink
 * lỗi — tránh cả bean {@code CryptoResource} không deploy (404 mọi endpoint).
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
          local = LocalEnvelopeSimulation.newService();
          local.ensureDekOnGcs();
          crypto = local;
        }
      }
    }
    return local;
  }
}
