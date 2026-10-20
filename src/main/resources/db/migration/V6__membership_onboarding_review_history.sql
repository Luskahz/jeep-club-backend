-- Independent, append-only review evidence. Identifiers deliberately have no FK to
-- operational User/Application rows: safe deletion must retain the original review.
-- No raw tokens, links, contact data or free-text personal data are copied here.
CREATE TABLE IF NOT EXISTS membership_application_review_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    operation VARCHAR(40) NOT NULL,
    previous_status VARCHAR(20) NOT NULL,
    resulting_status VARCHAR(20) NOT NULL,
    previous_reviewer_id BIGINT NULL,
    previous_reviewed_at DATETIME(6) NULL,
    previous_finished_at DATETIME(6) NULL,
    original_user_id BIGINT NULL,
    actor_id BIGINT NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_membership_review_application (application_id, id)
);
