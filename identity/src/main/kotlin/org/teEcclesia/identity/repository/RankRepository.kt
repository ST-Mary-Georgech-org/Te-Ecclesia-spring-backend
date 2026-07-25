package org.teEcclesia.identity.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.teEcclesia.identity.entity.lookups.Rank

interface RankRepository : JpaRepository<Rank, Long>
