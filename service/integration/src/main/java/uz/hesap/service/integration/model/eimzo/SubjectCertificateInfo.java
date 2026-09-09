package uz.hesap.service.integration.model.eimzo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SubjectCertificateInfo(
    String serialNumber, Map<String, String> subjectName, String validFrom, String validTo) {}
