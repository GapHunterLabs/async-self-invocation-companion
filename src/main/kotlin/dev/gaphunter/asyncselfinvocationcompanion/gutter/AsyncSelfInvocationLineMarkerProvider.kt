package dev.gaphunter.asyncselfinvocationcompanion.gutter

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import dev.gaphunter.asyncselfinvocationcompanion.detect.JavaSelfInvocationFinder
import dev.gaphunter.asyncselfinvocationcompanion.detect.KotlinSelfInvocationFinder
import dev.gaphunter.asyncselfinvocationcompanion.model.SelfInvocationHit
import dev.gaphunter.asyncselfinvocationcompanion.review.ReviewPrompt

class AsyncSelfInvocationLineMarkerProvider : LineMarkerProviderDescriptor(), DumbAware {

    override fun getName(): String = "@Async self-invocation"

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(elements: MutableList<out PsiElement>, result: MutableCollection<in LineMarkerInfo<*>>) {
        val file = elements.firstOrNull()?.containingFile ?: return
        val hits = when (file.language.id) {
            "JAVA" -> JavaSelfInvocationFinder.findAll(file)
            "kotlin" -> KotlinSelfInvocationFinder.findAll(file)
            else -> emptyList()
        }
        if (hits.isEmpty()) return

        val hitsByElement = hits.associateBy { it.callElement }
        for (element in elements) {
            val hit = hitsByElement[element] ?: continue
            result.add(buildMarker(hit))

            val path = file.virtualFile?.path ?: continue
            val lineNumber = file.viewProvider.document?.getLineNumber(element.textRange.startOffset) ?: -1
            ReviewPrompt.recordHit(file.project, "$path:$lineNumber")
        }
    }

    private fun buildMarker(hit: SelfInvocationHit): LineMarkerInfo<PsiElement> {
        val tooltip = "This calls @Async method '${hit.methodName}' from within its own declaring class -- " +
            "Spring's AOP proxy is bypassed on self-invocation, so this runs synchronously with no error or warning"
        return LineMarkerInfo(
            hit.callElement,
            hit.callElement.textRange,
            SelfInvocationIcons.RISK,
            { _: PsiElement -> tooltip },
            null,
            GutterIconRenderer.Alignment.RIGHT,
            { tooltip },
        )
    }
}
