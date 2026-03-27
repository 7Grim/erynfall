package com.osrs.auth.model;

public record CharacterRecord(
    int id,
    int accountId,
    String characterName,
    boolean active
) {
}
