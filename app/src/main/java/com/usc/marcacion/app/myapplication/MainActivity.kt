package com.usc.marcacion.app.myapplication

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    private val CAMERA_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(60) // Cambia esto según tus necesidades
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)

        val defaults: Map<String, Any> = mapOf(
            "web_url" to "https://marcacionapp.usc.edu.co/"
        )
        remoteConfig.setDefaultsAsync(defaults)

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val webUrl = remoteConfig.getString("web_url")
                    loadWebPage(webUrl)
                }
            }


        webView = findViewById(R.id.webView)
        val webSettings = webView.settings
        true.also { webSettings.javaScriptEnabled = it } // Habilitar JavaScript
        webSettings.allowFileAccess = true // Permitir acceso a archivos
        webSettings.allowContentAccess = true // Permitir acceso al contenido

        webView.webChromeClient = MyWebChromeClient()
        webView.webViewClient = WebViewClient()

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                // Aquí puedes decidir si aceptar o rechazar el certificado SSL
                // Por ejemplo, puedes llamar a handler?.proceed() para aceptar certificados no confiables
                // o handler?.cancel() para rechazarlos.
                handler?.proceed()
            }
        }

        val ua = System.getProperty("http.agent")
        webView.settings.userAgentString = "MARCACION_APP|$ua"



        val settings: WebSettings = webView.settings
        settings.userAgentString = settings.userAgentString

        // Verificar y solicitar permiso de la cámara si es necesario
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }




    }


    inner class MyWebChromeClient : WebChromeClient() {


        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            fileUploadCallback?.onReceiveValue(null)
            fileUploadCallback = filePathCallback

            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"

            startActivityForResult(intent, 123) // El número puede ser cualquier valor único

            return true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (requestCode == 123) {
            if (resultCode == Activity.RESULT_OK) {
                intent?.data?.let { uri ->
                    fileUploadCallback?.onReceiveValue(arrayOf(uri))
                    fileUploadCallback = null
                }
            } else {
                fileUploadCallback?.onReceiveValue(null)
                fileUploadCallback = null
            }
        }
    }


    private fun loadWebPage(url: String) {
        webView.loadUrl(url)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

}
