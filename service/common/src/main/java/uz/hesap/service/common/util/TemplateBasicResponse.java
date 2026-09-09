package uz.hesap.service.common.util;

import java.util.UUID;

// sellerName/buyerName — shablon taraf rol nomlari (Sotuvchi/Xaridor va h.k.).
// exchangeMode — oldi-berdi turi (GOODS/MONEY) String'da (common document enum'iga
// bog'lanmaydi); mijozlar shartnoma mahsulot/summa ko'rinishini shu bilan tanlaydi.
public record TemplateBasicResponse(
    UUID id, TextModel name, TextModel sellerName, TextModel buyerName, String exchangeMode) {}
