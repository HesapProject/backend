package uz.hesap.service.integration.model.eimzo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// E-IMZO server'ning /backend/auth javobi.
@JsonIgnoreProperties(ignoreUnknown = true)
public record EImzoAuthResponse(
    SubjectCertificateInfo subjectCertificateInfo, int status, String message) {}
