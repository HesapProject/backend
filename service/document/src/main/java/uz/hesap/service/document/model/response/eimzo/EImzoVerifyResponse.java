package uz.hesap.service.document.model.response.eimzo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EImzoVerifyResponse(Pkcs7Info pkcs7Info, int status, String message) {}
