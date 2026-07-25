package org.teEcclesia.notifications.service

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.teEcclesia.events.identity.DeviceTokenUnregisteredEvent
import org.teEcclesia.events.publisher.TeEcclesiaEventPublisher

class PushNotificationServiceTest {

    @MockK
    private lateinit var eventPublisher: TeEcclesiaEventPublisher

    @MockK
    private lateinit var firebaseMessaging: FirebaseMessaging

    @InjectMockKs
    private lateinit var pushNotificationService: PushNotificationService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        mockkStatic(FirebaseMessaging::class)
        every { FirebaseMessaging.getInstance() } returns firebaseMessaging
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(FirebaseMessaging::class)
    }

    @Test
    fun `sendPushNotificationToTokens should send successfully`() {
        val tokens = listOf("token1", "token2")
        val title = "Test Title"
        val body = "Test Body"
        val payload = mapOf("key" to "value")
        
        every { firebaseMessaging.send(any<Message>()) } returns "message-id"
        
        pushNotificationService.sendPushNotificationToTokens(tokens, title, body, payload)
        
        verify(exactly = 2) { firebaseMessaging.send(any<Message>()) }
        verify(exactly = 0) { eventPublisher.publish(any()) }
    }

    @Test
    fun `sendPushNotificationToTokens should publish event when unregistered token`() {
        val tokens = listOf("bad-token")
        val title = "Test Title"
        val body = "Test Body"
        
        val exception = mockk<FirebaseMessagingException>()
        every { exception.messagingErrorCode } returns MessagingErrorCode.UNREGISTERED
        every { firebaseMessaging.send(any<Message>()) } throws exception
        
        every { eventPublisher.publish(any()) } just Runs
        
        pushNotificationService.sendPushNotificationToTokens(tokens, title, body)
        
        verify(exactly = 1) { firebaseMessaging.send(any<Message>()) }
        verify(exactly = 1) { 
            eventPublisher.publish(match { 
                it is DeviceTokenUnregisteredEvent && it.token == "bad-token" 
            }) 
        }
    }
    
    @Test
    fun `sendPushNotificationToTokens should publish event when invalid argument`() {
        val tokens = listOf("invalid-token")
        val title = "Test Title"
        val body = "Test Body"
        
        val exception = mockk<FirebaseMessagingException>()
        every { exception.messagingErrorCode } returns MessagingErrorCode.INVALID_ARGUMENT
        every { firebaseMessaging.send(any<Message>()) } throws exception
        
        every { eventPublisher.publish(any()) } just Runs
        
        pushNotificationService.sendPushNotificationToTokens(tokens, title, body)
        
        verify(exactly = 1) { firebaseMessaging.send(any<Message>()) }
        verify(exactly = 1) { 
            eventPublisher.publish(match { 
                it is DeviceTokenUnregisteredEvent && it.token == "invalid-token" 
            }) 
        }
    }

    @Test
    fun `sendPushNotificationToTokens should ignore other firebase exceptions`() {
        val tokens = listOf("token")
        val title = "Test Title"
        val body = "Test Body"
        
        val exception = mockk<FirebaseMessagingException>()
        every { exception.messagingErrorCode } returns MessagingErrorCode.INTERNAL
        every { exception.message } returns "Internal Error"
        every { firebaseMessaging.send(any<Message>()) } throws exception
        
        pushNotificationService.sendPushNotificationToTokens(tokens, title, body)
        
        verify(exactly = 1) { firebaseMessaging.send(any<Message>()) }
        verify(exactly = 0) { eventPublisher.publish(any()) }
    }

    @Test
    fun `sendPushNotificationToTokens should not crash on runtime exceptions`() {
        val tokens = listOf("token")
        val title = "Test Title"
        val body = "Test Body"
        
        every { firebaseMessaging.send(any<Message>()) } throws RuntimeException("Unexpected error")
        
        pushNotificationService.sendPushNotificationToTokens(tokens, title, body)
        
        verify(exactly = 1) { firebaseMessaging.send(any<Message>()) }
        verify(exactly = 0) { eventPublisher.publish(any()) }
    }
}
