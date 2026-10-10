package com.jconbrazil.cfp;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// What the form sends. The server adds id, submittedAt and review.
public record NewSubmission(
        @NotBlank String speakerName,
        @NotBlank @Email String email,
        @NotBlank String talkTitle,
        @JsonbProperty("abstract") @NotBlank @Size(max = 2000) String abstractText,
        @NotNull Track track,
        @NotNull Level level,
        @NotNull Format format) {
}
