package nl.rijksoverheid.moz.exception;

import jakarta.annotation.Priority;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import nl.rijksoverheid.moz.dto.response.ErrorResponse;
import org.jboss.logging.Logger;

import java.util.stream.Collectors;

@Provider
@Priority(1)
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        LOG.debugf("Mapping exception: %s", exception.getClass().getName());

        return switch (exception) {
            case ConstraintViolationException constraintViolationException ->
                    handleConstraintViolationException(constraintViolationException);
            case ValidationException validationException -> handleValidationException(validationException);
            case WebApplicationException webAppException -> handleWebApplicationException(webAppException);
            default -> handleGenericException(exception);
        };

    }

    private Response handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + " " + cv.getMessage())
                .collect(Collectors.joining(", "));
        
        LOG.warnf("Constraint violation: %s", message);

        ErrorResponse errorResponse = new ErrorResponse(message, "VALIDATION_ERROR");
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private Response handleValidationException(ValidationException e) {
        LOG.warnf("Validation failed: %s", e.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), "VALIDATION_ERROR");
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private Response handleWebApplicationException(WebApplicationException e) {
        int status = e.getResponse().getStatus();
        String message = e.getMessage();
        
        LOG.errorf(e, "WebApplicationException occurred with status %d: %s", status, message);

        ErrorResponse errorResponse = new ErrorResponse(message, "HTTP_" + status);
        return Response.status(status)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private Response handleGenericException(Throwable e) {
        LOG.error("An unexpected error occurred", e);

        ErrorResponse errorResponse = new ErrorResponse("Internal Server Error", "INTERNAL_ERROR");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
