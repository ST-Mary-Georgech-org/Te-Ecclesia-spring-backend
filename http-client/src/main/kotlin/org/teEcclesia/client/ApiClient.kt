package org.teEcclesia.client

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class ApiClient(
    @Autowired(required = false) private val tokenProvider: TokenProvider,
    @Value("\${internal.api.base-url:http://localhost:8080}") baseUrl: String,
    @param:Value("\${teEcclesia.hmac.secret-key:}") private val hmacSecretKey: String
) {
    private val restClient: RestClient = RestClient.builder()
        .baseUrl(baseUrl)
        .configureMessageConverters { builder ->
            builder.registerDefaults()
            builder.withJsonConverter(JacksonJsonHttpMessageConverter())
        }
        .build()

    fun <T : Any> call(responseType: Class<T>, configure: ApiRequest.() -> Unit): T? {
        val request = ApiRequest().apply(configure)
        val targetPath = request.path
        val uriBuilder = UriComponentsBuilder.fromUriString(targetPath)
        request.queryParams.forEach { (key, value) ->
            uriBuilder.queryParam(key, value)
        }
        val uri = uriBuilder.build().toUriString()

        val requestSpec = restClient.method(request.method).uri(uri)
        request.headers.forEach { (key, value) ->
            requestSpec.header(key, value)
        }

        if (request.addToken) {
            val targetUserId = request.userId ?: (SecurityContextHolder.getContext().authentication?.principal as? UUID)
            if (targetUserId != null) {
                val token = tokenProvider.generateAccessToken(targetUserId)
                requestSpec.header("Authorization", "Bearer $token")
            }
        }

        if (request.includeHMAC && hmacSecretKey.isNotEmpty()) {
            val timestamp = java.time.Instant.now().epochSecond.toString()
            val bodyBytes = if (request.body != null) {
                val mapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
                    .registerModule(com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                mapper.writeValueAsBytes(request.body)
            } else {
                ByteArray(0)
            }
            val keySpec = SecretKeySpec(hmacSecretKey.toByteArray(Charsets.UTF_8), "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(keySpec)
            mac.update(timestamp.toByteArray(Charsets.UTF_8))
            val rawHmac = mac.doFinal(bodyBytes)
            val signature = rawHmac.joinToString("") { "%02x".format(it) }

            requestSpec.header("X-Signature", signature)
            requestSpec.header("X-Timestamp", timestamp)
            if (request.body != null) {
                requestSpec.contentType(MediaType.APPLICATION_JSON)
                requestSpec.body(bodyBytes)
            }
        } else if (request.body != null) {
            requestSpec.body(request.body!!)
        }

        return requestSpec.retrieve().body(responseType)
    }
}
