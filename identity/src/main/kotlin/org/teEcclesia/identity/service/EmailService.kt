package org.teEcclesia.identity.service

import org.teEcclesia.events.notifications.EmailEvent
import org.teEcclesia.events.publisher.TeEcclesiaEventPublisher
import org.springframework.stereotype.Service
import java.security.SecureRandom

@Service
class EmailService(
    private val publisher: TeEcclesiaEventPublisher,
) {
    fun sendOtp(email: String, otp: String) {
        val text = """
            Hello,
            
            you have requested to reset your password.
            Your OTP code is: $otp
            
            This code will expire in 15 minutes.
            If you did not request this, please ignore this email.
            
            Thanks,
            TeEcclesia Team
        """.trimIndent()

        val event = EmailEvent(
            to = email,
            subject = "TeEcclesia - Password Reset Code",
            text = text,
        )

        publisher.publish(event)
    }

    fun sendWelcomeVerificationOtp(email: String, otp: String) {
        val text = """
            Welcome to TeEcclesia!
            
            To complete your registration, please use the following OTP code: $otp
            
            This code will expire in 15 minutes.
            
            Thanks,
            TeEcclesia Team
        """.trimIndent()

        val event = EmailEvent(
            to = email,
            subject = "TeEcclesia - Welcome! Verify your email",
            text = text,
        )

        publisher.publish(event)
    }

    fun generateOtp(): String {
        val secureRandom = SecureRandom()
        val number = secureRandom.nextInt(10000)
        return String.format("%05d", number)
    }

}