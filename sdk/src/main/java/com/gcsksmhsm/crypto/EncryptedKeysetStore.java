package com.gcsksmhsm.crypto;

/** Lưu JSON keyset Tink đã được bọc bởi KEK (GCS, in-memory, hoặc mock khác). */
public interface EncryptedKeysetStore {

  boolean exists();

  String readEncryptedKeysetJson();

  void writeEncryptedKeysetJson(String json);
}
