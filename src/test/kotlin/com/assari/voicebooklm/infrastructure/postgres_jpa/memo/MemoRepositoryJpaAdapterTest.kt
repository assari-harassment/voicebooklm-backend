package com.assari.voicebooklm.infrastructure.postgres_jpa.memo

import com.assari.voicebooklm.AbstractIntegrationTest
import com.assari.voicebooklm.domain.model.memo.Memo
import com.assari.voicebooklm.domain.repository.memo.MemoRepository
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired

/**
 * JPA 実装経由で MemoRepository ポートを検証。
 */
@Tag("integration")
class MemoRepositoryJpaAdapterTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var memoRepository: MemoRepository

    @Autowired
    lateinit var memoJpaRepository: MemoJpaDataRepository

    @AfterEach
    fun tearDown() {
        memoJpaRepository.deleteAll()
    }

    @Test
    fun `保存したメモを取得できる`() = runBlocking {
        val userId = UUID.randomUUID()
        val memo = Memo.create(
            title = "title",
            content = "content",
            tags = listOf(" voice ", "memo"),
            userId = userId,
        )

        val saved = memoRepository.save(memo)
        val found = requireNotNull(memoRepository.findById(saved.id)) {
            "Memo should be found after save"
        }

        assertEquals(saved.id, found.id)
        assertEquals("title", found.title)
        assertEquals("content", found.content)
        assertEquals(listOf("voice", "memo"), found.tags)

        val byUser = memoRepository.findByUserId(userId)
        assertEquals(listOf(found.id), byUser.map { it.id })
    }

    @Test
    fun `論理削除されたメモは取得対象外`() = runBlocking {
        val memo = Memo.create(
            title = "deleted",
            content = "content",
            tags = listOf("tag"),
            userId = UUID.randomUUID(),
        ).markAsDeleted()

        val saved = memoRepository.save(memo)

        assertNull(memoRepository.findById(saved.id))
        assertTrue(memoRepository.findByUserId(saved.userId).isEmpty())
    }
}
