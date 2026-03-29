package com.gcsksmhsm.demo.jakarta;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.Set;

/**
 * Payara/Jersey: cần khai báo rõ lớp resource; chỉ {@code Application} rỗng dễ dẫn tới 404 (không mount
 * REST).
 */
@ApplicationPath("/api")
public class JaxRsApplication extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    return Set.of(CryptoResource.class);
  }
}
