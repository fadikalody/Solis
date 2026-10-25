package com.solis.tracker.core.common

sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val error: AppError) : Result<Nothing>
    data object Loading : Result<Nothing>
}

sealed interface AppError {
    data class DatabaseError(val message: String, val throwable: Throwable? = null) : AppError
    data class ValidationError(val message: String) : AppError
    data class SecurityError(val message: String) : AppError
    data class IoError(val message: String, val throwable: Throwable? = null) : AppError
    data class UnknownError(val message: String, val throwable: Throwable? = null) : AppError
}
