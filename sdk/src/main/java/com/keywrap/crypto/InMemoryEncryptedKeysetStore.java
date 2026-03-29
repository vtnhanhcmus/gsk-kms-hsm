package com.keywrap.crypto;

import java.util.concurrent.atomic.AtomicReference;

/** In-RAM stand-in for a GCS object — for tests / dev without GCP. */
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
      throw new IllegalStateException("No keyset in InMemoryEncryptedKeysetStore yet");
    }
    return s;
  }

  @Override
  public void writeEncryptedKeysetJson(String json) {
    content.set(json);
  }
}
