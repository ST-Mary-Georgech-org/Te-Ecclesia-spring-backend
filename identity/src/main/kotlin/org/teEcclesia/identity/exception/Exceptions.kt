package org.teEcclesia.identity.exception

abstract class AuthenticationException(message: String) : RuntimeException(message)

class InvalidCredentialsException(
    message: String = "error.auth.invalid_credentials"
) : AuthenticationException(message)

class TokenExpiredException(
    message: String = "error.auth.token_expired"
) : AuthenticationException(message)

class UnauthorizedException(
    message: String = "error.auth.unauthorized"
) : AuthenticationException(message)

class UserAlreadyExistsException(
    message: String = "error.auth.user_already_exists"
) : AuthenticationException(message)

class UserNotFoundException(
    message: String = "error.user.not_found"
) : AuthenticationException(message)

class AccountPendingApprovalException(
    message: String = "error.auth.account_pending_approval",
    val token: String? = null,
    val refreshToken: String? = null
) : RuntimeException(message)

class IncompleteProfileException(
    message: String = "error.auth.profile_incomplete",
    val token: String? = null,
    val refreshToken: String? = null
) : RuntimeException(message)

class PhoneNotVerifiedException(
    message: String = "error.auth.phone_not_verified",
    val token: String? = null,
    val refreshToken: String? = null
) : RuntimeException(message)

class EmailNotVerifiedException(
    message: String = "error.auth.email_not_verified"
) : RuntimeException(message)

class ResourceNotFoundException(
    message: String = "error.resource_not_found"
) : RuntimeException(message)

class DuplicatePhoneException(
    message: String = "error.auth.duplicate_phone"
) : RuntimeException(message)

class AccountDeletedException(
    message: String = "error.auth.account_deleted"
) : RuntimeException(message)

class IncorrectPasswordException(
    message: String = "error.auth.incorrect_password"
) : RuntimeException(message)
