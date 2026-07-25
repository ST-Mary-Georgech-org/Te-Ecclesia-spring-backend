package org.teEcclesia.storage.exception

abstract class UploadImageException(message: String, cause: Exception? = null): Exception(message, cause)

class InvalidImageException(
    val extension: String
): UploadImageException("uploading image with invalid extension:  $extension")

class UnknownErrorException(message: String, cause: Exception? = null): UploadImageException(message, cause)