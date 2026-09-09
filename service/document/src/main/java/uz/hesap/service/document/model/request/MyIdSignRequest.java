package uz.hesap.service.document.model.request;

// MyID (yuz) orqali imzolash: frontend MyID SDK'dan olingan code + platforma.
// platform: "WEB" (web SDK) yoki "MOBILE" (mobil SDK). null bo'lsa MOBILE.
public record MyIdSignRequest(String code, String platform, java.util.UUID userPackageId) {}
