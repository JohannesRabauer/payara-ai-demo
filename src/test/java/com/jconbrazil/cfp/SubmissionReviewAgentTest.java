package com.jconbrazil.cfp;

import jakarta.ai.agent.LLMException;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Result;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Calls the phases in the same order as the workflow engine, with a scripted LLM.
class SubmissionReviewAgentTest {

    private final SubmissionStore store = new SubmissionStore();
    private final SubmissionReviewAgent agent = agent(store);

    @Test
    void triggerLoadsTheSubmission() {
        assertEquals("Jakarta Data from Zero to Repository", agent.load(new ReviewRequested(2)).talkTitle());
    }

    @Test
    void outOfScopeStopsTheWorkflowAndStoresTheVerdict() {
        Submission submission = agent.load(new ReviewRequested(1));
        ScriptedModel model = new ScriptedModel("""
                ```json
                {"inScope": false, "reason": "This is a sales pitch."}
                ```""");

        Result result = agent.checkScope(submission, model);

        assertFalse(result.success());
        assertEquals(new Review(Review.Verdict.OUT_OF_SCOPE, "This is a sales pitch."), review(1));
        assertTrue(model.prompts.getFirst().contains("Virtual Threads in Production"));
        assertFalse(model.prompts.getFirst().contains("ana@example.com"), "personal data must not reach the model");
    }

    @Test
    void inScopeProceedsWithoutStoringAReview() {
        Submission submission = agent.load(new ReviewRequested(1));

        Result result = agent.checkScope(submission,
                new ScriptedModel("{\"inScope\": true, \"reason\": \"Core Java.\"}"));

        assertTrue(result.success());
        assertEquals(new ScopeCheck(true, "Core Java."), result.details());
        assertNull(review(1));
    }

    @Test
    void lowScoreRejects() {
        Submission submission = agent.load(new ReviewRequested(3));

        Result result = agent.checkQuality(submission,
                new ScriptedModel("Sure! {\"score\": 4, \"feedback\": \"Too vague.\"}"));

        assertFalse(result.success());
        assertEquals(new Review(Review.Verdict.REJECTED, "Score 4/10. Too vague."), review(3));
    }

    @Test
    void highScoreProceedsToTheOutcomeWhichAccepts() {
        Submission submission = agent.load(new ReviewRequested(1));

        Result result = agent.checkQuality(submission,
                new ScriptedModel("{\"score\": 9, \"feedback\": \"Concrete and useful.\"}"));
        assertTrue(result.success());
        assertNull(review(1));

        agent.accept(submission, (QualityCheck) result.details());
        assertEquals(new Review(Review.Verdict.ACCEPTED, "Score 9/10. Concrete and useful."), review(1));
    }

    @Test
    void outcomeAfterAHandledExceptionDoesNotAccept() {
        Submission submission = agent.load(new ReviewRequested(1));

        agent.failed(new LLMException("Ollama HTTP 500"), submission);
        agent.accept(submission, null);

        assertEquals(new Review(Review.Verdict.FAILED, "The reviewer agent failed: Ollama HTTP 500"), review(1));
    }

    @Test
    void unreadableReplyIsAnLlmException() {
        Submission submission = agent.load(new ReviewRequested(1));

        assertThrows(LLMException.class,
                () -> agent.checkScope(submission, new ScriptedModel("I think it is in scope.")));
    }

    private Review review(long id) {
        return store.find(id).orElseThrow().review();
    }

    private static SubmissionReviewAgent agent(SubmissionStore store) {
        SubmissionReviewAgent agent = new SubmissionReviewAgent();
        agent.store = store;
        return agent;
    }

    // Returns canned replies and records the prompts after placeholder substitution (Strings only).
    private static final class ScriptedModel implements LargeLanguageModel {

        private final Deque<String> replies;
        final List<String> prompts = new ArrayList<>();

        ScriptedModel(String... replies) {
            this.replies = new ArrayDeque<>(List.of(replies));
        }

        @Override
        public String query(String prompt) {
            prompts.add(prompt);
            return replies.removeFirst();
        }

        @Override
        public String query(String prompt, Object... parameters) {
            String filled = prompt;
            for (Object parameter : parameters) {
                filled = filled.replaceFirst("\\{\\}", java.util.regex.Matcher.quoteReplacement(String.valueOf(parameter)));
            }
            return query(filled);
        }

        @Override
        public <T> T query(String prompt, Class<T> resultType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T query(String prompt, Class<T> resultType, Object... parameters) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T unwrap(Class<T> implClass) {
            throw new UnsupportedOperationException();
        }
    }
}
