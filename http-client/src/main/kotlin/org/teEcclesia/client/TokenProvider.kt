package org.teEcclesia.client

import java.util.UUID

interface TokenProvider {
    fun generateAccessToken(id: UUID): String
}
