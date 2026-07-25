package org.teEcclesia.storage.service

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.teEcclesia.storage.config.StorageProperties
import org.teEcclesia.storage.exception.InvalidImageException
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse

class ImageStorageServiceTest {

    private lateinit var s3Client: S3Client
    private lateinit var storageProperties: StorageProperties
    private lateinit var imageStorageService: ImageStorageService

    @BeforeEach
    fun setUp() {
        s3Client = mockk()
        storageProperties = mockk()
        every { storageProperties.bucket } returns "test-bucket"
        imageStorageService = ImageStorageService(s3Client, storageProperties)
    }

    @Test
    fun `uploadImageFromBytes with PNG content type succeeds`() {
        val bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
        val contentType = "image/png"
        val fileName = "test-image"
        val folderName = "statistics"

        val requestSlot = slot<PutObjectRequest>()
        every { s3Client.putObject(capture(requestSlot), any<RequestBody>()) } returns PutObjectResponse.builder().build()

        val result = imageStorageService.uploadImageFromBytes(bytes, contentType, fileName, folderName)

        assertThat(result).contains("test-image.png")
        assertThat(result).contains("?time=")
        
        val capturedRequest = requestSlot.captured
        assertThat(capturedRequest.key()).isEqualTo("statistics/test-image.png")
        assertThat(capturedRequest.bucket()).isEqualTo("test-bucket")
        assertThat(capturedRequest.contentType()).isEqualTo("image/png")
    }

    @Test
    fun `uploadImageFromBytes with JPEG content type succeeds`() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        val contentType = "image/jpeg"
        val fileName = "test-photo"
        val folderName = "profiles"

        every { s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>()) } returns PutObjectResponse.builder().build()

        val result = imageStorageService.uploadImageFromBytes(bytes, contentType, fileName, folderName)

        assertThat(result).contains("test-photo.jpg")
        assertThat(result).contains("?time=")
    }

    @Test
    fun `uploadImageFromBytes with invalid content type throws InvalidImageException`() {
        val bytes = byteArrayOf(0x00, 0x01, 0x02, 0x03)
        val contentType = "application/pdf"
        val fileName = "test-file"
        val folderName = "documents"

        val exception = assertThrows<InvalidImageException> {
            imageStorageService.uploadImageFromBytes(bytes, contentType, fileName, folderName)
        }

        assertThat(exception.message).contains("application/pdf")
        verify(exactly = 0) { s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>()) }
    }

    @Test
    fun `uploadImageFromBytes return format does not include folder path`() {
        val bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
        val contentType = "image/png"
        val fileName = "chart-12345"
        val folderName = "statistics"

        every { s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>()) } returns PutObjectResponse.builder().build()

        val result = imageStorageService.uploadImageFromBytes(bytes, contentType, fileName, folderName)

        assertThat(result).matches(".*\\.png\\?time=.*")
        assertThat(result).doesNotContain("statistics/")
    }

    @Test
    fun `uploadImageFromBytes uses correct S3 key with folder path`() {
        val bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
        val contentType = "image/png"
        val fileName = "test"
        val folderName = "my-folder"

        val requestSlot = slot<PutObjectRequest>()
        every { s3Client.putObject(capture(requestSlot), any<RequestBody>()) } returns PutObjectResponse.builder().build()

        imageStorageService.uploadImageFromBytes(bytes, contentType, fileName, folderName)

        assertThat(requestSlot.captured.key()).isEqualTo("my-folder/test.png")
    }

    @Test
    fun `uploadImageFromBytes sets PUBLIC_READ ACL`() {
        val bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
        val contentType = "image/png"
        val fileName = "test"
        val folderName = "public"

        val requestSlot = slot<PutObjectRequest>()
        every { s3Client.putObject(capture(requestSlot), any<RequestBody>()) } returns PutObjectResponse.builder().build()

        imageStorageService.uploadImageFromBytes(bytes, contentType, fileName, folderName)

        assertThat(requestSlot.captured.acl().toString()).isEqualTo("public-read")
    }
}
