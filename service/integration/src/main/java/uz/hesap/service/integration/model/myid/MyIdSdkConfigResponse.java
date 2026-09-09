package uz.hesap.service.integration.model.myid;

// Mobil ilova MyID SDK uchun config — iOS hardcode o'rniga backend'dan oladi.
public record MyIdSdkConfigResponse(String clientHash, String clientHashId, String environment) {}
