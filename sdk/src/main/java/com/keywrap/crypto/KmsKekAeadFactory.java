package com.keywrap.crypto;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.integration.gcpkms.GcpKmsClient;
import java.security.GeneralSecurityException;

/** Builds the KEK AEAD primitive for Cloud KMS (wrap/unwrap the DEK in Tink). */
public final class KmsKekAeadFactory {

  private KmsKekAeadFactory() {}

  public static Aead create(String kmsKekUri, String gcpCredentialsPath)
      throws GeneralSecurityException {
    GcpKmsClient client = new GcpKmsClient();
    if (gcpCredentialsPath != null && !gcpCredentialsPath.isEmpty()) {
      client.withCredentials(gcpCredentialsPath);
    } else {
      client.withDefaultCredentials();
    }
    return client.getAead(kmsKekUri);
  }
}
