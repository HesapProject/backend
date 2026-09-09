package uz.hesap.service.integration.model.ableid;

// s2s verify natijasi: sessiya kimga ochilgan (PINFL) va statusi (PENDING/SUCCESS).
public record AbleIdVerifyResponse(String pinfl, String status) {}
