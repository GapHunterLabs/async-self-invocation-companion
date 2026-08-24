package dev.gaphunter.asyncselfinvocationcompanion.detect

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class KotlinSelfInvocationFinderTest : BasePlatformTestCase() {

    fun `test unqualified self-invocation of an Async method is flagged`() {
        val file = myFixture.configureByText(
            "OrderService.kt",
            """
            class OrderService {
                fun placeOrder() {
                    notifyWarehouse()
                }

                @Async
                fun notifyWarehouse() {}
            }
            """.trimIndent(),
        )
        assertEquals(1, KotlinSelfInvocationFinder.findAll(file).size)
    }

    fun `test this-qualified self-invocation of an Async method is flagged`() {
        val file = myFixture.configureByText(
            "OrderService.kt",
            """
            class OrderService {
                fun placeOrder() {
                    this.notifyWarehouse()
                }

                @Async
                fun notifyWarehouse() {}
            }
            """.trimIndent(),
        )
        assertEquals(1, KotlinSelfInvocationFinder.findAll(file).size)
    }

    fun `test call through another object is not flagged`() {
        val file = myFixture.configureByText(
            "OrderService.kt",
            """
            class OrderService {
                fun placeOrder(warehouseService: WarehouseService) {
                    warehouseService.notifyWarehouse()
                }
            }

            class WarehouseService {
                @Async
                fun notifyWarehouse() {}
            }
            """.trimIndent(),
        )
        assertTrue(KotlinSelfInvocationFinder.findAll(file).isEmpty())
    }

    fun `test a method with no Async annotation is never flagged`() {
        val file = myFixture.configureByText(
            "OrderService.kt",
            """
            class OrderService {
                fun placeOrder() {
                    notifyWarehouse()
                }

                fun notifyWarehouse() {}
            }
            """.trimIndent(),
        )
        assertTrue(KotlinSelfInvocationFinder.findAll(file).isEmpty())
    }
}
