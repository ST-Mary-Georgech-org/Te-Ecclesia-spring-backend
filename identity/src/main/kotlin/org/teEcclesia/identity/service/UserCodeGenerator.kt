package org.teEcclesia.identity.service

import org.springframework.stereotype.Service
import org.teEcclesia.identity.entity.User
import org.teEcclesia.identity.entity.enums.Gender
import org.teEcclesia.identity.entity.enums.RankKey
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.repository.UserRepository
import java.time.LocalDate

@Service
class UserCodeGenerator(
    private val userRepository: UserRepository
) {
    fun generateCode(user: User): String {
        val year = LocalDate.now().year.toString().takeLast(2)
        val rankChar = getRankChar(user)
        val prefix = "$year$rankChar"
        
        val latestCode = userRepository.findMaxCodeByPrefix(prefix)
        var sequenceNumber = 1
        
        if (latestCode != null) {
            val seqStr = latestCode.substring(3)
            sequenceNumber = (seqStr.toIntOrNull() ?: 0) + 1
        }
        
        var candidateCode: String
        
        do {
            val sequenceStr = String.format("%06d", sequenceNumber)
            candidateCode = "$prefix$sequenceStr"
            
            val exists = userRepository.findByCode(candidateCode) != null
            if (!exists) {
                break
            }
            sequenceNumber++
        } while (true)
        
        return candidateCode
    }

    private fun getRankChar(user: User): Char {
        if (user.role == UserRole.KAHEN) return 'H'
        if (user.gender == Gender.FEMALE) return 'G'
        
        val ordinationRankKey = user.ordinationProfile?.rank?.key
        
        return when (ordinationRankKey) {
            RankKey.EPSALTOS -> 'B'
            RankKey.OGNOSTOS -> 'C'
            RankKey.EPODKIAKON -> 'D'
            RankKey.DIAKON -> 'E'
            RankKey.ARCHDIAKON -> 'F'
            null -> 'A' // بدون
        }
    }
}
