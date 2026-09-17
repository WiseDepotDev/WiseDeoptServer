package com.huicang.wise.domain.sync;

public enum ConflictResolutionStrategy {
    LAST_WRITE_WINS,
    SERVER_WINS,
    CLIENT_WINS,
    MANUAL_RESOLUTION,
    MERGE
}