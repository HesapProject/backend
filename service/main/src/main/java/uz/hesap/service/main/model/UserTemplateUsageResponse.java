package uz.hesap.service.main.model;

import uz.hesap.service.common.util.TemplateBasicResponse;

// Aktiv paketda bitta shablon (shartnoma turi) bo'yicha berilgan va ishlatilgan son.
// used = user_package_usage'dagi ACTIVE qatorlar (shu user_package + template).
public record UserTemplateUsageResponse(TemplateBasicResponse template, Integer count, Integer used) {}
