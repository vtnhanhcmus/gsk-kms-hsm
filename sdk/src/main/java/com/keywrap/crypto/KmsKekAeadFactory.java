package com.keywrap.crypto;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.kms.v1.DecryptRequest;
import com.google.cloud.kms.v1.DecryptResponse;
import com.google.cloud.kms.v1.EncryptRequest;
import com.google.cloud.kms.v1.EncryptResponse;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.cloud.kms.v1.KeyManagementServiceSettings;
import com.google.crypto.tink.Aead;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;

/** Builds the KEK AEAD primitive for Cloud KMS (wrap/unwrap the DEK in Tink). */
public final class KmsKekAeadFactory {

  private KmsKekAeadFactory() {}

  public static Aead create(String kmsKekUri, String gcpCredentialsPath)
      throws GeneralSecurityException {
    final String keyName = normalizeKmsResourceName(kmsKekUri);
    final KeyManagementServiceClient client = createClient(gcpCredentialsPath);
    return new Aead() {
      @Override
      public byte[] encrypt(byte[] plaintext, byte[] associatedData) throws GeneralSecurityException {
        try {
          EncryptResponse response =
              client.encrypt(
                  EncryptRequest.newBuilder()
                      .setName(keyName)
                      .setPlaintext(ByteString.copyFrom(plaintext))
                      .setAdditionalAuthenticatedData(
                          ByteString.copyFrom(associatedData == null ? new byte[0] : associatedData))
                      .build());
          return response.getCiphertext().toByteArray();
        } catch (Exception e) {
          throw new GeneralSecurityException("KMS encrypt failed", e);
        }
      }

      @Override
      public byte[] decrypt(byte[] ciphertext, byte[] associatedData) throws GeneralSecurityException {
        try {
          DecryptResponse response =
              client.decrypt(
                  DecryptRequest.newBuilder()
                      .setName(keyName)
                      .setCiphertext(ByteString.copyFrom(ciphertext))
                      .setAdditionalAuthenticatedData(
                          ByteString.copyFrom(associatedData == null ? new byte[0] : associatedData))
                      .build());
          return response.getPlaintext().toByteArray();
        } catch (Exception e) {
          throw new GeneralSecurityException("KMS decrypt failed", e);
        }
      }
    };
  }

  private static KeyManagementServiceClient createClient(String gcpCredentialsPath)
      throws GeneralSecurityException {
    try {
      if (gcpCredentialsPath == null || gcpCredentialsPath.trim().isEmpty()) {
        return KeyManagementServiceClient.create();
      }
      try (InputStream in = Files.newInputStream(Paths.get(gcpCredentialsPath))) {
        GoogleCredentials credentials = GoogleCredentials.fromStream(in);
        KeyManagementServiceSettings settings =
            KeyManagementServiceSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();
        return KeyManagementServiceClient.create(settings);
      }
    } catch (IOException e) {
      throw new GeneralSecurityException("Failed to create Cloud KMS client", e);
    }
  }

  private static String normalizeKmsResourceName(String kmsKekUri) {
    if (kmsKekUri == null) {
      throw new IllegalArgumentException("kmsKekUri must not be null");
    }
    String resource = kmsKekUri;
    if (resource.startsWith("gcp-kms://")) {
      resource = resource.substring("gcp-kms://".length());
    }
    int versionIdx = resource.indexOf("/cryptoKeyVersions/");
    if (versionIdx >= 0) {
      resource = resource.substring(0, versionIdx);
    }
    return resource;
  }
}
