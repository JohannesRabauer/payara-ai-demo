package com.jconbrazil.cfp;

// The model's answer to "does this talk belong at JCON Brazil?"
public record ScopeCheck(boolean inScope, String reason) {
}
