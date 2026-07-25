package org.teEcclesia.storage.service

import org.teEcclesia.storage.config.StorageProperties
import org.teEcclesia.storage.exception.InvalidImageException
import org.teEcclesia.storage.exception.UnknownErrorException
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.time.Instant

@Service
@EnableConfigurationProperties(StorageProperties::class)
class ImageStorageService(
    private val teEcclesiaS3Client: S3Client,
    private val identityStorageProperties: StorageProperties
) {
    fun uploadImage(
        file: MultipartFile,
        fileName: String,
        folderName: String
    ): String {
        val mimeType = file.contentType ?: throw InvalidImageException("null")
        val extension = allowedMimeTypes[mimeType] ?: throw InvalidImageException(mimeType)
        try {
            val fullFileName = "${fileName}.$extension"
            val randomParameter = Instant.now().toString()
            val key = "$folderName/$fullFileName"
            val putReq = createObjectRequest(key, mimeType)
            teEcclesiaS3Client.putObject(putReq, RequestBody.fromBytes(file.bytes))
            val imageUri = "$fullFileName?time=$randomParameter"
            return imageUri
        } catch (e: Exception) {
            throw UnknownErrorException(e.message ?: "Unknown error occurred", e)
        }
    }

    fun uploadImageFromBytes(
        bytes: ByteArray,
        contentType: String,
        fileName: String,
        folderName: String
    ): String {
        val extension = allowedMimeTypes[contentType] ?: throw InvalidImageException(contentType)
        try {
            val fullFileName = "${fileName}.$extension"
            val randomParameter = Instant.now().toString()
            val key = "$folderName/$fullFileName"
            val putReq = createObjectRequest(key, contentType)
            teEcclesiaS3Client.putObject(putReq, RequestBody.fromBytes(bytes))
            val imageUri = "$fullFileName?time=$randomParameter"
            return imageUri
        } catch (e: Exception) {
            throw UnknownErrorException(e.message ?: "Unknown error occurred", e)
        }
    }

    fun deleteImage(folderName: String, fileName: String) {
        try {
            val deleteRequest = deleteObjectRequest("$folderName/$fileName")
            teEcclesiaS3Client.deleteObject(deleteRequest)
        } catch (e: Exception) {
            throw UnknownErrorException(e.message ?: "Unknown error occurred", e)
        }
    }

    private fun createObjectRequest(key: String, contentType: String): PutObjectRequest? {
        return PutObjectRequest.builder()
            .bucket(identityStorageProperties.bucket)
            .key(key)
            .contentType(contentType)
            .acl(ObjectCannedACL.PUBLIC_READ)
            .build()
    }

    private fun deleteObjectRequest(key: String): DeleteObjectRequest {
        return DeleteObjectRequest.builder()
            .bucket(identityStorageProperties.bucket)
            .key(key)
            .build()
    }

    private companion object {
        val allowedMimeTypes = mapOf(
            "image/jpeg" to "jpg",
            "image/jpg" to "jpg",
            "image/png" to "png",
            "image/webp" to "webp",
        )
    }
}