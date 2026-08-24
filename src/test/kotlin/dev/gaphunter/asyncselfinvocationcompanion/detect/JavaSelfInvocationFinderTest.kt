package dev.gaphunter.asyncselfinvocationcompanion.detect

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JavaSelfInvocationFinderTest : BasePlatformTestCase() {

    fun `test unqualified self-invocation of an Async method is flagged`() {
        val file = myFixture.configureByText(
            "OrderService.java",
            """
            class OrderService {
                void placeOrder() {
                    notifyWarehouse();
                }

                @Async
                void notifyWarehouse() {}
            }
            """.trimIndent(),
        )
        assertEquals(1, JavaSelfInvocationFinder.findAll(file).size)
    }

    fun `test this-qualified self-invocation of an Async method is flagged`() {
        val file = myFixture.configureByText(
            "OrderService.java",
            """
            class OrderService {
                void placeOrder() {
                    this.notifyWarehouse();
                }

                @Async
                void notifyWarehouse() {}
            }
            """.trimIndent(),
        )
        assertEquals(1, JavaSelfInvocationFinder.findAll(file).size)
    }

    fun `test call through another object is not flagged`() {
        val file = myFixture.configureByText(
            "OrderService.java",
            """
            class OrderService {
                void placeOrder(WarehouseService warehouseService) {
                    warehouseService.notifyWarehouse();
                }
            }

            class WarehouseService {
                @Async
                void notifyWarehouse() {}
            }
            """.trimIndent(),
        )
        assertTrue(JavaSelfInvocationFinder.findAll(file).isEmpty())
    }

    fun `test a method with no Async annotation is never flagged`() {
        val file = myFixture.configureByText(
            "OrderService.java",
            """
            class OrderService {
                void placeOrder() {
                    notifyWarehouse();
                }

                void notifyWarehouse() {}
            }
            """.trimIndent(),
        )
        assertTrue(JavaSelfInvocationFinder.findAll(file).isEmpty())
    }
}
