package uz.hesap.service.document.model.response;

import java.util.Map;
import java.util.UUID;
import uz.hesap.service.common.util.TemplateBasicResponse;
import uz.hesap.service.common.util.UserBasicResponse;

public record MetadataBundle(
    // Taraflar + yaratuvchi — barchasi PINFL/STIR bo'yicha (buyer_in/seller_in/creator_in).
    Map<String, UserBasicResponse> partyUsers,
    Map<UUID, TemplateBasicResponse> templates) {}
