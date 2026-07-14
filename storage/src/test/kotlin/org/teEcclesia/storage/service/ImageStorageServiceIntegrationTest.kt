package org.teEcclesia.storage.service

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.teEcclesia.storage.config.StorageProperties
import org.teEcclesia.storage.exception.InvalidImageException
import org.teEcclesia.storage.exception.UnknownErrorException
import org.springframework.mock.web.MockMultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse

class ImageStorageServiceIntegrationTest {

    private val s3Client = mockk<S3Client>()
    private val storageProperties = StorageProperties(
        key = "test-key",
        secret = "test-secret",
        endpoint = "https://example.test",
        bucket = "test-bucket",
        cdnEndpoint = "https://cdn.example.test"
    )
    private val imageStorageService = ImageStorageService(s3Client, storageProperties)

    @Test
    fun `uploadImage uploads object and returns filename with cache busting query`() {
        val requestSlot = slot<PutObjectRequest>()
        every {
            s3Client.putObject(capture(requestSlot), any<RequestBody>())
        } returns PutObjectResponse.builder().build()
        val file = MockMultipartFile("file", "avatar.png", "image/png", byteArrayOf(1, 2, 3))

        val imageUrl = imageStorageService.uploadImage(file, "user-1", "profiles")

        verify(exactly = 1) { s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>()) }
        assertThat(requestSlot.captured.bucket()).isEqualTo("test-bucket")
        assertThat(requestSlot.captured.key()).isEqualTo("profiles/user-1.png")
        assertThat(requestSlot.captured.contentType()).isEqualTo("image/png")
        assertThat(imageUrl).startsWith("user-1.png?time=")
    }

    @Test
    fun `uploadImage throws InvalidImageException for unsupported mime type`() {
        val file = MockMultipartFile("file", "avatar.gif", "image/gif", byteArrayOf(1, 2, 3))

        val thrownException = assertThrows<InvalidImageException> {
            imageStorageService.uploadImage(file, "user-1", "profiles")
        }

        assertThat(thrownException.extension).isEqualTo("image/gif")
    }

    @Test
    fun `uploadImage wraps storage failures in UnknownErrorException`() {
        every {
            s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>())
        } throws RuntimeException("s3 upload failed")
        val file = MockMultipartFile("file", "avatar.png", "image/png", byteArrayOf(1, 2, 3))

        val thrownException = assertThrows<UnknownErrorException> {
            imageStorageService.uploadImage(file, "user-1", "profiles")
        }

        assertThat(thrownException).hasMessageThat().contains("s3 upload failed")
    }

    @Test
    fun `deleteImage deletes object from bucket with folder key`() {
        val requestSlot = slot<DeleteObjectRequest>()
        every { s3Client.deleteObject(capture(requestSlot)) } returns DeleteObjectResponse.builder().build()

        imageStorageService.deleteImage("profiles", "user-1.png")

        verify(exactly = 1) { s3Client.deleteObject(any<DeleteObjectRequest>()) }
        assertThat(requestSlot.captured.bucket()).isEqualTo("test-bucket")
        assertThat(requestSlot.captured.key()).isEqualTo("profiles/user-1.png")
    }

    @Test
    fun `deleteImage wraps storage failures in UnknownErrorException`() {
        every { s3Client.deleteObject(any<DeleteObjectRequest>()) } throws RuntimeException("s3 delete failed")

        val thrownException = assertThrows<UnknownErrorException> {
            imageStorageService.deleteImage("profiles", "user-1.png")
        }

        assertThat(thrownException).hasMessageThat().contains("s3 delete failed")
    }
}

