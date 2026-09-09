package uz.hesap.service.document.model.response.eimzo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Pkcs7Info(List<SignerInfo> signers, String documentBase64) {}
