package org.teEcclesia.storage.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "storage.te-ecclesia")
data class StorageProperties(
    val key: String,
    val secret: String,
    val endpoint: String,
    val bucket: String,
    val cdnEndpoint: String,
)