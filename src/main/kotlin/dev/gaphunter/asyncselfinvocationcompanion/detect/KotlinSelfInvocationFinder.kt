package dev.gaphunter.asyncselfinvocationcompanion.detect

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import dev.gaphunter.asyncselfinvocationcompanion.model.SelfInvocationHit
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

/** Kotlin counterpart of [JavaSelfInvocationFinder]. */
object KotlinSelfInvocationFinder {

    fun findAll(file: PsiFile): List<SelfInvocationHit> {
        if (file !is KtFile) return emptyList()
        val hits = mutableListOf<SelfInvocationHit>()
        file.accept(object : KtTreeVisitorVoid() {
            override fun visitClass(klass: KtClass) {
                super.visitClass(klass)
                hits += findInClass(klass)
            }
        })
        return hits
    }

    private fun findInClass(klass: KtClass): List<SelfInvocationHit> {
        val functions = klass.declarations.filterIsInstance<KtNamedFunction>()
        val asyncMethodNames = functions
            .filter { function -> function.annotationEntries.any { AsyncSignals.isAsyncAnnotationName(it.shortName?.asString() ?: "") } }
            .mapNotNull { it.name }
            .toSet()
        if (asyncMethodNames.isEmpty()) return emptyList()

        val hits = mutableListOf<SelfInvocationHit>()
        for (function in functions) {
            function.bodyExpression?.accept(object : KtTreeVisitorVoid() {
                override fun visitCallExpression(expression: KtCallExpression) {
                    super.visitCallExpression(expression)
                    val name = expression.calleeExpression?.text ?: return
                    if (name !in asyncMethodNames) return

                    // A call qualified by a dot (obj.method()) is a different receiver, not a self-invocation
                    // in the buggy sense -- unless the qualifier is literally `this`. Only the *immediate*
                    // parent counts as "this call's own qualifier" -- an outer unrelated dot-qualified
                    // expression (e.g. this call is itself an argument to some other qualified call) must
                    // never be mistaken for this call's qualifier.
                    val immediateParent = expression.parent as? KtDotQualifiedExpression
                    val isDirectlyQualified = immediateParent != null && immediateParent.selectorExpression === expression
                    val receiverText = if (isDirectlyQualified) immediateParent?.receiverExpression?.text else null
                    val isSelfCall = !isDirectlyQualified || receiverText == "this"
                    if (!isSelfCall) return

                    hits += SelfInvocationHit(name, leafOf(expression.calleeExpression!!))
                }
            })
        }
        return hits
    }

    private fun leafOf(element: PsiElement): PsiElement {
        var current = element
        while (current.firstChild != null) current = current.firstChild
        return current
    }
}
