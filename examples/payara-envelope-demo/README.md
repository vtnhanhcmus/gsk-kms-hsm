# payara-envelope-demo

Ứng dụng **Jakarta EE 10** (WAR) chạy trên **Payara Micro 6**, phụ thuộc **`gcs-ksm-hsm-sdk`** và dùng `LocalEnvelopeSimulation` (không cần GCP).

**Cần JDK 17+** để build và để chạy JAR Payara Micro (không chạy được trên Java 8).

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
```

Kỳ vọng: `ok`.

Mã hóa / giải (body plaintext / Base64):

```bash
B64=$(echo -n 'hello' | curl -s -X POST --data-binary @- http://127.0.0.1:8080/api/demo/encrypt)
echo "$B64" | curl -s -X POST --data-binary @- http://127.0.0.1:8080/api/demo/decrypt
```

## REST

| Method | Path | Mô tả |
|--------|------|--------|
| GET | `/api/demo/ping` | Health |
| POST | `/api/demo/encrypt` | Body: plaintext UTF-8 → response: Base64 ciphertext |
| POST | `/api/demo/decrypt` | Body: Base64 → plaintext |

`@ApplicationPath("/api")` + `@Path("/demo")` → prefix **`/api/demo`**.
