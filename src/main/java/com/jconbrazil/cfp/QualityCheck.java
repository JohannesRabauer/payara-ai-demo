package com.jconbrazil.cfp;

// The model's rating of an in-scope talk: 1 (poor) to 10 (excellent).
public record QualityCheck(int score, String feedback) {
}
