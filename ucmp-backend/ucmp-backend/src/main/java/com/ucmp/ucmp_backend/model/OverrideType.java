package com.ucmp.ucmp_backend.model;

public enum OverrideType {
    CANCELLED,       // Class is cancelled for a specific date
    MOVED,           // Class moved to a different day/time/room
    SUBSTITUTE,      // Different faculty teaches this class for a date range
    MAKEUP,          // A makeup class for a previously cancelled session
    GUEST_LECTURE    // External speaker taking the slot
}
