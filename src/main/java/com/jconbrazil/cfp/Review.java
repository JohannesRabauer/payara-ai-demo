package com.jconbrazil.cfp;

// The reviewer agent's result, stored on the Submission.
public record Review(Verdict verdict, String reason) {

    public enum Verdict {
        OUT_OF_SCOPE,
        REJECTED,
        ACCEPTED,
        FAILED
    }
}
