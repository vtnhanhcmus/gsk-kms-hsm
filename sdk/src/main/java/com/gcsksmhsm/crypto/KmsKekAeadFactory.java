package com.gcsksmhsm.crypto;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.integration.gcpkms.GcpKmsClient;
import java.security.GeneralSecurityException;

/** Tạo primitive AEAD cho KEK trên Cloud KMS (dùng bọc/giải bọc DEK trong Tink). */
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
