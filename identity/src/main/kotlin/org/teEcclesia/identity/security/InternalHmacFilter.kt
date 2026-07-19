package org.teEcclesia.identity.security

import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

@Component
class InternalHmacFilter(
    @param:Value("\${teEcclesia.hmac.secret-key:}") private val hmacSecretKey: String
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI
        if (!path.startsWith("/api/v1/internal/")) {
            filterChain.doFilter(request, response)
            return
        }

        if (hmacSecretKey.isBlank()) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "HMAC secret key is not configured")
            return
        }

        val signature = request.getHeader("X-Signature")
        val timestamp = request.getHeader("X-Timestamp")

        if (signature == null || timestamp == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing HMAC headers")
            return
        }

        try {
            val requestTime = timestamp.toLong()
            val now = System.currentTimeMillis() / 1000
            if (abs(now - requestTime) > 300) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Request expired")
                return
            }
        } catch (_: NumberFormatException) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid timestamp")
            return
        }

        // Read the body completely
        val body = request.inputStream.readAllBytes()
        
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(hmacSecretKey.toByteArray(), "HmacSHA256")
        mac.init(secretKey)
        mac.update(timestamp.toByteArray())
        
        if (body.isNotEmpty()) {
            mac.update(body)
        }
        
        val computedSignature = mac.doFinal().joinToString("") { "%02x".format(it) }

        if (computedSignature != signature) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid signature")
            return
        }

        // Create a new request wrapper that allows reading the already-read body again.
        val requestWithBody = object : HttpServletRequestWrapper(request) {
            override fun getInputStream(): ServletInputStream {
                val byteArrayInputStream = ByteArrayInputStream(body)
                return object : ServletInputStream() {
                    override fun read(): Int = byteArrayInputStream.read()
                    override fun isFinished(): Boolean = byteArrayInputStream.available() == 0
                    override fun isReady(): Boolean = true
                    override fun setReadListener(readListener: ReadListener?) {}
                }
            }
            override fun getReader(): BufferedReader {
                return BufferedReader(InputStreamReader(inputStream))
            }
        }

        filterChain.doFilter(requestWithBody, response)
    }
}
