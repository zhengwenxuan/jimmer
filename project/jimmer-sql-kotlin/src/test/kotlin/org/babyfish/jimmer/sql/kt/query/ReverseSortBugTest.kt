package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.ast.expression.asc
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.like
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.classic.book.*
import org.junit.Test

class ReverseSortBugTest : AbstractQueryTest() {

    @Test
    fun testReverseSortDisabled() {
        connectAndExpect({
            sqlClient.createQuery(Book::class) {
                where(table.name like "GraphQL")
                orderBy(table.name.asc(), table.edition.desc())
                select(table)
            }.setReverseSortOptimizationEnabled(false).fetchPage(1, 3, it) { entities, totalCount, _ ->
                Page(entities, totalCount)
            }
        }) {
            sql(
                """select count(1) 
                    |from BOOK tb_1_ 
                    |where tb_1_.NAME like ?""".trimMargin()
            )
            // Should NOT have "reverse sorting optimization" comment
            // Should be original order and original limit/offset
            statement(1).sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |where tb_1_.NAME like ? 
                    |order by tb_1_.NAME asc, tb_1_.EDITION desc 
                    |limit ? offset ?""".trimMargin()
            ).variables("%GraphQL%", 3, 3)
            row(0) {
                expectJson(
                    """Page(
                        |--->entities=[
                        |--->--->{
                        |--->--->--->"id":3,
                        |--->--->--->"name":"Learning GraphQL",
                        |--->--->--->"edition":3,
                        |--->--->--->"price":51.00,
                        |--->--->--->"storeId":1
                        |--->--->}, {
                        |--->--->--->"id":2,
                        |--->--->--->"name":"Learning GraphQL",
                        |--->--->--->"edition":2,
                        |--->--->--->"price":55.00,
                        |--->--->--->"storeId":1
                        |--->--->}, {
                        |--->--->--->"id":1,
                        |--->--->--->"name":"Learning GraphQL",
                        |--->--->--->"edition":1,
                        |--->--->--->"price":50.00,
                        |--->--->--->"storeId":1
                        |--->--->}
                        |--->], 
                        |--->totalCount=6
                        |)""".trimMargin(),
                    it
                )
            }
        }
    }

    data class Page<E>(
        val entities: List<E>,
        val totalCount: Long
    )
}
