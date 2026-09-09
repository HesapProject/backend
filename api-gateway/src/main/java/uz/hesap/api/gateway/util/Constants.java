package uz.hesap.api.gateway.util;

public record Constants() {

  // Spring Boot Actuator services
  public static final String AUTOCONFIG_ENDPOINT = "/autoconfig";
  public static final String BEANS_ENDPOINT = "/beans";
  public static final String CONFIGPROPS_ENDPOINT = "/configprops";
  public static final String ENV_ENDPOINT = "/env";
  public static final String MAPPINGS_ENDPOINT = "/mappings";
  public static final String METRICS_ENDPOINT = "/metrics";
  public static final String SHUTDOWN_ENDPOINT = "/shutdown";
}
