package org.teEcclesia.identity.repository

import org.teEcclesia.identity.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.query.Param
import org.teEcclesia.identity.entity.enums.UserStatus
import org.teEcclesia.identity.entity.enums.UserRole

import org.springframework.data.jpa.repository.EntityGraph
import org.teEcclesia.identity.attendance.repository.projection.AttendeeCandidateProjection
import org.teEcclesia.identity.repository.projection.ParentChildProjection
import org.teEcclesia.identity.repository.projection.PriestSummaryProjection
import org.teEcclesia.identity.repository.projection.ProfileStageLookupProjection
import org.teEcclesia.identity.repository.projection.ProfileYearLookupProjection
import org.teEcclesia.identity.repository.projection.UserEmailProjection
import org.teEcclesia.identity.repository.projection.UserProfileProjection
import org.teEcclesia.identity.repository.projection.UserSummaryProjection
import org.teEcclesia.identity.repository.projection.DeletedUserCheckProjection
import org.teEcclesia.identity.repository.projection.UserAuthDetailsProjection
import org.teEcclesia.identity.repository.projection.UserAuthSummaryProjection
import org.teEcclesia.identity.repository.projection.UserPasswordResetProjection

interface UserRepository : JpaRepository<User, UUID> {
    fun findByCode(code: String): User?
    fun existsByCodeLike(codePattern: String): Boolean
    fun findAllByCodeIn(codes: List<String>): List<User>
    fun <T> findByNationalIdAndStatus(nationalId: String, status: UserStatus, type: Class<T>): T?
    fun findFirstByNationalIdAndStatusNotIn(nationalId: String, statuses: Collection<UserStatus>): User?
    fun findUsersByPhone(phone: String): List<User>
    fun <T> findUsersByPhone(phone: String, type: Class<T>): List<T>
    fun <T> findUsersByPhoneAndStatus(phone: String, status: UserStatus, type: Class<T>): List<T>
    fun <T> findByEmailAndStatusAndIsEmailVerifiedIsTrue(email: String, status: UserStatus, type: Class<T>): T?

    @Query("SELECT u.id as id, u.email as email FROM User u WHERE u.id IN :userIds AND u.email IS NOT NULL")
    fun findEmailsByUserIds(@Param("userIds") userIds: Collection<UUID>): List<UserEmailProjection>

    @Query(
        """
            SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END 
            FROM User u 
                WHERE LOWER(u.email) = LOWER(:email)
                    AND u.status = :status
                    AND u.isEmailVerified = true
                    AND (:excludeUserId IS NULL OR u.id != :excludeUserId)"""
    )
    fun existsVerifiedApprovedEmail(
        @Param("email") email: String,
        @Param("status") status: UserStatus = UserStatus.APPROVED,
        @Param("excludeUserId") excludeUserId: UUID? = null
    ): Boolean

    @Query(
        """
            SELECT COUNT(u)
            FROM User u
            WHERE u.phone = :phone
                AND u.isPhoneVerified = true
                AND u.status NOT IN (:excludedStatuses)
                AND (:excludeUserId IS NULL OR u.id != :excludeUserId)
        """
    )
    fun countVerifiedUsersByPhone(
        @Param("phone") phone: String,
        @Param("excludedStatuses") excludedStatuses: Collection<UserStatus> = listOf(UserStatus.REJECTED, UserStatus.BANNED),
        @Param("excludeUserId") excludeUserId: UUID? = null
    ): Long

    @Query(
        """
            SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END
            FROM User u
            WHERE u.nationalId = :nationalId
                AND u.status NOT IN (:excludedStatuses)
                AND (:excludeUserId IS NULL OR u.id != :excludeUserId)
        """
    )
    fun existsByNationalIdExcludingStatuses(
        @Param("nationalId") nationalId: String,
        @Param("excludedStatuses") excludedStatuses: Collection<UserStatus> = listOf(UserStatus.REJECTED, UserStatus.BANNED),
        @Param("excludeUserId") excludeUserId: UUID? = null
    ): Boolean

    fun findFirstByPhoneAndNationalIdAndIsPhoneVerifiedFalse(phone: String, nationalId: String): User?

    @EntityGraph(value = User.GRAPH_FULL_PROFILE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    fun findProfileById(@Param("id") id: UUID): User?

    @Query(
        value = """
            SELECT 
                u.id as id,
                u.code as code,
                u.firstName as firstName,
                u.secondName as secondName,
                u.thirdName as thirdName,
                u.lastName as lastName,
                u.displayName as displayName,
                u.nationalId as nationalId,
                u.phone as phone,
                u.homePhone as homePhone,
                u.email as email,
                u.isEmailVerified as isEmailVerified,
                u.isPhoneVerified as isPhoneVerified,
                u.imageUrl as imageUrl,
                u.identityDocumentImageUrl as identityDocumentImageUrl,
                u.job as job,
                u.buildingNo as buildingNo,
                u.street as street,
                u.streetBranch as streetBranch,
                u.area as area,
                u.floor as floor,
                u.apartment as apartment,
                u.specialMark as specialMark,
                u.gender as gender,
                u.status as status,
                u.statusReason as statusReason,
                u.role as role,
                u.createdAt as createdAt,
                u.actionTakenAt as actionTakenAt,

                atb.id as actionTakenById,
                atb.displayName as actionTakenByName,
                CONCAT(atb.firstName, ' ', atb.secondName, ' ', atb.thirdName, ' ', atb.lastName) as actionTakenByFullName,
                atb.code as actionTakenByCode,
                atb.imageUrl as actionTakenByImageUrl,

                cp.id as confessionPriestId,
                cp.displayName as confessionPriestName,
                CONCAT(cp.firstName, ' ', cp.secondName, ' ', cp.thirdName, ' ', cp.lastName) as confessionPriestFullName,
                cp.code as confessionPriestCode,
                cp.imageUrl as confessionPriestImageUrl,
                u.externalConfessionPriestName as externalConfessionPriestName,
                u.externalConfessionChurch as externalConfessionChurch,
                u.externalConfessionPhone as externalConfessionPhone,

                kp.id as khademProfileId,
                kp.canApproveRequests as khademCanApproveRequests,
                ks.id as khademStageId,
                ks.nameAr as khademStageNameAr,
                ks.nameEn as khademStageNameEn,
                ky.id as khademYearId,
                ky.nameAr as khademYearNameAr,
                ky.nameEn as khademYearNameEn,
                ky.whatsAppLink as khademYearWhatsAppLink,

                kap.id as kahenProfileId,
                kap.ordinationDate as kahenOrdinationDate,

                pp.id as parentProfileId,
                partner.id as partnerId,
                partner.displayName as partnerName,
                CONCAT(partner.firstName, ' ', partner.secondName, ' ', partner.thirdName, ' ', partner.lastName) as partnerFullName,
                partner.code as partnerCode,
                partner.imageUrl as partnerImageUrl,

                op.id as ordinationProfileId,
                r.id as ordinationRankId,
                r.nameAr as ordinationRankNameAr,
                r.nameEn as ordinationRankNameEn,
                op.isOrdinationInAnotherChurch as ordinationIsOrdinationInAnotherChurch,
                op.ordinationYear as ordinationYear,
                op.bishopName as ordinationBishopName,
                op.ordinationPlace as ordinationPlace,
                op.certificateImageUrl as ordinationCertificateImageUrl,

                mp.id as makhdoomProfileId,
                ms.id as makhdoomStageId,
                ms.nameAr as makhdoomStageNameAr,
                ms.nameEn as makhdoomStageNameEn,
                my.id as makhdoomYearId,
                my.nameAr as makhdoomYearNameAr,
                my.nameEn as makhdoomYearNameEn,
                my.whatsAppLink as makhdoomYearWhatsAppLink,
                mp.shamamsaStudyStatus as makhdoomShamamsaStudyStatus,
                mp.fatherPhone as makhdoomFatherPhone,
                mp.fatherWhatsapp as makhdoomFatherWhatsapp,
                mp.motherPhone as makhdoomMotherPhone,
                mp.motherWhatsapp as makhdoomMotherWhatsapp,
                mp.isFatherDeceased as makhdoomIsFatherDeceased,
                mp.isMotherDeceased as makhdoomIsMotherDeceased

            FROM User u 
            LEFT JOIN u.actionTakenBy atb
            LEFT JOIN u.confessionPriest cp
            LEFT JOIN u.khademProfile kp
            LEFT JOIN kp.educationalStage ks
            LEFT JOIN kp.educationalYear ky
            LEFT JOIN u.kahenProfile kap
            LEFT JOIN u.parentProfile pp
            LEFT JOIN pp.partner partner
            LEFT JOIN u.ordinationProfile op
            LEFT JOIN op.rank r
            LEFT JOIN u.makhdoomProfile mp
            LEFT JOIN mp.educationalStage ms
            LEFT JOIN mp.educationalYear my
            WHERE u.id = :id
        """
    )
    fun findProfileByIdProjection(@Param("id") id: UUID): UserProfileProjection?

    @Query("""
        SELECT 
            u.id as id,
            u.code as code,
            u.phone as phone,
            u.email as email,
            u.nationalId as nationalId,
            u.firstName as firstName,
            u.secondName as secondName,
            u.thirdName as thirdName,
            u.lastName as lastName,
            u.displayName as displayName,
            u.passwordHash as passwordHash,
            u.role as role,
            u.status as status,
            u.isEmailVerified as isEmailVerified,
            u.isPhoneVerified as isPhoneVerified
        FROM User u 
        WHERE (u.email = :id OR u.nationalId = :id OR u.code = :id OR u.phone = :id)
        ORDER BY u.isPhoneVerified DESC, u.createdAt DESC
    """)
    fun findAuthSummariesByIdentifier(@Param("id") id: String): List<UserAuthSummaryProjection>
    
    @Query(
        value = """
            SELECT 
                u.id as id,
                u.code as code,
                u.firstName as firstName,
                u.secondName as secondName,
                u.thirdName as thirdName,
                u.lastName as lastName,
                u.displayName as displayName,
                u.nationalId as nationalId,
                u.phone as phone,
                u.homePhone as homePhone,
                u.email as email,
                u.isEmailVerified as isEmailVerified,
                u.isPhoneVerified as isPhoneVerified,
                u.imageUrl as imageUrl,
                u.identityDocumentImageUrl as identityDocumentImageUrl,
                u.job as job,
                u.buildingNo as buildingNo,
                u.street as street,
                u.streetBranch as streetBranch,
                u.area as area,
                u.floor as floor,
                u.apartment as apartment,
                u.specialMark as specialMark,
                u.gender as gender,
                u.status as status,
                u.statusReason as statusReason,
                u.role as role,
                u.createdAt as createdAt,
                u.actionTakenAt as actionTakenAt,

                atb.id as actionTakenById,
                atb.displayName as actionTakenByName,
                CONCAT(atb.firstName, ' ', atb.secondName, ' ', atb.thirdName, ' ', atb.lastName) as actionTakenByFullName,
                atb.code as actionTakenByCode,
                atb.imageUrl as actionTakenByImageUrl,

                cp.id as confessionPriestId,
                cp.displayName as confessionPriestName,
                CONCAT(cp.firstName, ' ', cp.secondName, ' ', cp.thirdName, ' ', cp.lastName) as confessionPriestFullName,
                cp.code as confessionPriestCode,
                cp.imageUrl as confessionPriestImageUrl,
                u.externalConfessionPriestName as externalConfessionPriestName,
                u.externalConfessionChurch as externalConfessionChurch,
                u.externalConfessionPhone as externalConfessionPhone,

                kp.id as khademProfileId,
                kp.canApproveRequests as khademCanApproveRequests,
                ks.id as khademStageId,
                ks.nameAr as khademStageNameAr,
                ks.nameEn as khademStageNameEn,
                ky.id as khademYearId,
                ky.nameAr as khademYearNameAr,
                ky.nameEn as khademYearNameEn,
                ky.whatsAppLink as khademYearWhatsAppLink,

                kap.id as kahenProfileId,
                kap.ordinationDate as kahenOrdinationDate,

                pp.id as parentProfileId,
                partner.id as partnerId,
                partner.displayName as partnerName,
                CONCAT(partner.firstName, ' ', partner.secondName, ' ', partner.thirdName, ' ', partner.lastName) as partnerFullName,
                partner.code as partnerCode,
                partner.imageUrl as partnerImageUrl,

                op.id as ordinationProfileId,
                op.isOrdinationInAnotherChurch as ordinationIsOrdinationInAnotherChurch,
                op.ordinationYear as ordinationYear,
                op.bishopName as ordinationBishopName,
                op.ordinationPlace as ordinationPlace,
                op.certificateImageUrl as ordinationCertificateImageUrl,
                r.id as ordinationRankId,
                r.nameAr as ordinationRankNameAr,
                r.nameEn as ordinationRankNameEn,

                mp.id as makhdoomProfileId,
                mp.shamamsaStudyStatus as makhdoomShamamsaStudyStatus,
                ms.id as makhdoomStageId,
                ms.nameAr as makhdoomStageNameAr,
                ms.nameEn as makhdoomStageNameEn,
                my.id as makhdoomYearId,
                my.nameAr as makhdoomYearNameAr,
                my.nameEn as makhdoomYearNameEn,
                my.whatsAppLink as makhdoomYearWhatsAppLink,
                mp.fatherPhone as makhdoomFatherPhone,
                mp.fatherWhatsapp as makhdoomFatherWhatsapp,
                mp.motherPhone as makhdoomMotherPhone,
                mp.motherWhatsapp as makhdoomMotherWhatsapp,
                mp.isFatherDeceased as makhdoomIsFatherDeceased,
                mp.isMotherDeceased as makhdoomIsMotherDeceased

            FROM User u 
            LEFT JOIN u.actionTakenBy atb
            LEFT JOIN u.confessionPriest cp
            LEFT JOIN u.khademProfile kp
            LEFT JOIN kp.educationalStage ks
            LEFT JOIN kp.educationalYear ky
            LEFT JOIN u.kahenProfile kap
            LEFT JOIN u.parentProfile pp
            LEFT JOIN pp.partner partner
            LEFT JOIN u.ordinationProfile op
            LEFT JOIN op.rank r
            LEFT JOIN u.makhdoomProfile mp
            LEFT JOIN mp.educationalStage ms
            LEFT JOIN mp.educationalYear my
            WHERE u.status = :status 
            AND (cast(:stageIds as string) IS NULL OR ms.id IN :stageIds OR ks.id IN :stageIds) 
            AND (cast(:yearIds as string) IS NULL OR my.id IN :yearIds OR ky.id IN :yearIds)
            AND (cast(:role as string) IS NULL OR u.role = :role)
            AND (cast(:search as string) IS NULL OR 
                 LOWER(CONCAT(u.firstName, ' ', u.secondName, ' ', u.thirdName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', cast(:search as string), '%')) OR 
                 u.phone LIKE CONCAT('%', cast(:search as string), '%') OR 
                 u.nationalId LIKE CONCAT('%', cast(:search as string), '%') OR 
                 LOWER(u.email) LIKE LOWER(CONCAT('%', cast(:search as string), '%')) OR 
                 LOWER(u.code) LIKE LOWER(CONCAT('%', cast(:search as string), '%')))
        """,
        countQuery = """
            SELECT count(u.id) FROM User u 
            LEFT JOIN u.makhdoomProfile mp 
            LEFT JOIN u.khademProfile kp 
            WHERE u.status = :status 
            AND (cast(:stageIds as string) IS NULL OR mp.educationalStage.id IN :stageIds OR kp.educationalStage.id IN :stageIds) 
            AND (cast(:yearIds as string) IS NULL OR mp.educationalYear.id IN :yearIds OR kp.educationalYear.id IN :yearIds)
            AND (cast(:role as string) IS NULL OR u.role = :role)
            AND (cast(:search as string) IS NULL OR 
                 LOWER(CONCAT(u.firstName, ' ', u.secondName, ' ', u.thirdName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', cast(:search as string), '%')) OR 
                 u.phone LIKE CONCAT('%', cast(:search as string), '%') OR 
                 u.nationalId LIKE CONCAT('%', cast(:search as string), '%') OR 
                 LOWER(u.email) LIKE LOWER(CONCAT('%', cast(:search as string), '%')) OR 
                 LOWER(u.code) LIKE LOWER(CONCAT('%', cast(:search as string), '%')))
        """
    )
    fun findProfilesByStatusAndFiltersProjection(
        @Param("status") status: UserStatus,
        @Param("stageIds") stageIds: Collection<Long>?,
        @Param("yearIds") yearIds: Collection<Long>?,
        @Param("role") role: UserRole?,
        @Param("search") search: String?,
        pageable: Pageable
    ): Page<UserProfileProjection>
    
    @Query("SELECT p.id as parentId, c.id as childId, c.displayName as displayName, CONCAT(c.firstName, ' ', c.secondName, ' ', c.thirdName, ' ', c.lastName) as fullName, c.code as code, c.imageUrl as imageUrl FROM ParentProfile p JOIN p.children c WHERE p.id IN :parentIds")
    fun findChildrenByParentIds(@Param("parentIds") parentIds: Collection<Long>): List<ParentChildProjection>

    @Query("""
        SELECT kp.id as profileId, s.id as id, s.nameAr as nameAr, s.nameEn as nameEn 
        FROM KhademProfile kp 
        JOIN kp.responsibleStages s 
        WHERE kp.id IN :khademProfileIds
    """)
    fun findResponsibleStagesByKhademProfileIds(@Param("khademProfileIds") khademProfileIds: Collection<Long>): List<ProfileStageLookupProjection>

    @Query("""
        SELECT kp.id as profileId, y.id as id, y.nameAr as nameAr, y.nameEn as nameEn, y.whatsAppLink as whatsAppLink 
        FROM KhademProfile kp 
        JOIN kp.responsibleYears y 
        WHERE kp.id IN :khademProfileIds
    """)
    fun findResponsibleYearsByKhademProfileIds(@Param("khademProfileIds") khademProfileIds: Collection<Long>): List<ProfileYearLookupProjection>

    @Query("""
        SELECT kp.id as profileId, s.id as id, s.nameAr as nameAr, s.nameEn as nameEn 
        FROM KahenProfile kp 
        JOIN kp.educationalStages s 
        WHERE kp.id IN :kahenProfileIds
    """)
    fun findEducationalStagesByKahenProfileIds(@Param("kahenProfileIds") kahenProfileIds: Collection<Long>): List<ProfileStageLookupProjection>

    @Query("""
        SELECT 
            u.id as id,
            u.displayName as name,
            kp.ordinationDate as ordinationDate
        FROM User u
        LEFT JOIN u.kahenProfile kp
        WHERE u.role = :role AND u.status = :status
    """)
    fun findPriestsSummary(
        @Param("role") role: UserRole,
        @Param("status") status: UserStatus,
        pageable: Pageable
    ): Page<PriestSummaryProjection>

    @Query("""
        SELECT 
            u.id as id,
            u.displayName as displayName,
            CONCAT(u.firstName, ' ', u.secondName, ' ', u.thirdName, ' ', u.lastName) as fullName,
            u.code as code,
            u.imageUrl as imageUrl
        FROM User u 
        WHERE u.role IN :roles AND u.status = :status
        AND ((u.email = :query AND u.isEmailVerified = true) OR u.nationalId = :query OR u.code = :query OR u.phone = :query)
    """)
    fun findSummaryByRolesAndIdentifier(
        @Param("roles") roles: Collection<UserRole>,
        @Param("status") status: UserStatus,
        @Param("query") query: String
    ): List<UserSummaryProjection>

    @Query("""
        SELECT u FROM User u 
        LEFT JOIN u.khademProfile kp 
        WHERE u.status = :approvedStatus 
        AND u.deleted = false
        AND (u.role = :adminRole OR (u.role = :khademRole AND kp.canApproveRequests = true))
    """)
    fun findApprovers(
        @Param("adminRole") adminRole: UserRole = UserRole.ADMIN,
        @Param("khademRole") khademRole: UserRole = UserRole.KHADEM,
        @Param("approvedStatus") approvedStatus: UserStatus = UserStatus.APPROVED,
        pageable: Pageable
    ): Page<User>
    
    fun deleteAllByIsPhoneVerifiedIsFalseAndCreatedAtBefore(date: Instant)

    @Query("""
        SELECT 
            u.id as id,
            u.firstName as firstName,
            u.secondName as secondName,
            u.thirdName as thirdName,
            u.lastName as lastName,
            u.role as role,
            u.code as code,
            u.imageUrl as imageUrl,
            ms.id as makhdoomStageId,
            ms.nameAr as makhdoomStageAr,
            ms.nameEn as makhdoomStageEn,
            my.nameAr as makhdoomYearAr,
            my.nameEn as makhdoomYearEn,
            ks.id as khademStageId,
            ks.nameAr as khademStageAr,
            ks.nameEn as khademStageEn,
            ky.nameAr as khademYearAr,
            ky.nameEn as khademYearEn
        FROM User u 
        LEFT JOIN u.makhdoomProfile mp
        LEFT JOIN mp.educationalStage ms
        LEFT JOIN mp.educationalYear my
        LEFT JOIN u.khademProfile kp
        LEFT JOIN kp.educationalStage ks
        LEFT JOIN kp.educationalYear ky
        WHERE u.status = UserStatus.APPROVED
        AND (
            REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LOWER(CONCAT(u.firstName, ' ', u.secondName, ' ', u.thirdName, ' ', u.lastName)), 'أ', 'ا'), 'إ', 'ا'), 'آ', 'ا'), 'ة', 'ه'), 'ى', 'ي'), 'ؤ', 'و'), 'ئ', 'ء') LIKE CONCAT('%', :normalizedQuery, '%')
            OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LOWER(u.displayName), 'أ', 'ا'), 'إ', 'ا'), 'آ', 'ا'), 'ة', 'ه'), 'ى', 'ي'), 'ؤ', 'و'), 'ئ', 'ء') LIKE CONCAT('%', :normalizedQuery, '%')
            OR u.phone LIKE CONCAT('%', :rawQuery, '%')
            OR u.nationalId LIKE CONCAT('%', :rawQuery, '%')
            OR LOWER(u.code) LIKE LOWER(CONCAT('%', :rawQuery, '%'))
            OR (:numericQuery != '' AND u.code LIKE CONCAT('%', :numericQuery, '%'))
        )
    """)
    fun searchAttendanceCandidates(
        @Param("normalizedQuery") normalizedQuery: String,
        @Param("rawQuery") rawQuery: String,
        @Param("numericQuery") numericQuery: String,
        pageable: Pageable
    ): List<AttendeeCandidateProjection>

    @Query("""
        SELECT 
            u.id as id,
            u.firstName as firstName,
            u.secondName as secondName,
            u.thirdName as thirdName,
            u.lastName as lastName,
            u.role as role,
            u.code as code,
            u.imageUrl as imageUrl,
            ms.id as makhdoomStageId,
            ms.nameAr as makhdoomStageAr,
            ms.nameEn as makhdoomStageEn,
            my.nameAr as makhdoomYearAr,
            my.nameEn as makhdoomYearEn,
            ks.id as khademStageId,
            ks.nameAr as khademStageAr,
            ks.nameEn as khademStageEn,
            ky.nameAr as khademYearAr,
            ky.nameEn as khademYearEn
        FROM User u 
        LEFT JOIN u.makhdoomProfile mp
        LEFT JOIN mp.educationalStage ms
        LEFT JOIN mp.educationalYear my
        LEFT JOIN u.khademProfile kp
        LEFT JOIN kp.educationalStage ks
        LEFT JOIN kp.educationalYear ky
        WHERE u.status = UserStatus.APPROVED
        AND u.role = UserRole.KHADEM
        AND (
            REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LOWER(CONCAT(u.firstName, ' ', u.secondName, ' ', u.thirdName, ' ', u.lastName)), 'أ', 'ا'), 'إ', 'ا'), 'آ', 'ا'), 'ة', 'ه'), 'ى', 'ي'), 'ؤ', 'و'), 'ئ', 'ء') LIKE CONCAT('%', :normalizedQuery, '%')
            OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LOWER(u.displayName), 'أ', 'ا'), 'إ', 'ا'), 'آ', 'ا'), 'ة', 'ه'), 'ى', 'ي'), 'ؤ', 'و'), 'ئ', 'ء') LIKE CONCAT('%', :normalizedQuery, '%')
            OR u.phone LIKE CONCAT('%', :rawQuery, '%')
            OR u.nationalId LIKE CONCAT('%', :rawQuery, '%')
            OR LOWER(u.code) LIKE LOWER(CONCAT('%', :rawQuery, '%'))
            OR (:numericQuery != '' AND u.code LIKE CONCAT('%', :numericQuery, '%'))
        )
    """)
    fun searchServantCandidates(
        @Param("normalizedQuery") normalizedQuery: String,
        @Param("rawQuery") rawQuery: String,
        @Param("numericQuery") numericQuery: String,
        pageable: Pageable
    ): List<AttendeeCandidateProjection>

    @Query("SELECT u.role FROM User u WHERE u.id = :id AND u.deleted = false")
    fun findRoleById(@Param("id") id: UUID): UserRole?

    @Query("SELECT u.id FROM User u WHERE u.id IN :ids AND u.role = 'KHADEM' AND u.status = 'APPROVED' AND u.deleted = false")
    fun findApprovedKhademIdsByIds(@Param("ids") ids: Collection<UUID>): List<UUID>

    @Query("""
        SELECT 
            u.id as id,
            u.firstName as firstName,
            u.secondName as secondName,
            u.thirdName as thirdName,
            u.lastName as lastName,
            u.role as role,
            u.code as code,
            u.imageUrl as imageUrl,
            ms.id as makhdoomStageId,
            ms.nameAr as makhdoomStageAr,
            ms.nameEn as makhdoomStageEn,
            my.nameAr as makhdoomYearAr,
            my.nameEn as makhdoomYearEn,
            ks.id as khademStageId,
            ks.nameAr as khademStageAr,
            ks.nameEn as khademStageEn,
            ky.nameAr as khademYearAr,
            ky.nameEn as khademYearEn
        FROM User u 
        LEFT JOIN u.makhdoomProfile mp
        LEFT JOIN mp.educationalStage ms
        LEFT JOIN mp.educationalYear my
        LEFT JOIN u.khademProfile kp
        LEFT JOIN kp.educationalStage ks
        LEFT JOIN kp.educationalYear ky
        WHERE u.deleted = false
        AND (LOWER(u.code) = LOWER(:code) OR (:digits != '' AND u.code LIKE CONCAT('%', :digits)))
    """)
    fun findCandidateByCode(
        @Param("code") code: String,
        @Param("digits") digits: String
    ): List<AttendeeCandidateProjection>

    @Query("""
        SELECT DISTINCT u.id FROM User u 
        LEFT JOIN u.makhdoomProfile mp 
        LEFT JOIN u.khademProfile kp 
        WHERE u.status = :status 
        AND u.deleted = false
        AND (:role IS NULL OR u.role = :role)
        AND (
            :stageId IS NULL 
            OR (u.role = 'MAKHDOOM' AND mp.educationalStage.id = :stageId) 
            OR (u.role = 'KHADEM' AND kp.educationalStage.id = :stageId)
        )
    """)
    fun findTargetUserIds(
        @Param("status") status: UserStatus = UserStatus.APPROVED,
        @Param("stageId") stageId: Long? = null,
        @Param("role") role: UserRole? = null
    ): List<UUID>

    @Query(
        nativeQuery = true,
        value = """
            SELECT 
                u.id as id,
                CONCAT(u.first_name, ' ', u.second_name, ' ', u.third_name, ' ', u.last_name) as fullName,
                u.password_hash as passwordHash,
                u.image_url as imageUrl,
                u.role as role,
                u.status as status
            FROM identity.users u 
            WHERE u.id = :userId
        """
    )
    fun findAuthDetailsById(@Param("userId") userId: UUID): UserAuthDetailsProjection?

    @Query(
        nativeQuery = true,
        value = """
            SELECT 
                u.id as id,
                u.status as status,
                u.national_id as nationalId,
                CONCAT(u.first_name, ' ', u.second_name, ' ', u.third_name, ' ', u.last_name) as fullName,
                u.password_hash as passwordHash
            FROM identity.users u
            WHERE u.national_id = :nationalId 
              AND u.deleted = true 
              AND u.status NOT IN ('REJECTED', 'BANNED', 'UNVERIFIED', 'PROFILE_INCOMPLETE')
            ORDER BY u.created_at DESC
            LIMIT 1
        """
    )
    fun findDeletedByNationalId(@Param("nationalId") nationalId: String): DeletedUserCheckProjection?

    @Modifying
    @Transactional
    @Query(
        nativeQuery = true,
        value = "UPDATE identity.users SET deleted = true WHERE id = :userId"
    )
    fun softDeleteById(@Param("userId") userId: UUID): Int

    @Modifying
    @Transactional
    @Query(
        nativeQuery = true,
        value = "UPDATE identity.users SET deleted = false, status = 'APPROVED' WHERE id = :userId"
    )
    fun restoreUser(@Param("userId") userId: UUID)

    @Modifying
    @Transactional
    @Query(
        nativeQuery = true,
        value = "UPDATE identity.users SET password_hash = :newHash WHERE id = :userId"
    )
    fun updatePasswordHash(@Param("userId") userId: UUID, @Param("newHash") newHash: String)

    @Query("SELECT u.id FROM User u WHERE u.role = UserRole.ADMIN AND u.status = UserStatus.APPROVED AND u.deleted = false")
    fun findAllAdminIds(): List<UUID>

    @Query(
        nativeQuery = true,
        value = """
            SELECT 
                CAST(u.id AS varchar) as id,
                u.phone as phone,
                u.email as email,
                u.national_id as nationalId,
                CONCAT(u.first_name, ' ', u.second_name, ' ', u.third_name, ' ', u.last_name) as fullName,
                u.deleted as deleted,
                u.is_email_verified as isEmailVerified
            FROM identity.users u
            WHERE (
                (LOWER(u.email) = LOWER(:identifier) AND u.is_email_verified = true)
                OR u.national_id = :identifier
                OR UPPER(u.code) = UPPER(:identifier)
                OR u.phone = :identifier
            )
              AND u.status NOT IN ('REJECTED', 'BANNED', 'UNVERIFIED', 'PROFILE_INCOMPLETE')
            ORDER BY u.deleted ASC, u.is_phone_verified DESC, u.created_at DESC
        """
    )
    fun findForPasswordResetByIdentifier(@Param("identifier") identifier: String): List<UserPasswordResetProjection>

    @Query(
        nativeQuery = true,
        value = "SELECT u.deleted FROM identity.users u WHERE u.id = :userId"
    )
    fun isUserDeleted(@Param("userId") userId: UUID): Boolean?

    @Query(
        nativeQuery = true,
        value = "SELECT u.phone FROM identity.users u WHERE u.id = :id"
    )
    fun findPhoneById(@Param("id") id: UUID): String?

    @Query(
        nativeQuery = true,
        value = "SELECT u.email FROM identity.users u WHERE u.id = :id"
    )
    fun findEmailById(@Param("id") id: UUID): String?

    @Modifying
    @Transactional
    @Query(
        nativeQuery = true,
        value = "UPDATE identity.users SET email = :email, is_email_verified = true WHERE id = :userId"
    )
    fun updateEmailAndVerified(@Param("userId") userId: UUID, @Param("email") email: String): Int

    @Modifying
    @Transactional
    @Query(
        nativeQuery = true,
        value = "UPDATE identity.users SET phone = :phone, is_phone_verified = true WHERE id = :userId"
    )
    fun updatePhoneAndVerified(@Param("userId") userId: UUID, @Param("phone") phone: String): Int

    @Modifying
    @Transactional
    @Query(
        nativeQuery = true,
        value = """
            UPDATE identity.users 
            SET is_phone_verified = true, 
                created_at = :now, 
                status = CASE WHEN status = 'UNVERIFIED' THEN 'PENDING_APPROVAL' ELSE status END 
            WHERE id = :userId
        """
    )
    fun verifyUserPhone(
        @Param("userId") userId: UUID,
        @Param("now") now: Instant = Instant.now()
    ): Int
}