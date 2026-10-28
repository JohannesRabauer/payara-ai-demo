package com.jconbrazil.cfp;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.NotificationOptions;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmissionResourceTest {

    private final RecordingEvent events = new RecordingEvent();
    private final SubmissionResource resource = resource();

    @Test
    void listsSeededSubmissions() {
        assertEquals(3, resource.list().size());
    }

    @Test
    void createAddsSubmission() {
        Response response = resource.create(new NewSubmission("Dani", "dani@example.com",
                "Records everywhere", "An abstract.", Track.CORE_JAVA, Level.BEGINNER, Format.TALK_45_MIN));

        assertEquals(201, response.getStatus());
        Submission created = (Submission) response.getEntity();
        assertEquals(4, created.id());
        assertNull(created.review());
        assertEquals(4, resource.list().size());
    }

    @Test
    void reviewFiresTheAgentTriggerAndReturnsTheReviewedSubmission() {
        // Stands in for the agent: the real workflow runs synchronously inside fire().
        events.onFire = request -> resource.store.updateReview(request.submissionId(),
                new Review(Review.Verdict.ACCEPTED, "Score 9/10. Great."));

        Response response = resource.review(1);

        assertEquals(200, response.getStatus());
        assertEquals(List.of(new ReviewRequested(1)), events.fired);
        assertEquals(Review.Verdict.ACCEPTED, ((Submission) response.getEntity()).review().verdict());
    }

    @Test
    void reviewOfUnknownSubmissionIs404() {
        Response response = resource.review(99);

        assertEquals(404, response.getStatus());
        assertEquals(Map.of("message", "No submission with id 99"), response.getEntity());
        assertTrue(events.fired.isEmpty());
    }

    private SubmissionResource resource() {
        SubmissionResource resource = new SubmissionResource();
        resource.store = new SubmissionStore();
        resource.reviewRequested = events;
        return resource;
    }

    private static final class RecordingEvent implements Event<ReviewRequested> {

        final List<ReviewRequested> fired = new ArrayList<>();
        Consumer<ReviewRequested> onFire = request -> { };

        @Override
        public void fire(ReviewRequested event) {
            fired.add(event);
            onFire.accept(event);
        }

        @Override
        public <U extends ReviewRequested> CompletionStage<U> fireAsync(U event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <U extends ReviewRequested> CompletionStage<U> fireAsync(U event, NotificationOptions options) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Event<ReviewRequested> select(Annotation... qualifiers) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <U extends ReviewRequested> Event<U> select(Class<U> subtype, Annotation... qualifiers) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <U extends ReviewRequested> Event<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
            throw new UnsupportedOperationException();
        }
    }
}
