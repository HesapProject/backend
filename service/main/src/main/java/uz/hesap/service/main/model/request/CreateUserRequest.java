package uz.hesap.service.main.model.request;

import org.springframework.util.Assert;

/**
 * Request DTO for creating user after verification. Used with verify token to complete
 * registration.
 */
public record CreateUserRequest(
    String firstName,
    String lastName,
    String username,
    String uuid,
    String osVersion,
    String os,
    String model,
    String brand,
    String type,
    String device,
    String fcmToken) {
  public CreateUserRequest {
    Assert.hasLength(firstName, "firstName can't be null or empty");
    Assert.hasLength(lastName, "lastName can't be null or empty");
    Assert.notNull(uuid, "UUID can't be null");
  }
}
