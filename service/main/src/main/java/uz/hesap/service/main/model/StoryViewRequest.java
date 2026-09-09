package uz.hesap.service.main.model;

import java.util.UUID;
import org.springframework.util.Assert;

public record StoryViewRequest(UUID storyId) {
  public StoryViewRequest {
    Assert.notNull(storyId != null, "storyId required");
  }
}
