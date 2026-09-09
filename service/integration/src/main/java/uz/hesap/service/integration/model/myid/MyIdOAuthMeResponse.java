package uz.hesap.service.integration.model.myid;

// MyID OAuth /api/v1/users/me javobi: { profile: { common_data: { pinfl, ... }, ... } }
// (SDK oqimidagi MyIdUserDataResponse'dan farqli — `data` wrapper yo'q.)
public record MyIdOAuthMeResponse(MyIdProfile profile) {}
