package uz.hesap.service.main.model.response;

import java.util.Map;

public record SubjectCertificateInfo(
    String serialNumber, Map<String, String> subjectName, String validFrom, String validTo) {}
