package org.teEcclesia.identity.exception

abstract class AuthenticationException(message: String) : RuntimeException(message)

class InvalidCredentialsException(
    message: String = "Invalid username or password") : AuthenticationException(message)


class TokenExpiredException (
    message: String = "Token has expired. Please login again.") : AuthenticationException(message)


class UnauthorizedException (
    message: String = "Unauthorized access") : AuthenticationException(message)

class UserAlreadyExistsException(
    message: String = "User already exists") : AuthenticationException(message)

class UserNotFoundException(message: String) : AuthenticationException(message)

class AccountPendingApprovalException(
    message: String = "User account is pending approval",
    val token: String? = null,
    val refreshToken: String? = null
) : RuntimeException(message)

class IncompleteProfileException(
    message: String = "User profile is incomplete",
    val token: String? = null,
    val refreshToken: String? = null
) : RuntimeException(message)

class PhoneNotVerifiedException(
    message: String = "User phone number is not verified",
    val token: String? = null,
    val refreshToken: String? = null
) : RuntimeException(message)

class EmailNotVerifiedException(
    message: String = "User email is not verified"
) : RuntimeException(message)

class ResourceNotFoundException(message: String) : RuntimeException(message)

class DuplicatePhoneException(
    message: String = "This phone number is associated with multiple accounts. Please use your National ID to reset your password."
) : RuntimeException(message)

class AccountDeletedException(
    message: String = "Account has been deleted and can be reactivated"
) : RuntimeException(message)

class IncorrectPasswordException(
    message: String = "Invalid password"
) : RuntimeException(message)
