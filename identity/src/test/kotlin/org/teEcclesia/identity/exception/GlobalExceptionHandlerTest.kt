package org.teEcclesia.identity.exception

import com.google.common.truth.Truth.assertThat
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.http.HttpStatus
import org.teEcclesia.i18n.I18nService
import java.util.Locale

class GlobalExceptionHandlerTest {

    private lateinit var i18nService: I18nService
    private lateinit var handler: GlobalExceptionHandler

    @BeforeEach
    fun setUp() {
        val messageSource = ReloadableResourceBundleMessageSource().apply {
            setBasename("classpath:messages")
            setDefaultEncoding("UTF-8")
            setFallbackToSystemLocale(false)
            setDefaultLocale(Locale.forLanguageTag("ar"))
        }
        i18nService = I18nService(messageSource)
        handler = GlobalExceptionHandler(i18nService)
        LocaleContextHolder.setLocale(Locale.forLanguageTag("ar"))
    }

    @AfterEach
    fun tearDown() {
        LocaleContextHolder.resetLocaleContext()
    }

    @Test
    fun `handleUserAlreadyExists returns Arabic conflict message by default`() {
        val response = handler.handleUserAlreadyExists(UserAlreadyExistsException())
        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body?.message).isEqualTo("المستخدم مسجل بالفعل")
    }

    @Test
    fun `handleInvalidCredentials returns Arabic unauthorized message`() {
        val response = handler.handleInvalidCredentials(InvalidCredentialsException())
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(response.body?.message).isEqualTo("اسم المستخدم أو كلمة المرور غير صحيحة")
    }

    @Test
    fun `handleTokenExpired returns Arabic expired session message`() {
        val response = handler.handleTokenExpired(TokenExpiredException())
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(response.body?.message).isEqualTo("انتهت صلاحية الجلسة، يرجى تسجيل الدخول مرة أخرى")
    }

    @Test
    fun `handleUnauthorized returns Arabic forbidden message`() {
        val response = handler.handleUnauthorized(UnauthorizedException())
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(response.body?.message).isEqualTo("غير مصرح لك بالوصول")
    }

    @Test
    fun `handleIncompleteProfile returns Arabic profile incomplete message with tokens`() {
        val response = handler.handleIncompleteProfile(
            IncompleteProfileException(token = "temp-tok", refreshToken = "ref-tok")
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.PRECONDITION_REQUIRED)
        assertThat(response.body?.message).isEqualTo("الملف الشخصي غير مكتمل")
        assertThat(response.body?.token).isEqualTo("temp-tok")
        assertThat(response.body?.refreshToken).isEqualTo("ref-tok")
    }

    @Test
    fun `handlePhoneNotVerified returns Arabic phone not verified message`() {
        val response = handler.handlePhoneNotVerified(
            PhoneNotVerifiedException(token = "temp-tok", refreshToken = "ref-tok")
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.PRECONDITION_FAILED)
        assertThat(response.body?.message).isEqualTo("رقم الهاتف لم يتم تأكيده بعد")
    }

    @Test
    fun `handleEmailNotVerified returns Arabic email not verified message`() {
        val response = handler.handleEmailNotVerified(EmailNotVerifiedException())
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
        assertThat(response.body?.message).isEqualTo("البريد الإلكتروني لم يتم تأكيده بعد")
    }

    @Test
    fun `handleAccountPendingApproval returns Arabic pending approval message`() {
        val response = handler.handleAccountPendingApproval(
            AccountPendingApprovalException(token = "temp-tok", refreshToken = "ref-tok")
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.LOCKED)
        assertThat(response.body?.message).isEqualTo("حسابك قيد المراجعة والموافقة من الإدارة")
    }

    @Test
    fun `handleDuplicatePhone returns Arabic duplicate phone message`() {
        val response = handler.handleDuplicatePhone(DuplicatePhoneException())
        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body?.message).contains("رقم الهاتف هذا مرتبط بأكثر من حساب")
    }

    @Test
    fun `handleAccountDeleted returns Arabic account deleted message`() {
        val response = handler.handleAccountDeleted(AccountDeletedException())
        assertThat(response.statusCode).isEqualTo(HttpStatus.GONE)
        assertThat(response.body?.message).contains("تم حذف هذا الحساب مسبقاً ويمكنك إعادة تفعيله")
    }

    @Test
    fun `handleResourceNotFound returns Arabic not found message`() {
        val response = handler.handleResourceNotFound(ResourceNotFoundException())
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(response.body?.message).isEqualTo("العنصر المطلوب غير موجود")
    }

    @Test
    fun `handleBadRequestExceptions with EntityNotFoundException returns Arabic not found message`() {
        val response = handler.handleBadRequestExceptions(EntityNotFoundException("error.user.not_found"))
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(response.body?.message).isEqualTo("المستخدم غير موجود")
    }

    @Test
    fun `handleBadRequestExceptions with legacy English message resolves to Arabic`() {
        val response = handler.handleBadRequestExceptions(
            IllegalArgumentException("error.phone.same_as_current")
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.message).isEqualTo("يجب أن يكون رقم الهاتف الجديد مختلفاً عن الحالي.")
    }

    @Test
    fun `handleGenericException returns Arabic internal server error message`() {
        val response = handler.handleGenericException(RuntimeException("Boom"))
        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body?.message).isEqualTo("حدث خطأ غير متوقع في الخادم، يرجى المحاولة لاحقاً")
    }

    @Test
    fun `when English locale is active, responses are returned in English`() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("en"))

        val response = handler.handleUserAlreadyExists(UserAlreadyExistsException())
        assertThat(response.body?.message).isEqualTo("User already exists")

        val credResponse = handler.handleInvalidCredentials(InvalidCredentialsException())
        assertThat(credResponse.body?.message).isEqualTo("Invalid username or password")

        val genericResponse = handler.handleGenericException(RuntimeException("Crash"))
        assertThat(genericResponse.body?.message).isEqualTo("Internal Server Error")
    }
}
