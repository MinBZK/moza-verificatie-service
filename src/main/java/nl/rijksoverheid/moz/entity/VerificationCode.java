package nl.rijksoverheid.moz.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.smallrye.common.constraint.Nullable;
import jakarta.persistence.*;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hibernate.annotations.UpdateTimestamp;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "verification_code")
public class VerificationCode extends PanacheEntity {

    private String referenceId;
    private String code;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "verify_email_sent_at")
    private LocalDateTime verifyEmailSentAt;

    @Nullable
    private LocalDateTime verifiedAt;

    @Nullable
    private LocalDateTime validUntil;

    private static final SecureRandom RANDOM = new SecureRandom();

    public VerificationCode() {
        this.createdAt = LocalDateTime.now();
        int validityMinutes = ConfigProvider.getConfig()
                .getOptionalValue("verification.code.validity-minutes", Integer.class)
                .orElse(10);
        this.validUntil = createdAt.plusMinutes(validityMinutes);
        this.referenceId = UUID.randomUUID().toString();
        
        int codeLength = ConfigProvider.getConfig()
                .getOptionalValue("verification.code.length", Integer.class)
                .orElse(6);
        this.code = generateCode(codeLength);
    }
    
    private String generateCode(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Code length must be positive");
        }
        int min = (int) Math.pow(10, length - 1);
        int max = (int) Math.pow(10, length) - 1;
        int range = max - min + 1;
        return String.valueOf(min + RANDOM.nextInt(range));
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public LocalDateTime getVerifyEmailSentAt() {
        return verifyEmailSentAt;
    }

    public void setVerifyEmailSentAt(LocalDateTime verifyEmailSentAt) {
        this.verifyEmailSentAt = verifyEmailSentAt;
    }

    public boolean isUsed() {
        return verifiedAt != null;
    }


    public static Optional<VerificationCode> findByReferenceId(String referenceId) {
        return find("referenceId = ?1", referenceId).singleResultOptional();
    }

    public static List<VerificationCode> findSuccessfulVerifications() {
        return find("verifiedAt is not null").list();
    }

    public static List<VerificationCode> findExpiredCodes(LocalDateTime now) {
        return find("validUntil < ?1", now).list();
    }


}
