package com.keywrap.crypto;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.PredefinedAeadParameters;
import java.security.GeneralSecurityException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LocalEnvelopeSimulationTest {

  @BeforeAll
  static void registerTink() throws GeneralSecurityException {
    AeadConfig.register();
  }

  @Test
  void roundTripWithoutGcp() throws GeneralSecurityException {
    EnvelopeCryptoService crypto = LocalEnvelopeSimulation.newService();
    crypto.ensureDekOnGcs();

    byte[] plain = "secret payload".getBytes(UTF_8);
    byte[] ct = crypto.encrypt(plain);
    byte[] round = crypto.decrypt(ct);
    assertArrayEquals(plain, round);
  }

  @Test
  void twoIsolatedSimulationsDoNotShareKeys() throws GeneralSecurityException {
    EnvelopeCryptoService a = LocalEnvelopeSimulation.newService();
    EnvelopeCryptoService b = LocalEnvelopeSimulation.newService();
    a.ensureDekOnGcs();
    b.ensureDekOnGcs();

    byte[] ct = a.encrypt("x".getBytes(UTF_8));
    org.junit.jupiter.api.Assertions.assertThrows(
        GeneralSecurityException.class, () -> b.decrypt(ct));
  }

  @Test
  void sameStoreAndSameKekAcrossServiceInstances() throws GeneralSecurityException {
    InMemoryEncryptedKeysetStore store = new InMemoryEncryptedKeysetStore();
    KeysetHandle kek = KeysetHandle.generateNew(PredefinedAeadParameters.AES128_GCM);
    Aead kekAead = kek.getPrimitive(Aead.class);
    EnvelopeCryptoService first = new EnvelopeCryptoService(store, kekAead);
    first.ensureDekOnGcs();
    byte[] ct = first.encrypt("shared".getBytes(UTF_8));
    EnvelopeCryptoService second = new EnvelopeCryptoService(store, kekAead);
    assertEquals("shared", new String(second.decrypt(ct), UTF_8));
  }
}
