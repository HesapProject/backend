package uz.hesap.service.main.model.response;

public record BackendAuthResponse(
    SubjectCertificateInfo subjectCertificateInfo, int status, String message) {}
