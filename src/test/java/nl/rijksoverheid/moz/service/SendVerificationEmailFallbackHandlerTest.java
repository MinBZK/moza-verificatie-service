package nl.rijksoverheid.moz.service;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;

class SendVerificationEmailFallbackHandlerTest {

    private final SendVerificationEmailFallbackHandler handler = new SendVerificationEmailFallbackHandler();

    @Test
    void testHandleReturnsFalseWhenCircuitBreakerOpen() {
        ExecutionContext context = Mockito.mock();
        Mockito.when(context.getFailure()).thenReturn(new CircuitBreakerOpenException());

        assertFalse(handler.handle(context));
    }

    @Test
    void testHandleReturnsFalseOnOtherException() {
        ExecutionContext context = Mockito.mock();
        Mockito.when(context.getFailure()).thenReturn(new RuntimeException("Connection refused"));

        assertFalse(handler.handle(context));
    }
}
