package dev.gaphunter.asyncselfinvocationcompanion.detect

import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import dev.gaphunter.asyncselfinvocationcompanion.model.SelfInvocationHit

/**
 * Finds a call to an `@Async` method made from another method of the
 * *same* declaring class, unqualified or via `this.` -- Spring's own
 * documentation states this bypasses the AOP proxy entirely, so the
 * call runs synchronously on the caller's thread with no error, no
 * warning, silently defeating the whole point of `@Async`.
 *
 * **v0.1 scope, stated honestly:** matches `@Async` by simple
 * annotation name, not by resolving the real Spring classpath symbol
 * -- an unrelated custom `@Async` annotation from a different library
 * is a possible (rare) false positive. Only unqualified/`this.`-
 * qualified calls within the exact same class are flagged; a call
 * through an injected self-reference (`@Lazy` proxy workaround) is
 * correctly never flagged, since it's qualified by a different
 * expression.
 */
object JavaSelfInvocationFinder {

    fun findAll(file: PsiFile): List<SelfInvocationHit> {
        if (file !is PsiJavaFile) return emptyList()
        val hits = mutableListOf<SelfInvocationHit>()
        file.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitClass(aClass: PsiClass) {
                super.visitClass(aClass)
                hits += findInClass(aClass)
            }
        })
        return hits
    }

    private fun findInClass(psiClass: PsiClass): List<SelfInvocationHit> {
        val asyncMethodNames = psiClass.methods
            .filter { method -> method.annotations.any { AsyncSignals.isAsyncAnnotationName(it.qualifiedName?.substringAfterLast('.') ?: "") } }
            .mapNotNull { it.name }
            .toSet()
        if (asyncMethodNames.isEmpty()) return emptyList()

        val hits = mutableListOf<SelfInvocationHit>()
        for (method in psiClass.methods) {
            method.body?.accept(object : JavaRecursiveElementWalkingVisitor() {
                override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                    super.visitMethodCallExpression(expression)
                    val name = expression.methodExpression.referenceName ?: return
                    if (name !in asyncMethodNames) return

                    val qualifier = expression.methodExpression.qualifierExpression
                    val isSelfCall = qualifier == null || qualifier.text == "this"
                    if (!isSelfCall) return

                    // Don't flag the @Async method calling itself recursively via a different code path check --
                    // a call from within the async method's own body to itself is still a self-invocation bug.
                    hits += SelfInvocationHit(name, leafOf(expression.methodExpression))
                }
            })
        }
        return hits
    }

    /** Descends to a real leaf PSI element -- LineMarkerInfo must never anchor on a composite node. */
    private fun leafOf(element: PsiElement): PsiElement {
        var current = element
        while (current.firstChild != null) current = current.firstChild
        return current
    }
}
