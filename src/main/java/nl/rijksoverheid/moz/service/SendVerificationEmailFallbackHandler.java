package nl.rijksoverheid.moz.service;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.jboss.logging.Logger;

public class SendVerificationEmailFallbackHandler implements FallbackHandler<Boolean> {

    private static final Logger LOG = Logger.getLogger(SendVerificationEmailFallbackHandler.class);

    @Override
    public Boolean handle(ExecutionContext context) {
        if (context.getFailure() instanceof CircuitBreakerOpenException) {
            LOG.warn("Circuit breaker is open - skipping verification email send");
        }
        return false;
    }
}
