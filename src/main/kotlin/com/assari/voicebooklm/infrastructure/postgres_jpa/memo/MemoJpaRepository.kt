package com.assari.voicebooklm.infrastructure.postgres_jpa.memo

import com.assari.voicebooklm.domain.repository.MemoRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * メモリポジトリ実装
 *
 * Domain Layer の MemoRepository インターフェースを実装する。
 * Infrastructure Layer に属し、JPA を使用してデータベースアクセスを行う。
 *
 * NOTE: MVP では Memo テーブルはまだ存在しないため、
 * deleteByUserId は no-op として実装している。
 * Phase 3 で Memo 機能実装時に実装を更新する。
 */
@Repository
class MemoJpaRepository : MemoRepository {

    override fun deleteByUserId(userId: UUID) {
        // NOTE: Memo テーブルがまだ存在しないため、no-op として実装
        // Phase 3 で Memo 機能実装時に以下のように更新予定:
        // jpaRepo.deleteByUserId(userId)
    }
}
