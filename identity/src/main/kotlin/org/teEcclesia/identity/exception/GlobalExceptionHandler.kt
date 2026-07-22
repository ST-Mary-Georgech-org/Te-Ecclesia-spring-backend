package org.teEcclesia.identity.exception

import org.slf4j.LoggerFactory
import org.teEcclesia.identity.api.dto.response.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import org.teEcclesia.identity.api.dto.response.IncompleteProfileResponse


@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(UserAlreadyExistsException::class)
    fun handleUserAlreadyExists(ex: UserAlreadyExistsException): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(ex.message ?: "Conflict", HttpStatus.CONFLICT.value())
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error)
    }

    @ExceptionHandler(InvalidCredentialsException::class, BadCredentialsException::class)
    fun handleInvalidCredentials(ex: Exception): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse("Invalid email or password", HttpStatus.UNAUTHORIZED.value())
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error)
    }

    @ExceptionHandler(TokenExpiredException::class)
    fun handleTokenExpired(ex: TokenExpiredException): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(ex.message ?: "Token expired", HttpStatus.UNAUTHORIZED.value())
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error)
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(ex: UnauthorizedException): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(ex.message ?: "Unauthorized", HttpStatus.FORBIDDEN.value())
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error)
    }

    @ExceptionHandler(IncompleteProfileException::class)
    fun handleIncompleteProfile(ex: IncompleteProfileException): ResponseEntity<IncompleteProfileResponse> {
        val error = IncompleteProfileResponse(
            message = ex.message ?: "Profile incomplete",
            status = HttpStatus.PRECONDITION_REQUIRED.value(),
            token = ex.token,
            refreshToken = ex.refreshToken
        )
        return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).body(error)
    }

    @ExceptionHandler(PhoneNotVerifiedException::class)
    fun handlePhoneNotVerified(ex: PhoneNotVerifiedException): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(ex.message ?: "Phone not verified", HttpStatus.PRECONDITION_FAILED.value())
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(error)
    }

    @ExceptionHandler(AccountPendingApprovalException::class)
    fun handleAccountPendingApproval(ex: AccountPendingApprovalException): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(ex.message ?: "Account pending approval", HttpStatus.LOCKED.value())
        return ResponseEntity.status(HttpStatus.LOCKED).body(error)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errorMessage = ex.bindingResult.allErrors.firstOrNull()?.defaultMessage ?: "Validation failed"
        val error = ErrorResponse(errorMessage, HttpStatus.BAD_REQUEST.value())
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<ErrorResponse> {
        if (ex.statusCode == HttpStatus.PAYMENT_REQUIRED) {
            val error = ErrorResponse(
                message = ex.reason ?: "Payment required",
                status = HttpStatus.PAYMENT_REQUIRED.value()
            )
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(error)
        }

        return handleGenericException(ex)
    }

    @ExceptionHandler(DuplicatePhoneException::class)
    fun handleDuplicatePhone(ex: DuplicatePhoneException): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(ex.message ?: "Conflict", HttpStatus.CONFLICT.value())
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error occurred", ex)
        val error = ErrorResponse(ex.message ?: "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR.value()) // 500
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }
}