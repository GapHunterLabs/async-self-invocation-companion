# Async Self-Invocation Companion

Warning icon on a call to a Spring `@Async` method made from another
method of the **same** declaring class — Spring's own documentation
states this bypasses the AOP proxy entirely: "a call to the
async-marked method from within the target object is not
intercepted... self-invocation... effectively bypasses the proxy and
its interceptor chain." The call still compiles and runs — just
synchronously, on the caller's thread, with no error and no warning.

JetBrains Ultimate's Spring plugin already has this exact inspection
for `@Transactional` (`SpringTransactionalMethodCallsInspection`) but
not for `@Async` — confirmed by inspecting the Spring plugin's own
bundled `plugin.xml` before building this.

## Why it exists

`this.notifyWarehouse()` (or the unqualified `notifyWarehouse()`)
compiles fine and looks correct — the method really is annotated
`@Async`. It just never runs asynchronously, because the call never
goes through Spring's proxy. The bug is invisible until someone
notices the "async" call is blocking the caller.

## Why built this way

- **100% static PSI analysis** — matches `@Async` by simple annotation
  name, not by resolving the real Spring classpath symbol, so it works
  whether the real Spring jar is on the classpath or not. Java and
  Kotlin.

## v0.1 scope — stated honestly, not exhaustively

Only flags an unqualified or `this.`-qualified call within the exact
same class — a call through an injected self-reference (the `@Lazy`
proxy workaround) is correctly never flagged, since it's qualified by
a different expression.

## Usage

Open any Java/Kotlin class with an `@Async` method. A call to that
method from elsewhere in the same class shows a warning icon.

## Enterprise / Team Licensing

Need enterprise features, custom rules, or team licensing? Contact us at
**gaphunterlabs@gmail.com**.

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
