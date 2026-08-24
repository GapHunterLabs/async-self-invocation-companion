package dev.gaphunter.asyncselfinvocationcompanion.model

import com.intellij.psi.PsiElement

/** One self-invocation call site of an `@Async` method from within its own declaring class. */
data class SelfInvocationHit(val methodName: String, val callElement: PsiElement)
