package nl.rijksoverheid.moz.dto.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Reason for verification failure. " +
        "1: Reference ID not found, " +
        "2: Code expired, " +
        "3: Code already used, " +
        "4: Incorrect code")
public enum VerificationFailureReason {
    CODE_NOT_FOUND(1, "Reference ID not found"),
    CODE_EXPIRED(2, "Code expired"),
    CODE_ALREADY_USED(3, "Code already used"),
    INCORRECT_CODE(4, "Incorrect code");

    private final int id;
    private final String message;

    VerificationFailureReason(int id, String message) {
        this.id = id;
        this.message = message;
    }

    public int getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }
}
