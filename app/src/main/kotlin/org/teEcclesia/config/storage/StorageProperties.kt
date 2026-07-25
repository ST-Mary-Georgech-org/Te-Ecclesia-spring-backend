package org.teEcclesia.config.storage

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "storage")
data class AllStorageProperties(
    val teEcclesia: StorageProperties,
)

data class StorageProperties(
    val key: String,
    val secret: String,
    val endpoint: String,
    val bucket: String,
    val cdnEndpoint: String
)