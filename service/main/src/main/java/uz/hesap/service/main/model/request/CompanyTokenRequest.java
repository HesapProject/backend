package uz.hesap.service.main.model.request;

import java.util.UUID;

// Person token bilan yuborilib, shu company nomidan ishlovchi token olish uchun.
public record CompanyTokenRequest(UUID companyId) {}
