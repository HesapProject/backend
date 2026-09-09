package uz.hesap.service.integration.model.click;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ClickFiscalizationModel(
    @JsonProperty("service_id") Long serviceId,
    @JsonProperty("payment_id") Long paymentId,
    @JsonProperty("fiscal_items") List<ClickItemModel> items,
    @JsonProperty("received_ecash") Long receivedEcash,
    @JsonProperty("received_cash") Long receivedCash,
    @JsonProperty("received_card") Long receivedCard) {}
