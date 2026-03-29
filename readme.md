# gcs-ksm-hsm

Envelope encryption with **Tink** (DEK on **GCS**, KEK on **Cloud KMS**). The KEK can use **HSM** protection level in KMS (no PKCS#11 in the app).

```
┌─────────────┐     wrap/unwrap      ┌──────────────┐
│ Cloud KMS   │◄────────────────────►│  Tink DEK    │
│ (KEK, HSM)  │                      │ (AES-GCM…)   │
└─────────────┘                      └──────┬───────┘
                                          │ read/write ciphertext
                                          ▼
                                   ┌──────────────┐
                                   │ GCS object   │
                                   │ (encrypted   │
                                   │  keyset)     │
                                   └──────────────┘
```

## Modules

| Module | Role |
|--------|------|
| **`sdk`** | `keywrap-sdk` — envelope API (Tink + GCS + KMS) |
| **`examples/payara-envelope-demo`** | Jakarta EE 10 + Payara Micro — REST demo |

**Coordinates (reactor root `pom.xml`):**

| Field | Value |
|-------|-------|
| **groupId** | `com.keywrap` |
| **artifactId (sdk)** | `keywrap-sdk` |
| **version** | `1.0.0-SNAPSHOT` (bump when releasing) |

## SDK (`sdk`)

- **DEK**: Tink keyset, stored encrypted on GCS (object URI you configure).
- **KEK**: Cloud KMS key — create a symmetric key with purpose **encrypt/decrypt**; choose **HSM** protection level if required.
- **HSM**: create the CryptoKey in KMS with **HSM** protection level — the master key stays in Google’s HSM; no PKCS#11 in the app.

**Transitive dependencies:** `tink`, `tink-gcpkms`, `google-cloud-storage` — you do not need to declare them again unless you pin versions.

**Java:** the SDK is compiled with **Java 8 bytecode** (`--release 8`) so older apps can still use the level-8 API. **At runtime**, the JVM must still be **11+** because Google Cloud / gRPC libraries no longer support JRE 8. (If you only use the `InMemoryEncryptedKeysetStore` simulation path with a local KEK and remove GCS/KMS dependencies, you could split a separate module — the default setup does not support JRE 8.)

### Usage (Maven)

Add the BOM (optional but recommended) and the SDK:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.google.cloud</groupId>
      <artifactId>libraries-bom</artifactId>
      <version>26.50.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependency>
  <groupId>com.keywrap</groupId>
  <artifactId>keywrap-sdk</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Install the reactor locally first: `mvn clean install` from the repo root.

### Jakarta EE + Payara Micro example

See **`examples/payara-envelope-demo`**.

Build the microbundle:

```bash
cd examples/payara-envelope-demo
mvn -q clean package
java -jar target/ROOT-microbundle.jar
```

REST base path: **`/api`** (e.g. `GET http://localhost:8080/api/demo/ping`).

**Docker Compose** (8080):

```bash
docker compose up --build
```

**Run the JAR locally (no Docker)** with JDWP:

```bash
cd examples/payara-envelope-demo
mvn -q clean package
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
  -jar target/ROOT-microbundle.jar
```

Attach the debugger to port **5005** (e.g. IntelliJ: Run → Attach to Process / Remote JVM Debug).

To make the JVM **wait for the debugger** before continuing: change `suspend=n` to `suspend=y`.

**Compose with debug** (exposes 5005):

```bash
docker compose -f docker-compose.yml -f docker-compose.debug.yml up --build
```

### IAM (GCP)

Typical roles for the runtime identity:

- **KMS**: `cloudkms.cryptoKeyEncrypterDecrypter` on the KEK (or a custom role with `cryptoKeys.encrypt`, `cryptoKeys.decrypt`).
- **GCS**: read/write the DEK object (e.g. `storage.objects.get/create/update`).

### Publishing

Publish to Nexus / Artifactory / Maven Central: configure `distributionManagement` + `mvn deploy` (often with signing). See comments in the POMs for hints.

### Example

Minimal flow: build config → create service → ensure DEK exists on first deploy → encrypt/decrypt.

```java
EnvelopeEncryptionConfig cfg = new EnvelopeEncryptionConfig(
    "my-bucket", "crypto/dek.encrypted.json",
    "gcp-kms://projects/.../locations/.../keyRings/.../cryptoKeys/.../cryptoKeyVersions/1",
    null);

EnvelopeCryptoService crypto = new EnvelopeCryptoService(cfg);
crypto.ensureDekOnGcs(); // idempotent: create DEK + upload if missing

byte[] ct = crypto.encrypt("hello".getBytes(StandardCharsets.UTF_8));
byte[] pt = crypto.decrypt(ct);
```

### Test / dev without a GCP account

Use a **local Tink KEK** + **`InMemoryEncryptedKeysetStore`** — same envelope flow (DEK wrapped by KEK), no Google API calls.

```java
EnvelopeCryptoService crypto = LocalEnvelopeSimulation.newService();
crypto.ensureDekOnGcs();
```

Or use `new EnvelopeEncryptionConfig(...)` with a **mock/fake** `EncryptedKeysetStore` in tests.

## Repo layout

```
gcs-ksm-hsm/
├── pom.xml                 # reactor (dependencyManagement, versions)
├── sdk/                    # keywrap-sdk
│   └── src/main/java/com/keywrap/crypto/
└── examples/payara-envelope-demo/   # WAR + Payara Micro bundle
```

## References

- [Tink](https://github.com/tink-crypto/tink-java)
- [Tink GCP KMS](https://github.com/tink-crypto/tink-java/tree/main/kms)
- [Google Cloud KMS](https://cloud.google.com/kms/docs)
- [Cloud Storage](https://cloud.google.com/storage/docs)
