package com.example.devoopsclass;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rollback_requests")
public class RollbackRequest {
    @Id
    private UUID id;

    @Column(name = "target_image", nullable = false, length = 100)
    private String targetImage;

    @Column(name = "previous_image", length = 100)
    private String previousImage;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false, length = 40)
    private String phase;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected RollbackRequest() {
    }

    RollbackRequest(UUID id, String targetImage, String previousImage) {
        this.id = id;
        this.targetImage = targetImage;
        this.previousImage = previousImage;
        this.status = "queued";
        this.phase = "validating";
        this.message = "Rollback request accepted.";
        this.requestedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTargetImage() {
        return targetImage;
    }

    public String getPreviousImage() {
        return previousImage;
    }

    public String getStatus() {
        return status;
    }

    public String getPhase() {
        return phase;
    }

    public String getMessage() {
        return message;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    void update(String status, String phase, String message) {
        this.status = status;
        this.phase = phase;
        this.message = message;
        if ("succeeded".equals(status) || "failed".equals(status)) {
            this.completedAt = Instant.now();
        }
    }
}
