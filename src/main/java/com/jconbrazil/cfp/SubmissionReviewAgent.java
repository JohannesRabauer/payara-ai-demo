package com.jconbrazil.cfp;

import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.HandleException;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Result;
import jakarta.ai.agent.Trigger;
import jakarta.ai.agent.WorkflowScoped;
import jakarta.inject.Inject;

import java.util.logging.Logger;

/*
 * Reviews a CFP submission in two LLM-backed steps:
 *   @Trigger  ReviewRequested -> load the Submission into the workflow context
 *   @Decision 1: is the talk in scope for JCON Brazil?   no -> OUT_OF_SCOPE, workflow stops
 *   @Decision 2: is the talk good enough to accept?      no -> REJECTED,     workflow stops
 *   @Outcome  ACCEPTED
 *
 * A false decision ends the workflow without running the outcome, so each decision
 * stores its own negative verdict before it returns.
 */
// @WorkflowScoped is the default scope of an @Agent, but it is declared here because @Agent alone is
// not a bean-defining annotation: without a beans.xml (discovery mode "annotated") CDI would not see this class.
@Agent(name = "submissionReviewer",
        description = "Checks that a CFP submission fits JCON Brazil 2030 and decides whether to accept it.")
@WorkflowScoped
public class SubmissionReviewAgent {

    private static final Logger LOGGER = Logger.getLogger(SubmissionReviewAgent.class.getName());

    static final int ACCEPT_THRESHOLD = 7;

    static final String CONFERENCE_SCOPE = """
            JCON Brazil 2030 is a community conference for Java developers.
            In scope: the Java language and the JVM (including Kotlin, Scala and other JVM languages), \
            Jakarta EE and MicroProfile, frameworks and libraries of the Java ecosystem, \
            AI and LLMs used from Java, cloud, containers and DevOps for Java applications, \
            and frontends that are built together with Java backends.
            Out of scope: topics with no connection to Java or the JVM, product or sales pitches, \
            recruiting, and non-technical talks.""";

    private static final String SCOPE_PROMPT = """
            You are on the program committee of a developer conference.

            Conference scope:
            {}

            Talk proposal:
            {}

            Is this talk proposal in scope for the conference? Judge only the topic, not the quality.
            Answer with JSON only, no other text, in exactly this shape:
            {"inScope": true, "reason": "one or two sentences"}""";

    private static final String QUALITY_PROMPT = """
            You are on the program committee of JCON Brazil 2030, a Java developer conference.
            The following talk proposal is in scope. Decide whether it is good enough to accept.

            Talk proposal:
            {}

            Rate the proposal from 1 (poor) to 10 (excellent). A good proposal has a clear, specific title,
            an abstract that says concretely what the audience will learn, content that matches the
            chosen level and fits the chosen format, and no dependency on things that may fail on stage.
            Answer with JSON only, no other text, in exactly this shape:
            {"score": 7, "feedback": "two or three sentences for the speaker"}""";

    @Inject
    SubmissionStore store;

    @Trigger
    Submission load(ReviewRequested request) {
        LOGGER.info(() -> "[TRIGGER] review requested for submission " + request.submissionId());
        return store.find(request.submissionId())
                .orElseThrow(() -> new IllegalArgumentException("No submission " + request.submissionId()));
    }

    @Decision(order = 1)
    Result checkScope(Submission submission, LargeLanguageModel model) {
        ScopeCheck check = LlmJson.parse(
                model.query(SCOPE_PROMPT, CONFERENCE_SCOPE, describe(submission)), ScopeCheck.class);
        LOGGER.info(() -> "[DECISION] scope: inScope=" + check.inScope() + " - " + check.reason());
        if (!check.inScope()) {
            store.updateReview(submission.id(), new Review(Review.Verdict.OUT_OF_SCOPE, check.reason()));
        }
        return new Result(check.inScope(), check);
    }

    @Decision(order = 2)
    Result checkQuality(Submission submission, LargeLanguageModel model) {
        QualityCheck check = LlmJson.parse(
                model.query(QUALITY_PROMPT, describe(submission)), QualityCheck.class);
        boolean accept = isAccepted(check);
        LOGGER.info(() -> "[DECISION] quality: score=" + check.score() + " accept=" + accept);
        if (!accept) {
            store.updateReview(submission.id(), new Review(Review.Verdict.REJECTED, summary(check)));
        }
        return new Result(accept, check);
    }

    // The engine also runs the outcome after a handled exception, so only accept with a passing QualityCheck.
    @Outcome
    void accept(Submission submission, QualityCheck quality) {
        if (submission == null || !isAccepted(quality)) {
            return;
        }
        LOGGER.info(() -> "[OUTCOME] submission " + submission.id() + " accepted");
        store.updateReview(submission.id(), new Review(Review.Verdict.ACCEPTED, summary(quality)));
    }

    @HandleException
    void failed(Exception exception, Submission submission) {
        LOGGER.warning(() -> "[EXCEPTION] review failed: " + exception);
        if (submission != null) {
            store.updateReview(submission.id(),
                    new Review(Review.Verdict.FAILED, "The reviewer agent failed: " + exception.getMessage()));
        }
    }

    private static boolean isAccepted(QualityCheck check) {
        return check != null && check.score() >= ACCEPT_THRESHOLD;
    }

    private static String summary(QualityCheck check) {
        return "Score " + check.score() + "/10. " + check.feedback();
    }

    // Only the talk itself goes to the model, not the speaker's name or e-mail.
    static String describe(Submission submission) {
        return """
                Title: %s
                Track: %s
                Level: %s
                Format: %s
                Abstract: %s""".formatted(
                submission.talkTitle(), submission.track(), submission.level(),
                submission.format(), submission.abstractText());
    }
}
