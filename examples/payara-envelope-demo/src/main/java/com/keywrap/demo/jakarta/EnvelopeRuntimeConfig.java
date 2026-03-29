package com.keywrap.demo.jakarta;

import com.keywrap.crypto.EnvelopeEncryptionConfig;

/**
 * Two modes: {@link Mode#LOCAL} (in-memory simulation) and {@link Mode#GCP} (real GCS + Cloud KMS). For GCP,
 * set {@code GCS_DEK_URI} (gs://…) and {@code KMS_KEK_URI}. Credentials: Application Default Credentials
 * (e.g. {@code GOOGLE_APPLICATION_CREDENTIALS}).
 */
public final class EnvelopeRuntimeConfig {

  public static final String ENV_ENVELOPE_MODE = "ENVELOPE_MODE";

  /** Example: {@code gs://my-bucket/path/dek.encrypted.json} */
  public static final String ENV_GCS_DEK_URI = "GCS_DEK_URI";

  public static final String ENV_KMS_KEK_URI = "KMS_KEK_URI";

  private EnvelopeRuntimeConfig() {}

  public enum Mode {
    LOCAL,
    GCP
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
    if (isGcpMode(raw)) {
      return Mode.GCP;
    }
    return Mode.LOCAL;
  }

  /** Legacy names {@code staging} / {@code prod} still select GCP. */
  private static boolean isGcpMode(String raw) {
    return "gcp".equalsIgnoreCase(raw)
        || "cloud".equalsIgnoreCase(raw)
        || "staging".equalsIgnoreCase(raw)
        || "prod".equalsIgnoreCase(raw);
  }

  /** Use when branching (logs, metrics): GCP uses GCS+KMS; local does not. */
  public static boolean usesGoogleCloud(Mode m) {
    return m == Mode.GCP;
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
          "Missing " + name + " when ENVELOPE_MODE=gcp");
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
