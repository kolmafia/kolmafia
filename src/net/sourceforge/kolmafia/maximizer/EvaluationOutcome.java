package net.sourceforge.kolmafia.maximizer;

public record EvaluationOutcome(double score, boolean failed, boolean exceeded) {}
