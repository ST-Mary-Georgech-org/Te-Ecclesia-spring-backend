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
    message: String = "User account is pending approval") : RuntimeException(message)

class IncompleteProfileException(
    message: String = "User profile is incomplete") : RuntimeException(message)

class PhoneNotVerifiedException(
    message: String = "User phone number is not verified") : RuntimeException(message)

class ResourceNotFoundException(message: String) : RuntimeException(message)