package uz.hesap.service.integration.model.myid;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MyIdDocData(
    @JsonProperty("pass_data") String passData,
    @JsonProperty("issued_by") String issuedBy,
    @JsonProperty("issued_by_id") String issuedById,
    @JsonProperty("issued_date") String issuedDate,
    @JsonProperty("expiry_date") String expiryDate,
    @JsonProperty("doc_type") String docType,
    @JsonProperty("doc_type_id") String docTypeId,
    @JsonProperty("doc_type_id_cbu") String docTypeIdCbu) {}
