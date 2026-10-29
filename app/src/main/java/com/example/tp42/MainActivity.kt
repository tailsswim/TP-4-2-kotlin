package com.example.internetapp

import android.R
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private var btnTelecharger: Button? = null
    private var txtResultat: TextView? = null

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnTelecharger = findViewById<Button?>(R.id.btnTelecharger)
        txtResultat = findViewById<TextView?>(R.id.txtResultat)

        btnTelecharger!!.setOnClickListener(View.OnClickListener { v: View? ->
            telechargerDonnees()
        })
    }

    // Vérifier si un réseau permet l'accès à Internet
    private fun verifierConnexionInternet(): Boolean {
        val cm =
            getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager?

        if (cm == null) {
            return false
        }

        val network = cm.getActiveNetwork()

        if (network == null) {
            return false
        }

        val capabilities =
            cm.getNetworkCapabilities(network)

        return capabilities != null && capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        )
                && capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
        )
    }

    // Télécharger les données en arrière-plan
    private fun telechargerDonnees() {
        if (!verifierConnexionInternet()) {
            txtResultat!!.setText(
                "Aucune connexion Internet disponible."
            )
            return
        }

        txtResultat!!.setText("Connexion en cours...")
        btnTelecharger!!.setEnabled(false)

        executor.execute(Runnable {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(
                    "https://jsonplaceholder.typicode.com/posts/1"
                )

                connection =
                    url.openConnection() as HttpURLConnection?

                connection!!.setRequestMethod("GET")
                connection.setConnectTimeout(10000)
                connection.setReadTimeout(10000)

                val codeHTTP = connection.getResponseCode()

                if (codeHTTP < 200 || codeHTTP >= 300) {
                    throw IOException(
                        "Erreur HTTP : " + codeHTTP
                    )
                }

                val resultat = StringBuilder()

                connection.getInputStream().use { input ->
                    BufferedReader(
                        InputStreamReader(
                            input, "UTF-8"
                        )
                    ).use { reader ->
                        var ligne: String?
                        while ((reader.readLine().also { ligne = it }) != null) {
                            resultat.append(ligne)
                        }
                    }
                }
                val json =
                    JSONObject(resultat.toString())

                val titre = json.getString("title")
                val contenu = json.getString("body")
                val id = json.getInt("id")

                val affichage =
                    ("Connexion réussie !\n\n"
                            + "ID : " + id + "\n\n"
                            + "Titre : " + titre + "\n\n"
                            + "Contenu : " + contenu)

                runOnUiThread(Runnable { txtResultat!!.setText(affichage) }
                )
            } catch (e: Exception) {
                runOnUiThread(Runnable {
                    txtResultat!!.setText(
                        "Erreur : " + e.message
                    )
                }
                )
            } finally {
                if (connection != null) {
                    connection.disconnect()
                }

                runOnUiThread(Runnable { btnTelecharger!!.setEnabled(true) }
                )
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdownNow()
    }
}