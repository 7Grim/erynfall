package com.osrs.auth.model;

public record AccountRecord(
    int id,
    String email,
    String passwordHash,
    boolean emailVerified,
    int status
) {
}
