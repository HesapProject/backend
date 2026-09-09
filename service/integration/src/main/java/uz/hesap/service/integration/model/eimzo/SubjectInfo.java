package uz.hesap.service.integration.model.eimzo;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SubjectInfo(
    @JsonProperty("1.2.860.3.16.1.2") String tin, // STIR (INN) uchun OID
    String CN,
    String O) {}
