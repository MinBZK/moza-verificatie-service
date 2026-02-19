package nl.rijksoverheid.moz.controller;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import nl.rijksoverheid.moz.dto.request.VerificationApplicationRequest;
import nl.rijksoverheid.moz.dto.request.VerificationRequest;
import nl.rijksoverheid.moz.dto.response.VerificationFailureReason;
import nl.rijksoverheid.moz.dto.response.VerificationResponse;
import nl.rijksoverheid.moz.entity.VerificationCode;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Path("/")
public class VerificationController {

    private static final Logger LOG = Logger.getLogger(VerificationController.class);

    /**
     * Emitter for the 'verification-requests-out' channel.
     * Messages sent via this emitter will be forwarded to the 'verification-requests' exchange in RabbitMQ,
     * as configured in application.properties.
     */
    @Channel("verification-requests-out")
    Emitter<String> requestEmitter;


    @POST
    @Path("/request")
    @Transactional
    @Operation(
            summary = "Create a new verification request",
            description = "Creates a new verification code for the given email and sends it via the messaging system."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Verification request created",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(implementation = String.class))
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid request"
            )
    })
    public String requestVerification(@Valid VerificationApplicationRequest request) {
        LOG.info("Creating verification request for email: " + request.getEmail());
        VerificationCode code = new VerificationCode(request.getEmail());
        code.persist();
        requestEmitter.send(String.valueOf(code.id));
        return code.getReferenceId();
    }

    @POST
    @Path("/verify")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Operation(
            summary = "Verify a verification code",
            description = "Verifies the code for a given reference ID and email."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Verification attempt completed",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = VerificationResponse.class))
            )
    })
    public VerificationResponse verify(@Valid VerificationRequest request) {

        Optional<VerificationCode> codeOpt = VerificationCode.findByReferenceIdAndEmail(request.getReferenceId(), request.getEmail());
        if (codeOpt.isEmpty()) {
            LOG.warn("Verification failed: code not found for referenceId: " + request.getReferenceId() + " and email: " + request.getEmail());
            return new VerificationResponse(false, VerificationFailureReason.CODE_NOT_FOUND);
        }
        VerificationCode code = codeOpt.get();

        if (code.getValidUntil().isBefore(LocalDateTime.now())) {
            LOG.warn("Verification failed: code expired for referenceId: " + request.getReferenceId());
            return new VerificationResponse(false, VerificationFailureReason.CODE_EXPIRED);
        }

        if (code.isUsed()) {
            LOG.warn("Verification failed: code already used for referenceId: " + request.getReferenceId());
            return new VerificationResponse(false, VerificationFailureReason.CODE_ALREADY_USED);
        }

        if (!Objects.equals(code.getCode(), request.getCode())) {
            LOG.warn("Verification failed: incorrect code for referenceId: " + request.getReferenceId());
            return new VerificationResponse(false, VerificationFailureReason.INCORRECT_CODE);
        }

        VerificationCode.update("verifiedAt = ?1 where id = ?2", LocalDateTime.now(), code.id);
        LOG.info("Verification successful for referenceId: " + request.getReferenceId());
        return new VerificationResponse(true);
    }
}
