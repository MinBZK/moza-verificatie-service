package nl.rijksoverheid.moz.dto.response;

import com.fasterxml.jackson.annotation.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class VerificationResponse {

    private final boolean success;

    @JsonProperty
    private Integer reasonId;

    @JsonProperty
    private String reasonMessage;

    @JsonCreator
    public VerificationResponse(@JsonProperty("success") boolean success) {
        this.success = success;
    }

    public VerificationResponse(boolean success, VerificationFailureReason reason) {
        this.success = success;
        if (reason != null) {
            this.reasonId = reason.getId();
            this.reasonMessage = reason.getMessage();
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public Integer getReasonId() {
        return reasonId;
    }

    public String getReasonMessage() {
        return reasonMessage;
    }
}
