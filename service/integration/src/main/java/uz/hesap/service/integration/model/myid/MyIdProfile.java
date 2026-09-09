package uz.hesap.service.integration.model.myid;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MyIdProfile(
    @JsonProperty("common_data") MyIdCommonData commonData,
    @JsonProperty("doc_data") MyIdDocData docData,
    MyIdContacts contacts,
    MyIdAddress address) {}
