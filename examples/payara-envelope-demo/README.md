# payara-envelope-demo

Jakarta EE 10 example on **Payara Micro**, using **`keywrap-sdk`**.

- **WAR** `ROOT.war` and microbundle **`ROOT-microbundle.jar`** (`finalName` = `ROOT` → context **`/`**).
- REST: application base path **`/api`** (`JaxRsApplication`), resources under **`com.keywrap.demo.jakarta`**.
- **`ENVELOPE_MODE`**: `local` | `staging` | `prod`. **`local`** uses `LocalEnvelopeSimulation` (no GCP). **`staging`** / **`prod`** use real GCS + KMS (same code path; differ by env at deploy time).

## Environment variables (only when `staging` or `prod`)

| Variable | Example |
|----------|---------|
| `GCS_DEK_URI` | `gs://my-bucket/path/dek.encrypted.json` |
| `KMS_KEK_URI` | `projects/.../locations/.../keyRings/.../cryptoKeys/...` |

Credentials: **Application Default Credentials** (e.g. `GOOGLE_APPLICATION_CREDENTIALS` pointing at a JSON key file).

## Build and run

```bash
mvn -q clean package
java -jar target/ROOT-microbundle.jar
```

Ping: `GET http://localhost:8080/api/demo/ping`

Encrypt (demo): `POST http://localhost:8080/api/demo/encrypt` — body: plaintext string.

Decrypt: `POST http://localhost:8080/api/demo/decrypt` — body: Base64 ciphertext from encrypt.
