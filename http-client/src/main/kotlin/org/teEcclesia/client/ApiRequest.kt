package org.teEcclesia.client

import org.springframework.http.HttpMethod
import java.util.*

class ApiRequest {
    var includeHMAC: Boolean = false

    var path: String = ""
    var method: HttpMethod = HttpMethod.GET
    val headers = mutableMapOf<String, String>()
    val queryParams = mutableMapOf<String, String>()
    var body: Any? = null
    var addToken: Boolean = false
    var userId: UUID? = null

    fun header(name: String, value: String) {
        headers[name] = value
    }

    fun queryParam(name: String, value: String) {
        queryParams[name] = value
    }
}
