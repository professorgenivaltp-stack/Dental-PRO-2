package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale
import com.example.ui.theme.DentalBackground
import com.example.ui.theme.DentalPrimary
import com.example.ui.theme.DentalProTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Configurar idioma padrão do aplicativo para Português do Brasil (pt-BR)
    val ptBrLocale = Locale("pt", "BR")
    Locale.setDefault(ptBrLocale)
    val config = resources.configuration
    config.setLocale(ptBrLocale)
    config.setLayoutDirection(ptBrLocale)
    @Suppress("DEPRECATION")
    resources.updateConfiguration(config, resources.displayMetrics)

    enableEdgeToEdge()
    window.statusBarColor = DentalPrimary.toArgb()
    window.navigationBarColor = DentalBackground.toArgb()

    setContent {
      DentalProTheme {
        Surface(
          modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(DentalBackground)
            .testTag("main_screen_container")
        ) {
          DentalProWebView()
        }
      }
    }
  }
}

class DentalAndroidBridge(private val context: Context, private val webView: WebView) {
  @JavascriptInterface
  fun printDocument(title: String?) {
    val activity = context as? ComponentActivity ?: return
    activity.runOnUiThread {
      val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
      val jobName = title ?: "DentalPro_Extrato_Producao"
      val printAdapter = webView.createPrintDocumentAdapter(jobName)
      val printAttributes = PrintAttributes.Builder()
        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        .build()
      printManager?.print(jobName, printAdapter, printAttributes)
    }
  }

  @JavascriptInterface
  fun savePdfBase64(base64Data: String, filename: String) {
    val activity = context as? ComponentActivity ?: return
    activity.runOnUiThread {
      try {
        val cleanBase64 = base64Data.substringAfter("base64,")
        val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
        val resolver = activity.contentResolver
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = android.content.ContentValues().apply {
              put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
              put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
              put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
              resolver.openOutputStream(uri)?.use { it.write(bytes) }
              android.widget.Toast.makeText(activity, "PDF salvo na pasta Downloads!", android.widget.Toast.LENGTH_LONG).show()
            }
        } else {
            // Fallback for older Android (write directly to Downloads if permitted)
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(downloadsDir, filename)
            java.io.FileOutputStream(file).use { it.write(bytes) }
            android.widget.Toast.makeText(activity, "PDF salvo na pasta Downloads!", android.widget.Toast.LENGTH_LONG).show()
        }
      } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(activity, "Erro ao salvar PDF: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
      }
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DentalProWebView(modifier: Modifier = Modifier) {
  var webViewInstance by remember { mutableStateOf<WebView?>(null) }

  BackHandler(enabled = webViewInstance?.canGoBack() == true) {
    webViewInstance?.goBack()
  }

  AndroidView(
    factory = { context ->
      WebView(context).apply {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        isFocusable = true
        isFocusableInTouchMode = true
        isScrollbarFadingEnabled = true
        scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY

        settings.apply {
          javaScriptEnabled = true
          domStorageEnabled = true
          databaseEnabled = true
          allowFileAccess = true
          allowContentAccess = true
          cacheMode = WebSettings.LOAD_DEFAULT
          useWideViewPort = true
          loadWithOverviewMode = true
          setSupportZoom(false)
          builtInZoomControls = false
          displayZoomControls = false
          mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
          mediaPlaybackRequiresUserGesture = false
        }

        addJavascriptInterface(DentalAndroidBridge(context, this), "DentalNative")

        webChromeClient = WebChromeClient()
        webViewClient = object : WebViewClient() {
          override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
          ) {
            super.onReceivedError(view, request, error)
          }
        }
        loadUrl("file:///android_asset/index.html")
        webViewInstance = this
      }
    },
    modifier = modifier.fillMaxSize().testTag("dentalpro_webview")
  )
}
