package uz.hesap.service.main.model.request;

import java.time.Instant;
import uz.hesap.service.common.util.enums.CompanyType;
import uz.hesap.service.common.util.enums.OnboardingStatus;

public record CompanyRequest(
    CompanyType type,
    String name,
    String tin,
    String customName,
    Instant startTime,
    Instant pilotTime,
    Instant contractTime,
    Instant firstPaymentTime,
    OnboardingStatus onboardingStatus) {}
