package org.teEcclesia.identity.service

import org.springframework.stereotype.Service
import org.teEcclesia.identity.entity.User
import org.teEcclesia.identity.entity.enums.Gender
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.repository.UserRepository
import java.time.LocalDate

@Service
class UserCodeGenerator(
    private val userRepository: UserRepository
) {
    fun generateCode(user: User): String {
        val rankChar = getRankChar(user)
        val regYear = LocalDate.now().year.toString().takeLast(2)
        val birthYear = user.nationalId.substring(1, 3)
        val prefix = "$rankChar$regYear$birthYear"
        
        var sequenceNumber = user.nationalId.takeLast(4).toIntOrNull() ?: 0
        var candidateCode: String
        
        do {
            val sequenceStr = String.format("%04d", sequenceNumber)
            candidateCode = "$prefix$sequenceStr"
            
            val searchPattern = "_" + candidateCode.substring(1)
            val exists = userRepository.existsByCodeLike(searchPattern)
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
        
        return user.ordinationProfile?.rank?.codeLetter ?: 'A'
    }
}
