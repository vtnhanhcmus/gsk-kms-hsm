package com.gcsksmhsm.crypto;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Đọc/ghi nội dung keyset Tink đã mã hóa bởi KMS (JSON) lên một object GCS. */
public final class GcsEncryptedKeysetStore implements EncryptedKeysetStore {

  private final Storage storage;
  private final String bucket;
  private final String objectName;

  public GcsEncryptedKeysetStore(String bucket, String objectName) {
    this(StorageOptions.getDefaultInstance().getService(), bucket, objectName);
  }

  public GcsEncryptedKeysetStore(Storage storage, String bucket, String objectName) {
    this.storage = Objects.requireNonNull(storage, "storage");
    this.bucket = Objects.requireNonNull(bucket, "bucket");
    this.objectName = Objects.requireNonNull(objectName, "objectName");
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
      throw new IllegalStateException("GCS object không tồn tại: gs://" + bucket + "/" + objectName);
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
