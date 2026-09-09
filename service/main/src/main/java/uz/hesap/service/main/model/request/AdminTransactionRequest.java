package uz.hesap.service.main.model.request;

import java.util.UUID;

public record AdminTransactionRequest(UUID companyId, Double amount, String description) {
  public AdminTransactionRequest {}
}
