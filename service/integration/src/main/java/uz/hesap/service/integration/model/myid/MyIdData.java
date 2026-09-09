package uz.hesap.service.integration.model.myid;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record MyIdData(
    @JsonProperty("comparison_value") Double comparisonValue,
    @JsonProperty("pass_data") String passData,
    @JsonProperty("job_id") UUID jobId,
    MyIdProfile profile) {}
