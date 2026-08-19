package com.denggl2.masonremote.ui

import android.graphics.Color as AndroidColor
import android.view.View
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.ceil

@Composable
internal fun FigmaSvgAsset(
    assetPath: String,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = false,
) {
    val context = LocalContext.current
    val svg = remember(assetPath) {
        context.assets.open(assetPath).bufferedReader().use { it.readText() }
    }
    val svgKey = remember(assetPath, darkTheme) {
        Triple(assetPath, darkTheme, svg.hashCode())
    }
    val webView = remember(context) {
        WebView(context).apply {
            setBackgroundColor(AndroidColor.TRANSPARENT)
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            isHorizontalScrollBarEnabled = false
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            settings.javaScriptEnabled = false
            settings.domStorageEnabled = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
        }
    }
    DisposableEffect(webView, svgKey) {
        fun loadContent() {
            if (webView.width <= 0 || webView.height <= 0) return
            val density = webView.resources.displayMetrics.density.coerceAtLeast(1f)
            val viewportWidth = ceil(webView.width / density).toInt().coerceAtLeast(1)
            val viewportHeight = ceil(webView.height / density).toInt().coerceAtLeast(1)
            val contentKey = Triple(svgKey, viewportWidth, viewportHeight)
            if (webView.tag == contentKey) return
            webView.loadDataWithBaseURL(
                null,
                buildFigmaSvgDocument(svg, viewportWidth, viewportHeight, darkTheme),
                "text/html",
                "UTF-8",
                null,
            )
            webView.tag = contentKey
        }
        val listener = View.OnLayoutChangeListener { view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (view.width > 0 && view.height > 0 &&
                (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop)
            ) loadContent()
        }
        webView.addOnLayoutChangeListener(listener)
        if (webView.isLaidOut) loadContent()
        onDispose {
            webView.removeOnLayoutChangeListener(listener)
            webView.stopLoading()
        }
    }
    AndroidView(factory = { webView }, modifier = modifier)
}

private fun buildFigmaSvgDocument(
    svg: String,
    viewportWidth: Int,
    viewportHeight: Int,
    darkTheme: Boolean,
): String {
    val filter = if (darkTheme) " filter: invert(1) brightness(0.72);" else " opacity: 0.55;"
    return """
        <!doctype html>
        <html><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
          html, body { width: ${viewportWidth}px; height: ${viewportHeight}px; margin: 0; padding: 0; overflow: hidden; background: transparent; }
          body { display: flex; align-items: center; justify-content: center; }
          body > svg { display: block !important; width: ${viewportWidth}px !important; height: ${viewportHeight}px !important; max-width: ${viewportWidth}px !important; max-height: ${viewportHeight}px !important;$filter }
        </style></head><body>$svg</body></html>
    """.trimIndent()
}
