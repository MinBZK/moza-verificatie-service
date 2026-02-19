package nl.rijksoverheid.moz.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class VerificationResponse {

    private boolean success;

    private Integer reasonId;

    private String reasonMessage;

    public VerificationResponse() {}

    public VerificationResponse(boolean success) {
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

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Integer getReasonId() {
        return reasonId;
    }

    public void setReasonId(Integer reasonId) {
        this.reasonId = reasonId;
    }

    public String getReasonMessage() {
        return reasonMessage;
    }

    public void setReasonMessage(String reasonMessage) {
        this.reasonMessage = reasonMessage;
    }
}
