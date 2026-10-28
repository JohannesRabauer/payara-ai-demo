package com.jconbrazil.cfp;

import jakarta.ai.agent.LLMException;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/*
 * Small local models often wrap their JSON in ```json fences or add a sentence around it.
 * LargeLanguageModel.query(prompt, Class) hands the raw reply to JSON-B and fails on that,
 * so we cut out the JSON object ourselves before binding it.
 */
final class LlmJson {

    private static final Jsonb JSONB = JsonbBuilder.create();

    private LlmJson() {
    }

    static <T> T parse(String reply, Class<T> type) {
        if (reply == null) {
            throw new LLMException("The model returned no reply");
        }
        int start = reply.indexOf('{');
        int end = reply.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new LLMException("The model reply contains no JSON object: " + reply);
        }
        try {
            return JSONB.fromJson(reply.substring(start, end + 1), type);
        } catch (RuntimeException e) {
            throw new LLMException("Could not read the model reply as " + type.getSimpleName() + ": " + reply, e);
        }
    }
}
