package org.teEcclesia.config.storage

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import java.net.URI

@Configuration
@EnableConfigurationProperties(AllStorageProperties::class)
class StorageConfig(
    private val props: AllStorageProperties,
) {
    @Bean
    fun teEcclesiaS3Client(teEcclesiaCreds: StaticCredentialsProvider): S3Client =
        buildClient(props.teEcclesia.endpoint, teEcclesiaCreds)

    @Bean
    fun teEcclesiaCreds(): StaticCredentialsProvider {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(props.teEcclesia.key, props.teEcclesia.secret))
    }

    private fun buildClient(
        endpoint: String,
        creds: StaticCredentialsProvider
    ): S3Client {
        return S3Client.builder()
            .region(Region.US_EAST_1) // The region is required by AWS SDK, but DigitalOcean Spaces ignores it
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(creds)
            .serviceConfiguration(pathStyleStorageConfiguration())
            .build()
    }

    @Bean
    fun pathStyleStorageConfiguration(): S3Configuration {
        return S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .build()
    }
}