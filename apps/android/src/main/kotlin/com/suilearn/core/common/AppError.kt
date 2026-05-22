package com.suilearn.core.common

sealed interface AppError {
    data class ImportError(
        val message: String,
        val cause: Throwable? = null,
    ) : AppError

    data class DataError(
        val message: String,
        val cause: Throwable? = null,
    ) : AppError

    data class ValidationError(
        val message: String,
    ) : AppError
}

