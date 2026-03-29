package com.keywrap.crypto;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Reads/writes KMS-encrypted Tink keyset JSON to a GCS object.
 *
 * <p>Authentication uses the same rules as other Google Cloud Java clients:
 * <ul>
 *   <li>If {@code gcpCredentialsPath} is non-null, that JSON key file is used for Storage.
 *   <li>Otherwise {@linkplain StorageOptions#getDefaultInstance() default} credentials apply:
 *       {@code GOOGLE_APPLICATION_CREDENTIALS}, workload identity on GKE/GCE, or
 *       {@code gcloud auth application-default login} for local dev.
 * </ul>
 */
public final class GcsEncryptedKeysetStore implements EncryptedKeysetStore {

  private final Storage storage;
  private final String bucket;
  private final String objectName;

  public GcsEncryptedKeysetStore(String bucket, String objectName) {
    this(bucket, objectName, null);
  }

  /**
   * @param gcpCredentialsPath path to service account JSON, or null for Application Default Credentials
   */
  public GcsEncryptedKeysetStore(String bucket, String objectName, String gcpCredentialsPath) {
    this(createStorage(gcpCredentialsPath), bucket, objectName);
  }

  public GcsEncryptedKeysetStore(Storage storage, String bucket, String objectName) {
    this.storage = Objects.requireNonNull(storage, "storage");
    this.bucket = Objects.requireNonNull(bucket, "bucket");
    this.objectName = Objects.requireNonNull(objectName, "objectName");
  }

  private static Storage createStorage(String gcpCredentialsPath) {
    try {
      if (gcpCredentialsPath != null && !gcpCredentialsPath.trim().isEmpty()) {
        try (InputStream in = Files.newInputStream(Paths.get(gcpCredentialsPath))) {
          GoogleCredentials credentials = GoogleCredentials.fromStream(in);
          return StorageOptions.newBuilder().setCredentials(credentials).build().getService();
        }
      }
      return StorageOptions.getDefaultInstance().getService();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create GCS Storage client", e);
    }
  }

  @Override
  public boolean exists() {
    BlobId id = BlobId.of(bucket, objectName);
    return storage.get(id) != null;
  }

  @Override
  public String readEncryptedKeysetJson() {
    BlobId id = BlobId.of(bucket, objectName);
    Blob blob = storage.get(id);
    if (blob == null) {
      throw new IllegalStateException("GCS object does not exist: gs://" + bucket + "/" + objectName);
    }
    return new String(blob.getContent(), StandardCharsets.UTF_8);
  }

  @Override
  public void writeEncryptedKeysetJson(String json) {
    BlobId id = BlobId.of(bucket, objectName);
    BlobInfo info = BlobInfo.newBuilder(id).setContentType("application/json").build();
    storage.create(info, json.getBytes(StandardCharsets.UTF_8));
  }
}
