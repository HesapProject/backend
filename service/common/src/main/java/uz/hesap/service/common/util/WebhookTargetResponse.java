package uz.hesap.service.common.util;

import java.util.List;
import java.util.UUID;
import uz.hesap.service.common.util.enums.WebhookEventType;

// Webhook yetkazish manzili — `POST /main/v1/local/api-keys/webhooks` javobi.
// events bo'sh bo'lsa kalit BARCHA hodisalarga obuna hisoblanadi.
public record WebhookTargetResponse(
    UUID keyId, String ownerIn, String url, String secret, List<WebhookEventType> events) {

  public boolean subscribedTo(final WebhookEventType event) {
    return events == null || events.isEmpty() || events.contains(event);
  }
}
