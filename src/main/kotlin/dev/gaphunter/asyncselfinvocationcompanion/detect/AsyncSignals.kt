package dev.gaphunter.asyncselfinvocationcompanion.detect

/**
 * Matches Spring's `@Async` by simple annotation name, never by
 * resolving the real classpath symbol -- same principle already used
 * for `@RequestHeader`/`@CrossOrigin`/etc. elsewhere in the catalog.
 * Spring's own documentation states self-invocation bypasses the AOP
 * proxy: "a call to the async-marked method from within the target
 * object is not intercepted... self-invocation... effectively bypasses
 * the proxy and its interceptor chain."
 */
object AsyncSignals {
    fun isAsyncAnnotationName(simpleName: String): Boolean = simpleName == "Async"
}
