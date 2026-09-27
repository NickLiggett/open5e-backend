package com.main.app.item;

/** @param rank 1 (common) to 6 (artifact) */
public record RarityReference(String key, String name, Integer rank) {
}
