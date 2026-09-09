package uz.hesap.service.document.model.response.eimzo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CertificateInfo(
    @JsonProperty("subjectName") String subjectNameString,
    Map<String, String> subjectInfo,
    String serialNumber,
    String validFrom,
    String validTo) {}
