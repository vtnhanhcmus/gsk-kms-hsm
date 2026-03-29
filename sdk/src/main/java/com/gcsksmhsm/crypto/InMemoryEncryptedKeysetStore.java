package com.gcsksmhsm.crypto;

import java.util.concurrent.atomic.AtomicReference;

/** Giả lập object GCS trong RAM — dùng cho test / dev không có GCP. */
public final class InMemoryEncryptedKeysetStore implements EncryptedKeysetStore {

  private final AtomicReference<String> content = new AtomicReference<>();

  @Override
  public boolean exists() {
    return content.get() != null;
  }

  @Override
  public String readEncryptedKeysetJson() {
    String s = content.get();
    if (s == null) {
      throw new IllegalStateException("Chưa có keyset trong InMemoryEncryptedKeysetStore");
    }
    return s;
  }

  @Override
  public void writeEncryptedKeysetJson(String json) {
    content.set(json);
  }
}
