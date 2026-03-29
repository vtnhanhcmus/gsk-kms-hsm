Application (dạng lib dùng pom.xml)
       │
       ▼
Google Tink
   │
   ▼
KMS (encrypt key)
   │
   ▼
HSM (protect master key)


Envelope Encryption (DEK + KEK)
Dek file duoc lay tu GCS

---

## SDK `gcs-ksm-hsm-sdk` (Maven)

Maven coordinates:

| | |
|---|---|
| **groupId** | `com.gcsksmhsm` |
| **artifactId** | `gcs-ksm-hsm-sdk` |
| **version** | `1.0.0-SNAPSHOT` (đổi khi release) |

- **DEK**: keyset Tink (AES-128-GCM), file JSON **đã mã hóa** lưu trên **GCS**.
- **KEK**: khóa **Cloud KMS**; Tink dùng `GcpKmsClient` để bọc/giải bọc file DEK.
- **HSM**: tạo CryptoKey trên KMS với **protection level HSM** — master key nằm trong HSM của Google, không cần PKCS#11 trong app.

**Dependency kéo theo (transitive):** `tink`, `tink-gcpkms`, `google-cloud-storage` — không cần khai báo lại trừ khi bạn ép version khác.

**Java:** SDK được biên dịch với **bytecode Java 8** (`--release 8`) để app cũ vẫn import được API cấp 8. **Khi chạy**, JVM vẫn phải **11+** vì các thư viện Google Cloud / gRPC kéo theo không còn hỗ trợ JRE 8. (Nếu bạn chỉ dùng nhánh giả lập `InMemoryEncryptedKeysetStore` + KEK cục bộ và loại bỏ dependency GCS/KMS, có thể tách thành module riêng — mặc định hiện tại không hỗ trợ chạy trên JRE 8.)

### Cấu trúc repo

- **`pom.xml`** (gốc): reactor Maven, gồm module **`sdk/`** và **`examples/payara-envelope-demo/`**.
- **`sdk/`**: mã nguồn + `pom.xml` của **`gcs-ksm-hsm-sdk`** (artifact JAR).

### Build SDK trong repo này

Từ thư mục gốc:

```bash
mvn clean install
```

JAR SDK nằm trong `sdk/target/`: `gcs-ksm-hsm-sdk-*.jar`, `-sources.jar`, `-javadoc.jar`.

### Ví dụ Jakarta EE + Payara Micro

Module **`examples/payara-envelope-demo`**: WAR + Payara Micro, import SDK, REST `/api/demo/*` dùng `LocalEnvelopeSimulation`. Chi tiết và lệnh chạy: [`examples/payara-envelope-demo/README.md`](examples/payara-envelope-demo/README.md).

### Docker Compose (Payara Micro)

Cần **Docker** + **Docker Compose v2**. Từ thư mục gốc repo:

```bash
docker compose up --build
```

Payara Micro **khởi động chậm** (thường **30–60 giây**); đợi log có dạng *Payara Micro.* sẵn sàng rồi mới gọi HTTP.

Ứng dụng lắng nghe **8080**. WAR build dưới dạng **`ROOT.war`** → context **`/`**, nên ping:

```bash
curl -s http://127.0.0.1:8080/api/demo/ping
```

(kỳ vọng: `ok`).

`Dockerfile`: multi-stage (Maven 17 → JRE 17), chạy `ROOT-microbundle.jar` với `--nocluster` (tránh Hazelcast/cluster trong container).

#### Remote debug từ IntelliJ IDEA (Docker)

1. Chạy container có **JDWP** (port **5005**):

   ```bash
   docker compose -f docker-compose.yml -f docker-compose.debug.yml up --build
   ```

2. Trong IntelliJ: **Run → Edit Configurations → + → Remote JVM Debug** (hoặc dùng sẵn **“Remote Debug Payara (localhost:5005)”** trong `.idea/runConfigurations/` sau khi **Reload Maven Project**).
   - Host: `localhost`, Port: `5005`, **Debugger mode**: `Attach to remote JVM`.
   - Ở mục **Use module classpath**, chọn module **`payara-envelope-demo`** để breakpoint khớp source.

3. **Debug** (icon bọ cạnh) — đợi Payara khởi động xong rồi mới attach (hoặc để attach trước cũng được nếu dùng `suspend=y`, xem dưới).

**Chạy JAR local (không Docker)** với JDWP:

```bash
cd examples/payara-envelope-demo
mvn package -DskipTests
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
  -jar target/ROOT-microbundle.jar --nocluster
```

Muốn JVM **đợi debugger** trước khi chạy tiếp: đổi `suspend=n` thành `suspend=y`.

### Đưa SDK vào project khác (local)

Từ thư mục gốc repo (build module `sdk`):

```bash
mvn clean install
```

Sau đó trong app consumer (cùng máy, `~/.m2/repository`):

**Maven (`pom.xml`):**

```xml
<dependency>
  <groupId>com.gcsksmhsm</groupId>
  <artifactId>gcs-ksm-hsm-sdk</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Gradle (Kotlin DSL) `build.gradle.kts`:**

```kotlin
repositories { mavenLocal() }
dependencies {
  implementation("com.gcsksmhsm:gcs-ksm-hsm-sdk:1.0.0-SNAPSHOT")
}
```

**Gradle (Groovy) `build.gradle`:**

```groovy
repositories { mavenLocal() }
dependencies {
  implementation 'com.gcsksmhsm:gcs-ksm-hsm-sdk:1.0.0-SNAPSHOT'
}
```

Publish lên Nexus / Artifactory / Maven Central: cấu hình `distributionManagement` + `mvn deploy` (thường kèm signing). Trong `pom.xml` có comment gợi ý.

### Ví dụ

```java
EnvelopeEncryptionConfig cfg = new EnvelopeEncryptionConfig(
    "my-bucket",
    "crypto/dek.encrypted.json",
    "gcp-kms://projects/PROJECT/locations/LOCATION/keyRings/RING/cryptoKeys/KEY/cryptoKeyVersions/1",
    null // hoặc đường dẫn file JSON service account
);
EnvelopeCryptoService crypto = new EnvelopeCryptoService(cfg);
crypto.ensureDekOnGcs(); // lần đầu: tạo DEK, bọc KMS, ghi GCS
byte[] ct = crypto.encrypt("hello".getBytes(StandardCharsets.UTF_8));
byte[] pt = crypto.decrypt(ct);
```

### Test / dev không có tài khoản GCP

Dùng **KEK cục bộ (Tink)** + **`InMemoryEncryptedKeysetStore`** — cùng luồng envelope (DEK bọc bởi KEK), không gọi API Google.

```java
EnvelopeCryptoService crypto = LocalEnvelopeSimulation.newService();
crypto.ensureDekOnGcs();
byte[] ct = crypto.encrypt("hello".getBytes(StandardCharsets.UTF_8));
byte[] pt = crypto.decrypt(ct);
```

Hoặc tự tạo `new EnvelopeCryptoService(new InMemoryEncryptedKeysetStore(), kekAead)` với `kekAead` lấy từ `KeysetHandle.generateNew(PredefinedAeadParameters.AES128_GCM).getPrimitive(Aead.class)` (nhớ gọi `AeadConfig.register()` trước).

### Quyền GCP (gợi ý)

- KMS: `cloudkms.cryptoKeyVersions.useToEncrypt`, `useToDecrypt` trên KEK.
- GCS: đọc/ghi object chứa file DEK (ví dụ `storage.objects.get/create/update`).

Thiết lập credential: biến `GOOGLE_APPLICATION_CREDENTIALS` hoặc `gcloud auth application-default login`.
