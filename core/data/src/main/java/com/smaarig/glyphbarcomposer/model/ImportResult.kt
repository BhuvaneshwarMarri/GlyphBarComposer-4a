package com.smaarig.glyphbarcomposer.model

/**
 * Result of a sequence import operation.
 */
sealed class ImportResult {
    /**
     * Successfully parsed sequence data.
     * @param name The name of the sequence as defined in the source file.
     * @param steps The list of glyph sequences representing each step.
     */
    data class Success(
        val name: String,
        val steps: List<GlyphSequence>
    ) : ImportResult()

    /**
     * Failed to parse sequence data due to one or more errors.
     * @param errors List of errors encountered during parsing.
     */
    data class Failure(
        val errors: List<ImportError>
    ) : ImportResult()
}

/**
 * Detailed error information for an import failure.
 * @param code Unique error code (e.g., "JSON_MISSING_ROOT").
 * @param message Human-readable error message.
 * @param line Optional 1-based line number (primarily for CSV errors).
 */
data class ImportError(
    val code: String,
    val message: String,
    val line: Int? = null
)
