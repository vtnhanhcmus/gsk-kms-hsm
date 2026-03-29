package com.keywrap.demo.jakarta;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.Set;

/**
 * Payara/Jersey: resources must be registered explicitly; an empty {@code Application} alone often yields 404
 * (REST not mounted).
 */
@ApplicationPath("/api")
public class JaxRsApplication extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    return Set.of(CryptoResource.class);
  }
}
