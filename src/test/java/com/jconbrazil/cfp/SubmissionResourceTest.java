package com.jconbrazil.cfp;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SubmissionResourceTest {

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
    void reviewIsNotImplementedYet() {
        Response response = resource.review(1);

        assertEquals(501, response.getStatus());
        assertEquals(Map.of("message", "Reviewer agent not built yet"), response.getEntity());
    }

    private static SubmissionResource resource() {
        SubmissionResource resource = new SubmissionResource();
        resource.store = new SubmissionStore();
        return resource;
    }
}
