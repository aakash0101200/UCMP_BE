package com.ucmp.ucmp_backend.model;

public enum PreferenceType {
    PREFERRED,  // Faculty prefers to teach in this slot (soft: +3 score)
    BLOCKED     // Faculty cannot teach in this slot (soft: -5 score, admin can override)
}
