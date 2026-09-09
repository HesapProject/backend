package uz.hesap.service.document.domain.enums;

public enum DocumentStatus {
  CREATED(1),
  SIGNED_BY_SELLER(4),
  SIGNED_BY_BUYER(4),
  // Ikkala tomon imzolagandan keyin shartnoma FAOL holatga o'tadi (yakunlanmaydi).
  ACTIVE(5),
  COMPLETED(10),
  REJECTED(20),
  CANCELLED(21);

  private final int order;

  DocumentStatus(int order) {
    this.order = order;
  }

  public int getOrder() {
    return order;
  }

  public boolean canTransitionTo(DocumentStatus newStatus) {
    if (this == COMPLETED || this == REJECTED || this == CANCELLED) {
      return false;
    }
    if (newStatus == REJECTED || newStatus == CANCELLED) {
      // Can always reject or cancel as long as not already terminal
      return true;
    }
    // Basic check: can generally move forward.
    // However, specific transitions (e.g. from WAITING_WITNESS_SIGN to SIGNED_BY_BUYER)
    // might need stricter rules, but user asked for `order` based check primarily.
    // We can refine logic: newStatus order > currentStatus order.
    return newStatus.getOrder() >= this.order;
  }
}
