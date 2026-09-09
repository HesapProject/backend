package uz.hesap.service.common.util;

import java.util.UUID;

public record CompanyBalanceTariffResponse(UUID companyId, Double balance, TariffResponse tariff) {}
