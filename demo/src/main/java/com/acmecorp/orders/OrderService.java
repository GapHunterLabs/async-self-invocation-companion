package com.acmecorp.orders;

import org.springframework.scheduling.annotation.Async;

/**
 * Demo data for Async Self-Invocation Companion — used with
 * `./gradlew runIde` to capture the real Marketplace screenshot. Open
 * this file, the warning icon should appear on the call inside
 * `placeOrder`.
 */
public class OrderService {

    public void placeOrder() {
        // Self-invocation -- bypasses Spring's AOP proxy. Runs
        // synchronously, blocking placeOrder(). FLAGGED.
        notifyWarehouse();
    }

    @Async
    public void notifyWarehouse() {
        // ... slow downstream call ...
    }
}
