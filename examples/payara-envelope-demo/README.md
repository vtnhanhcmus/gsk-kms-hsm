# payara-envelope-demo

Ứng dụng **Jakarta EE 10** (WAR) chạy trên **Payara Micro 6**, phụ thuộc **`gcs-ksm-hsm-sdk`**.

**Cần JDK 17+** để build và để chạy JAR Payara Micro (không chạy được trên Java 8).

## Cấu hình môi trường (`ENVELOPE_MODE`)

| Giá trị | Khi nào | Yêu cầu |
|--------|---------|---------|
| **`local`** (mặc định) | Dev trên máy, không GCP | Không cần biến nào — dùng `LocalEnvelopeSimulation` (DEK in-memory). |
| **`gcp`** | Production (GCS + Cloud KMS) | `GCS_BUCKET`, `GCS_OBJECT_NAME`, `KMS_KEK_URI`; credential qua ADC hoặc `GCP_CREDENTIALS_PATH`. |

### Biến môi trường (GCP)

| Biến | Bắt buộc (khi `gcp`) | Mô tả |
|------|----------------------|--------|
| `ENVELOPE_MODE` | Không | `local` hoặc `gcp` (mặc định `local`). |
| `GCS_BUCKET` | Có | Tên bucket chứa file DEK. |
| `GCS_OBJECT_NAME` | Có | Tên object (ví dụ `crypto/dek.encrypted.json`). |
| `KMS_KEK_URI` | Có | URI KEK, dạng `gcp-kms://projects/.../locations/.../keyRings/.../cryptoKeys/.../cryptoKeyVersions/...` |
| `GCP_CREDENTIALS_PATH` | Không | Đường dẫn file JSON service account. Nếu **không** set: SDK dùng **Application Default Credentials** (khuyến nghị trên GKE: Workload Identity; máy dev: `GOOGLE_APPLICATION_CREDENTIALS`). |

Ví dụ chạy **local**:

```bash
export ENVELOPE_MODE=local   # hoặc bỏ hẳn (mặc định)
java -jar target/ROOT-microbundle.jar --nocluster
```

Ví dụ chạy **GCP** (cùng máy có credential):

```bash
export ENVELOPE_MODE=gcp
export GCS_BUCKET=my-bucket
export GCS_OBJECT_NAME=crypto/dek.encrypted.json
export KMS_KEK_URI='gcp-kms://projects/PROJECT/locations/LOCATION/keyRings/RING/cryptoKeys/KEY/cryptoKeyVersions/1'
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/sa.json   # ADC
java -jar target/ROOT-microbundle.jar --nocluster
```

Có thể dùng system property tương ứng (ví dụ `-Denvelope.mode=gcp`, `-Dgcs.bucket=...`) — xem `EnvelopeRuntimeConfig`.

## Build (từ thư mục gốc repo)

```bash
cd /path/to/gcs-ksm-hsm
mvn clean install
```

## Chạy trên máy (không Docker)

Docker **không bắt buộc**. Build xong, chạy JAR Payara Micro trực tiếp bằng `java` (hoặc cấu hình **Run** trong IntelliJ: *JAR application* → `ROOT-microbundle.jar`, VM options `--nocluster`).

## Tạo JAR Payara Micro (uber) và chạy

`pom` dùng `<finalName>ROOT</finalName>` → file **`ROOT-microbundle.jar`**, ứng dụng ở context **`/`** (không prefix `/payara-envelope-demo`).

```bash
cd examples/payara-envelope-demo
mvn package
java -jar target/ROOT-microbundle.jar --nocluster
```

Đợi log báo server sẵn sàng (thường port **8080**), rồi thử:

```bash
curl -s http://127.0.0.1:8080/api/demo/ping
curl -s http://127.0.0.1:8080/api/demo/mode
```

Kỳ vọng: `ok` và `local` (hoặc `gcp` nếu đã cấu hình).

Mã hóa / giải (body plaintext / Base64):

```bash
B64=$(echo -n 'hello' | curl -s -X POST --data-binary @- http://127.0.0.1:8080/api/demo/encrypt)
echo "$B64" | curl -s -X POST --data-binary @- http://127.0.0.1:8080/api/demo/decrypt
```

## REST

| Method | Path | Mô tả |
|--------|------|--------|
| GET | `/api/demo/ping` | Health |
| GET | `/api/demo/mode` | `local` hoặc `gcp` |
| POST | `/api/demo/encrypt` | Body: plaintext UTF-8 → response: Base64 ciphertext |
| POST | `/api/demo/decrypt` | Body: Base64 → plaintext |

`@ApplicationPath("/api")` + `@Path("/demo")` → prefix **`/api/demo`**.
