package com.keywrap.crypto;

/** Persists Tink keyset JSON wrapped by the KEK (GCS, in-memory, or another mock). */
public interface EncryptedKeysetStore {

  boolean exists();

  String readEncryptedKeysetJson();

  void writeEncryptedKeysetJson(String json);
}
