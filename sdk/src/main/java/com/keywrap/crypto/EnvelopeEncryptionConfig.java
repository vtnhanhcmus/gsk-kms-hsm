package com.keywrap.crypto;

import java.util.Objects;

/**
 * Envelope encryption settings: DEK (Tink keyset) on GCS, wrapped by a KEK in Cloud KMS.
 *
 * <p>HSM: create the CryptoKey in KMS with {@code protection_level = HSM} so the master key stays in
 * Google’s HSM; the app still calls KMS over the API like a normal key.
 */
public final class EnvelopeEncryptionConfig {

  private final String gcsBucket;
  private final String gcsObjectName;
  /** URI like {@code gcp-kms://projects/.../locations/.../keyRings/.../cryptoKeys/.../cryptoKeyVersions/...} */
  private final String kmsKekUri;
  /** Path to service account JSON; null = Application Default Credentials */
  private final String gcpCredentialsPath;

  public EnvelopeEncryptionConfig(
      String gcsBucket, String gcsObjectName, String kmsKekUri, String gcpCredentialsPath) {
    this.gcsBucket = Objects.requireNonNull(gcsBucket, "gcsBucket");
    this.gcsObjectName = Objects.requireNonNull(gcsObjectName, "gcsObjectName");
    this.kmsKekUri = Objects.requireNonNull(kmsKekUri, "kmsKekUri");
    this.gcpCredentialsPath = gcpCredentialsPath;
  }

  public String gcsBucket() {
    return gcsBucket;
  }

  public String gcsObjectName() {
    return gcsObjectName;
  }

  public String kmsKekUri() {
    return kmsKekUri;
  }

  /** Nullable: if null, use ADC (GOOGLE_APPLICATION_CREDENTIALS, gcloud, etc.). */
  public String gcpCredentialsPath() {
    return gcpCredentialsPath;
  }
}
