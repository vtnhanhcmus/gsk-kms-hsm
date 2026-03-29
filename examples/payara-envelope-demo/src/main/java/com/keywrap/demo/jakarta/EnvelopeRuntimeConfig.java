package com.keywrap.demo.jakarta;

import com.keywrap.crypto.EnvelopeEncryptionConfig;

/**
 * Three modes: {@link Mode#LOCAL}, {@link Mode#STAGING}, {@link Mode#PROD}. Only two variables for cloud:
 * {@code GCS_DEK_URI} (gs://…) and {@code KMS_KEK_URI}. Credentials: Application Default Credentials (e.g.
 * {@code GOOGLE_APPLICATION_CREDENTIALS}).
 */
public final class EnvelopeRuntimeConfig {

  public static final String ENV_ENVELOPE_MODE = "ENVELOPE_MODE";

  /** Example: {@code gs://my-bucket/path/dek.encrypted.json} */
  public static final String ENV_GCS_DEK_URI = "GCS_DEK_URI";

  public static final String ENV_KMS_KEK_URI = "KMS_KEK_URI";

  private EnvelopeRuntimeConfig() {}

  public enum Mode {
    LOCAL,
    STAGING,
    PROD
  }

  public static Mode mode() {
    String raw =
        firstNonBlank(
            System.getenv(ENV_ENVELOPE_MODE), System.getProperty("envelope.mode"));
    raw = firstNonBlank(raw, System.getProperty(toPropertyName(ENV_ENVELOPE_MODE)));
    if (raw == null) {
      raw = "local";
    }
    raw = raw.trim();
    if ("staging".equalsIgnoreCase(raw)) {
      return Mode.STAGING;
    }
    if ("prod".equalsIgnoreCase(raw)) {
      return Mode.PROD;
    }
    return Mode.LOCAL;
  }

  /** Use when branching (logs, metrics): staging/prod use GCS+KMS; local does not. */
  public static boolean usesGoogleCloud(Mode m) {
    return m == Mode.STAGING || m == Mode.PROD;
  }

  public static EnvelopeEncryptionConfig buildCloudConfig() {
    String gs = requireEnv(ENV_GCS_DEK_URI);
    String kmsUri = requireEnv(ENV_KMS_KEK_URI);
    String[] bucketAndObject = parseGsUri(gs);
    return new EnvelopeEncryptionConfig(
        bucketAndObject[0], bucketAndObject[1], kmsUri, null);
  }

  /** gs://bucket/object/key — object path may contain slashes */
  static String[] parseGsUri(String gs) {
    if (gs == null || !gs.startsWith("gs://")) {
      throw new IllegalStateException(
          ENV_GCS_DEK_URI + " must be gs://bucket/object (e.g. gs://b/crypto/dek.json)");
    }
    String rest = gs.substring("gs://".length());
    int slash = rest.indexOf('/');
    if (slash <= 0 || slash >= rest.length() - 1) {
      throw new IllegalStateException(
          ENV_GCS_DEK_URI + " must include bucket and object: gs://bucket/path/to/file");
    }
    String bucket = rest.substring(0, slash);
    String object = rest.substring(slash + 1);
    return new String[] {bucket, object};
  }

  private static String requireEnv(String name) {
    String v =
        firstNonBlank(System.getenv(name), System.getProperty(toPropertyName(name)));
    if (v == null) {
      throw new IllegalStateException(
          "Missing " + name + " when ENVELOPE_MODE=staging|prod");
    }
    return v.trim();
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
}
