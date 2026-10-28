package com.jconbrazil.cfp;

// CDI event that starts a SubmissionReviewAgent workflow.
public record ReviewRequested(long submissionId) {
}
