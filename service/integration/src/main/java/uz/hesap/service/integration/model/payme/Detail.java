package uz.hesap.service.integration.model.payme;

import java.util.List;

public record Detail(Integer receipt_type, List<Items> items) {}
