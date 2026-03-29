package com.gcsksmhsm.demo.jakarta;

import com.gcsksmhsm.crypto.EnvelopeEncryptionConfig;

/**
 * Cấu hình theo môi trường (ưu tiên biến môi trường, sau đó system property).
 *
 * <p><b>Local (mặc định):</b> không cần GCP — dùng {@code LocalEnvelopeSimulation}.
 *
 * <p><b>Production / GCP:</b> đặt {@code ENVELOPE_MODE=gcp} và bucket, object DEK trên GCS, URI KEK KMS.
 * Credential: để trống {@code GCP_CREDENTIALS_PATH} và dùng Application Default Credentials (biến {@code
 * GOOGLE_APPLICATION_CREDENTIALS}, metadata GCE/GKE, hoặc Workload Identity).
 */
public final class EnvelopeRuntimeConfig {

  /** {@code local} | {@code gcp} */
  public static final String ENV_ENVELOPE_MODE = "ENVELOPE_MODE";

  public static final String ENV_GCS_BUCKET = "GCS_BUCKET";
  public static final String ENV_GCS_OBJECT_NAME = "GCS_OBJECT_NAME";
  public static final String ENV_KMS_KEK_URI = "KMS_KEK_URI";

  /**
   * Đường dẫn file JSON service account (tuỳ chọn). Nếu không set, SDK dùng ADC (khuyến nghị production:
   * mount secret + {@code GOOGLE_APPLICATION_CREDENTIALS}).
   */
  public static final String ENV_GCP_CREDENTIALS_PATH = "GCP_CREDENTIALS_PATH";

  private EnvelopeRuntimeConfig() {}

  public enum Mode {
    LOCAL,
    GCP
  }

  public static Mode mode() {
    String raw =
        firstNonBlank(
            System.getenv(ENV_ENVELOPE_MODE),
            System.getProperty("envelope.mode"),
            System.getProperty(toPropertyName(ENV_ENVELOPE_MODE)));
    if (raw == null) {
      raw = "local";
    }
    raw = raw.trim();
    if ("gcp".equalsIgnoreCase(raw)) {
      return Mode.GCP;
    }
    return Mode.LOCAL;
  }

  /** Cấu hình GCP; chỉ gọi khi {@link #mode()} là {@link Mode#GCP}. */
  public static EnvelopeEncryptionConfig buildGcpConfig() {
    String bucket = requireEnv(ENV_GCS_BUCKET);
    String object = requireEnv(ENV_GCS_OBJECT_NAME);
    String kmsUri = requireEnv(ENV_KMS_KEK_URI);
    String credPath = optionalEnv(ENV_GCP_CREDENTIALS_PATH);
    return new EnvelopeEncryptionConfig(bucket, object, kmsUri, credPath);
  }

  private static String requireEnv(String name) {
    String v =
        firstNonBlank(System.getenv(name), System.getProperty(toPropertyName(name)));
    if (v == null) {
      throw new IllegalStateException(
          "Thiếu biến môi trường bắt buộc cho ENVELOPE_MODE=gcp: " + name);
    }
    return v.trim();
  }

  private static String optionalEnv(String name) {
    return firstNonBlank(System.getenv(name), System.getProperty(toPropertyName(name)));
  }

  private static String toPropertyName(String envName) {
    return envName.toLowerCase().replace('_', '.');
  }

  private static String firstNonBlank(String a, String b) {
    if (a != null && !a.isBlank()) {
      return a;
    }
    if (b != null && !b.isBlank()) {
      return b;
    }
    return null;
  }

  private static String firstNonBlank(String a, String b, String c) {
    String x = firstNonBlank(a, b);
    return firstNonBlank(x, c);
  }
}
