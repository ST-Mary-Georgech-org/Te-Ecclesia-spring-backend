package org.teEcclesia.storage.exception

abstract class UploadImageException(message: String, cause: Exception? = null): Exception(message, cause)

class InvalidImageException(
    val extension: String
): UploadImageException("error.storage.invalid_extension")

class UnknownErrorException(message: String = "error.storage.upload_failed", cause: Exception? = null): UploadImageException(message, cause)