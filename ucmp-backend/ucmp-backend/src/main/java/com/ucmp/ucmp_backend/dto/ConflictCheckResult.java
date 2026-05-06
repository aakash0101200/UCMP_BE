package com.ucmp.ucmp_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for the conflict validation endpoint.
 * Returns whether an entry is safe to save, and if not, exactly what conflicts exist.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictCheckResult {

    private boolean hasConflicts;

    // Human-readable conflict messages for the frontend to display
    private List<String> conflicts;

    // Convenience factory methods
    public static ConflictCheckResult clean() {
        return ConflictCheckResult.builder()
                .hasConflicts(false)
                .conflicts(List.of())
                .build();
    }

    public static ConflictCheckResult withConflicts(List<String> conflicts) {
        return ConflictCheckResult.builder()
                .hasConflicts(true)
                .conflicts(conflicts)
                .build();
    }
}
