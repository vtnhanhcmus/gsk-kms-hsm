package com.keywrap.demo.jakarta;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.keywrap.crypto.EnvelopeCryptoService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.security.GeneralSecurityException;
import java.util.Base64;

@RequestScoped
@Path("/demo")
public class CryptoResource {

  @Inject EnvelopeCryptoHolder holder;

  @GET
  @Path("/ping")
  @Produces(MediaType.TEXT_PLAIN)
  public String ping() {
    return "ok";
  }

  /** Returns {@code local}, {@code staging}, or {@code prod}. */
  @GET
  @Path("/mode")
  @Produces(MediaType.TEXT_PLAIN)
  public String mode() {
    return EnvelopeRuntimeConfig.mode().name().toLowerCase();
  }

  /** Body: plaintext UTF-8; response: Base64 ciphertext */
  @POST
  @Path("/encrypt")
  @Consumes(MediaType.TEXT_PLAIN)
  @Produces(MediaType.TEXT_PLAIN)
  public String encrypt(String plaintext) throws GeneralSecurityException {
    EnvelopeCryptoService crypto = holder.get();
    byte[] ct = crypto.encrypt(plaintext.getBytes(UTF_8));
    return Base64.getEncoder().encodeToString(ct);
  }

  /** Body: Base64 ciphertext; response: plaintext UTF-8 */
  @POST
  @Path("/decrypt")
  @Consumes(MediaType.TEXT_PLAIN)
  @Produces(MediaType.TEXT_PLAIN)
  public String decrypt(String base64Ciphertext) throws GeneralSecurityException {
    EnvelopeCryptoService crypto = holder.get();
    byte[] raw = Base64.getDecoder().decode(base64Ciphertext.trim());
    byte[] pt = crypto.decrypt(raw);
    return new String(pt, UTF_8);
  }
}
