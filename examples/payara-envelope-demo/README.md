# payara-envelope-demo

Jakarta EE 10 example on **Payara Micro**, using **`keywrap-sdk`**.

- **WAR** `ROOT.war` and microbundle **`ROOT-microbundle.jar`** (`finalName` = `ROOT` → context **`/`**).
- REST: application base path **`/api`** (`JaxRsApplication`), resources under **`com.keywrap.demo.jakarta`**.
- **`ENVELOPE_MODE`**: **`local`** (in-memory simulation, no GCP) or **`gcp`** (real GCS + Cloud KMS). Legacy values `staging`, `prod`, or `cloud` are treated the same as **`gcp`**.

## Environment variables (only when `ENVELOPE_MODE=gcp`)

| Variable | Example |
|----------|---------|
| `GCS_DEK_URI` | `gs://my-bucket/path/dek.encrypted.json` |
| `KMS_KEK_URI` | `projects/.../locations/.../keyRings/.../cryptoKeys/...` |

### How GCS (and KMS) authenticate

The app does **not** define separate GCS credentials. The Google **Storage** and **KMS** clients use **Application Default Credentials**:

| Setup | What to do |
|-------|------------|
| Service account key file | Export `GOOGLE_APPLICATION_CREDENTIALS=/path/to/key.json` before `java -jar ...` |
| GKE / GCE | Use Workload Identity or the instance service account (ADC picks it up). |
| Local laptop | `gcloud auth application-default login`, or set `GOOGLE_APPLICATION_CREDENTIALS`. |

Optional: in your own code you can pass a path into `new EnvelopeEncryptionConfig(bucket, object, kmsUri, "/path/to/key.json")` so **both** GCS and KMS use that file (the demo uses `null` and relies on ADC).

## Build and run

```bash
mvn -q clean package
java -jar target/ROOT-microbundle.jar
```

Ping: `GET http://localhost:8080/api/demo/ping`

Encrypt (demo): `POST http://localhost:8080/api/demo/encrypt` — body: plaintext string.

Decrypt: `POST http://localhost:8080/api/demo/decrypt` — body: Base64 ciphertext from encrypt.
