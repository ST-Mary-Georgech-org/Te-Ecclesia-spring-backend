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
import org.teEcclesia.i18n.I18nService
import jakarta.persistence.EntityNotFoundException
import org.teEcclesia.storage.exception.InvalidImageException
import org.teEcclesia.storage.exception.UploadImageException

@RestControllerAdvice
class GlobalExceptionHandler(
    private val i18nService: I18nService
) {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    private fun resolveMessage(rawMessage: String?, defaultKey: String, fallbackArabic: String): String {
        if (rawMessage.isNullOrBlank()) {
            return i18nService.getMessage(defaultKey, defaultMessage = fallbackArabic)
        }
        val clean = rawMessage.trim().removeSurrounding("{", "}")
        val translated = i18nService.getMessage(clean, defaultMessage = "")
        if (translated.isNotBlank() && translated != clean) {
            return translated
        }
        if (clean.any { it in '\u0600'..'\u06FF' }) {
            return clean
        }
        return i18nService.getMessage(defaultKey, defaultMessage = fallbackArabic)
    }

    @ExceptionHandler(UserAlreadyExistsException::class)
    fun handleUserAlreadyExists(ex: UserAlreadyExistsException): ResponseEntity<ErrorResponse> {
        val message = resolveMessage(ex.message, "error.auth.user_already_exists", "المستخدم مسجل بالفعل")
        val error = ErrorResponse(message, HttpStatus.CONFLICT.value())
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error)
    }

    @ExceptionHandler(InvalidCredentialsException::class, BadCredentialsException::class)
    fun handleInvalidCredentials(ex: Exception): ResponseEntity<ErrorResponse> {
        val message = resolveMessage(ex.message, "error.auth.invalid_credentials", "اسم المستخدم أو كلمة المرور غير صحيحة")
        val error = ErrorResponse(message, HttpStatus.UNAUTHORIZED.value())
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error)
    }

    @ExceptionHandler(TokenExpiredException::class)
    fun handleTokenExpired(ex: TokenExpiredException): ResponseEntity<ErrorResponse> {
        val message = resolveMessage(ex.message, "error.auth.token_expired", "انتهت صلاحية الجلسة، يرجى تسجيل الدخول مرة أخرى")
        val error = ErrorResponse(message, HttpStatus.UNAUTHORIZED.value())
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error)
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(ex: UnauthorizedException): ResponseEntity<ErrorResponse> {
        val message = resolveMessage(ex.message, "error.auth.unauthorized", "غير مصرح لك بالوصول")
        val error = ErrorResponse(message, HttpStatus.FORBIDDEN.value())
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error)
    }

    @ExceptionHandler(IncompleteProfileException::class)
    fun handleIncompleteProfile(ex: IncompleteProfileException): ResponseEntity<IncompleteProfileResponse> {
        val message = resolveMessage(ex.message, "error.auth.profile_incomplete", "الملف الشخصي غير مكتمل")
        val error = IncompleteProfileResponse(
            message = message,
            status = HttpStatus.PRECONDITION_REQUIRED.value(),
            token = ex.token,
            refreshToken = ex.refreshToken
        )
        return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).body(error)
    }

    @ExceptionHandler(PhoneNotVerifiedException::class)
    fun handlePhoneNotVerified(ex: PhoneNotVerifiedException): ResponseEntity<IncompleteProfileResponse> {
        val message = resolveMessage(ex.message, "error.auth.phone_not_verified", "رقم الهاتف لم يتم تأكيده بعد")
        val error = IncompleteProfileResponse(
            message = message,
            status = HttpStatus.PRECONDITION_FAILED.value(),
            token = ex.token,
            refreshToken = ex.refreshToken
        )
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(error)
    }

    @ExceptionHandler(EmailNotVerifiedException::class)
    fun handleEmailNotVerified(ex: EmailNotVerifiedException): ResponseEntity<ErrorResponse> {
        val message = resolveMessage(ex.message, "error.auth.email_not_verified", "البريد الإلكتروني لم يتم تأكيده بعد")
        val error = ErrorResponse(message, HttpStatus.UNPROCESSABLE_ENTITY.value())
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error)
    }

    @ExceptionHandler(AccountPendingApprovalException::class)
    fun handleAccountPendingApproval(ex: AccountPendingApprovalException): ResponseEntity<IncompleteProfileResponse> {
        val message = resolveMessage(ex.message, "error.auth.account_pending_approval", "حسابك قيد المراجعة والموافقة من الإدارة")
        val error = IncompleteProfileResponse(
            message = message,
            status = HttpStatus.LOCKED.value(),
            token = ex.token,
            refreshToken = ex.refreshToken
        )
        return ResponseEntity.status(HttpStatus.LOCKED).body(error)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val rawMessage = ex.bindingResult.allErrors.firstOrNull()?.defaultMessage
        val message = resolveMessage(rawMessage, "validation.failed", "فشل التحقق من صحة البيانات المدخلة")
        val error = ErrorResponse(message, HttpStatus.BAD_REQUEST.value())
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<ErrorResponse> {
        if (ex.statusCode == HttpStatus.PAYMENT_REQUIRED) {
            val message = resolveMessage(ex.reason, "error.payment_required", "الدفع مطلوب للمتابعة")
            val error = ErrorResponse(
                message = message,
                status = HttpStatus.PAYMENT_REQUIRED.value()
            )
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(error)
        }

        return handleGenericException(ex)
    }

    @ExceptionHandler(DuplicatePhoneException::class)
    fun handleDuplicatePhone(ex: DuplicatePhoneException): ResponseEntity<ErrorResponse> {
        val message = resolveMessage(ex.message, "error.auth.duplicate_phone", "رقم الهاتف هذا مرتبط بأكثر من حساب. يرجى استخدام الرقم القومي لإعادة ضبط كلمة المرور.")
        val error = ErrorResponse(message, HttpStatus.CONFLICT.value())
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error)
    }

    @ExceptionHandler(AccountDeletedException::class)
    fun handleAccountDeleted(ex: AccountDeletedException): ResponseEntity<ErrorResponse> {
        val message = resolveMessage(ex.message, "error.auth.account_deleted", "تم حذف هذا الحساب مسبقاً ويمكنك إعادة تفعيله.")
        val error = ErrorResponse(message, HttpStatus.GONE.value())
        return ResponseEntity.status(HttpStatus.GONE).body(error)
    }

    @ExceptionHandler(ResourceNotFoundException::class, UserNotFoundException::class)
    fun handleResourceNotFound(ex: Exception): ResponseEntity<ErrorResponse> {
        val defaultKey = if (ex is UserNotFoundException) "error.user.not_found" else "error.resource_not_found"
        val fallback = if (ex is UserNotFoundException) "المستخدم غير موجود" else "العنصر المطلوب غير موجود"
        val message = resolveMessage(ex.message, defaultKey, fallback)
        val error = ErrorResponse(message, HttpStatus.NOT_FOUND.value())
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error)
    }

    @ExceptionHandler(IncorrectPasswordException::class)
    fun handleIncorrectPassword(ex: IncorrectPasswordException): ResponseEntity<ErrorResponse> {
        val message = resolveMessage(ex.message, "error.auth.incorrect_password", "كلمة المرور الحالية غير صحيحة")
        val error = ErrorResponse(message, HttpStatus.BAD_REQUEST.value())
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    @ExceptionHandler(UploadImageException::class)
    fun handleUploadImage(ex: UploadImageException): ResponseEntity<ErrorResponse> {
        val defaultKey = if (ex is InvalidImageException) "error.storage.invalid_extension" else "error.storage.upload_failed"
        val fallback = if (ex is InvalidImageException) "صيغة الملف غير مدعومة" else "فشل رفع الملف، يرجى المحاولة لاحقاً"
        val message = resolveMessage(ex.message, defaultKey, fallback)
        val error = ErrorResponse(message, HttpStatus.BAD_REQUEST.value())
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    @ExceptionHandler(
        IllegalArgumentException::class,
        IllegalStateException::class,
        EntityNotFoundException::class,
        NoSuchElementException::class,
        RuntimeException::class
    )
    fun handleBadRequestExceptions(ex: Exception): ResponseEntity<ErrorResponse> {
        if (ex is NullPointerException) {
            return handleGenericException(ex)
        }
        val defaultKey = when (ex) {
            is UserNotFoundException -> "error.user.not_found"
            is EntityNotFoundException, is NoSuchElementException -> "error.resource_not_found"
            else -> "error.bad_request"
        }
        val fallback = when (ex) {
            is UserNotFoundException -> "المستخدم غير موجود"
            is EntityNotFoundException, is NoSuchElementException -> "العنصر المطلوب غير موجود"
            else -> "طلب غير صالح"
        }
        val message = resolveMessage(ex.message, defaultKey, fallback)
        val status = when (ex) {
            is EntityNotFoundException, is NoSuchElementException, is UserNotFoundException -> HttpStatus.NOT_FOUND
            else -> HttpStatus.BAD_REQUEST
        }
        val error = ErrorResponse(message, status.value())
        return ResponseEntity.status(status).body(error)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected server error occurred", ex)
        val message = i18nService.getMessage("error.server.internal", defaultMessage = "حدث خطأ غير متوقع في الخادم، يرجى المحاولة لاحقاً")
        val error = ErrorResponse(message, HttpStatus.INTERNAL_SERVER_ERROR.value())
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }
}