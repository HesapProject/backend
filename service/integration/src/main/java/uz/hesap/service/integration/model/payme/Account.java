package uz.hesap.service.integration.model.payme;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/**
 * Payme kassasidagi hisob (account) rekviziti. Kassada maydon nomi `id` — checkout
 * linkda ham `ac.id=` yuboriladi. Eski linklarda `companyId` bo'lgani uchun u ham
 * alias sifatida qabul qilinadi.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Account(@JsonAlias({"id", "companyId"}) UUID companyId) {}
