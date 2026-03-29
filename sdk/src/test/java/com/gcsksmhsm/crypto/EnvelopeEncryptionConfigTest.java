package com.gcsksmhsm.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class EnvelopeEncryptionConfigTest {

  @Test
  void holdsValues() {
    EnvelopeEncryptionConfig c =
        new EnvelopeEncryptionConfig("b", "dek.json", "gcp-kms://projects/p/locations/l/keyRings/r/cryptoKeys/k/cryptoKeyVersions/1", null);
    assertEquals("b", c.gcsBucket());
    assertEquals("dek.json", c.gcsObjectName());
    assertEquals("gcp-kms://projects/p/locations/l/keyRings/r/cryptoKeys/k/cryptoKeyVersions/1", c.kmsKekUri());
    assertNull(c.gcpCredentialsPath());
  }
}
