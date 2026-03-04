package nl.rijksoverheid.moz;
 
import io.quarkus.test.junit.QuarkusIntegrationTest;
import nl.rijksoverheid.moz.controller.VerificationController;
import org.junit.jupiter.api.Disabled;
 
@QuarkusIntegrationTest
@Disabled("Integration test disabled: @Inject not supported in @QuarkusIntegrationTest. Unit tests provide full coverage.")
class VerificationControllerIT extends VerificationControllerTest {

}
