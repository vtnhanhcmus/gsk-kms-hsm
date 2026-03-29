package com.gcsksmhsm.crypto;

import java.util.Objects;

/**
 * Cấu hình envelope encryption: DEK (Tink keyset) lưu trên GCS, được bọc bởi KEK trên Cloud KMS.
 *
 * <p>HSM: tạo CryptoKey trên KMS với {@code protection_level = HSM} để master key nằm trên HSM
 * của Google; ứng dụng vẫn gọi KMS qua API như khóa thường.
 */
public final class EnvelopeEncryptionConfig {

  private final String gcsBucket;
  private final String gcsObjectName;
  /** URI dạng {@code gcp-kms://projects/.../locations/.../keyRings/.../cryptoKeys/.../cryptoKeyVersions/...} */
  private final String kmsKekUri;
  /** Đường tới JSON service account; null = Application Default Credentials */
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

  /** Nullable: nếu null thì dùng ADC (GOOGLE_APPLICATION_CREDENTIALS, gcloud, v.v.). */
  public String gcpCredentialsPath() {
    return gcpCredentialsPath;
  }
}
