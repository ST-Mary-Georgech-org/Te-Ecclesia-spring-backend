package org.teEcclesia.identity.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.teEcclesia.identity.entity.WhatsAppPendingToken

interface WhatsAppPendingTokenRepository : JpaRepository<WhatsAppPendingToken, String>
