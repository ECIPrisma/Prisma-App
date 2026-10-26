package com.prisma.mobile.ui.auth

import com.prisma.mobile.BuildConfig
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.prisma.mobile.PrismaApplication
import com.prisma.mobile.data.api.ApiClient
import com.prisma.mobile.data.model.LoginRequest
import com.prisma.mobile.databinding.ActivityLoginBinding
import com.prisma.mobile.ui.conductor.ConductorRouteActivity
import com.prisma.mobile.ui.home.MainActivity
import com.prisma.mobile.ui.tendero.CatalogoActivity
import com.prisma.mobile.util.Constants
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val sessionManager by lazy { PrismaApplication.instance.sessionManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Si ya tiene sesión activa, redirigir directo al módulo según rol
        if (sessionManager.isLoggedIn()) {
            navigateByRole(sessionManager.getUserRole())
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Reflejar URL actual configurada
        updateServerDisplay()

        // Accesos demo y credenciales de prueba: SOLO en builds debug (N-05)
        binding.demoSelectors.visibility =
            if (BuildConfig.DEBUG) android.view.View.VISIBLE else android.view.View.GONE

        // Botón rápido para rol Tendero
        binding.btnQuickTendero.setOnClickListener {
            binding.etEmail.setText("dona.marta@labendicion.com")
            binding.etPassword.setText("admin123")
        }

        // Botón rápido para rol Conductor
        binding.btnQuickConductor.setOnClickListener {
            binding.etEmail.setText("conductor.juan@femsa.com.co")
            binding.etPassword.setText("admin123")
        }

        // Diálogo para cambiar la IP/URL del servidor fácilmente
        binding.tvServerConfig.setOnClickListener {
            showServerConfigDialog()
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            executeLogin(email, password)
        }
    }

    private fun updateServerDisplay() {
        binding.tvServerConfig.text = "Servidor: ${sessionManager.getServerUrl()} (Toca para editar)"
    }

    private fun showServerConfigDialog() {
        val options = arrayOf(
            "Servidor Sabio Público (Acceso 4G/Internet) [Predeterminado]",
            "Servidor Sabio Red Tailscale",
            "Emulador Android Local (10.0.2.2:8080)",
            "Red Wi-Fi Local (${Constants.SERVER_WIFI_IP}:8080)",
            "Ingresar URL personalizada..."
        )

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Seleccionar Servidor")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> applyNewServerUrl(Constants.SERVER_SABIO_PUBLIC_URL)
                    1 -> applyNewServerUrl(Constants.SERVER_SABIO_VPN_URL)
                    2 -> applyNewServerUrl(Constants.EMULATOR_BASE_URL)
                    3 -> applyNewServerUrl(Constants.WIFI_BASE_URL)
                    4 -> promptCustomServerUrl()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun promptCustomServerUrl() {
        val input = android.widget.EditText(this).apply {
            setText(sessionManager.getServerUrl())
            setSelection(text.length)
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Ingresar URL Personalizada")
            .setMessage("Ejemplo: http://${Constants.SERVER_WIFI_IP}:8080/api/v1/")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val newUrl = input.text.toString().trim()
                if (newUrl.isNotEmpty()) {
                    applyNewServerUrl(newUrl)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun applyNewServerUrl(url: String) {
        val formatted = if (url.endsWith("/")) url else "$url/"
        sessionManager.setServerUrl(formatted)
        ApiClient.updateBaseUrl(formatted)
        updateServerDisplay()
        Toast.makeText(this, "Servidor configurado:\n$formatted", Toast.LENGTH_SHORT).show()
    }

    private fun executeLogin(email: String, pass: String) {
        binding.btnLogin.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = ApiClient.authService.login(LoginRequest(email, pass))
                if (response.isSuccessful && response.body()?.exitoso == true) {
                    val authData = response.body()?.datos
                    if (authData != null) {
                        sessionManager.saveAuthSession(authData)
                        Toast.makeText(this@LoginActivity, "¡Bienvenido, ${authData.nombreCompleto}!", Toast.LENGTH_SHORT).show()
                        navigateByRole(authData.rol)
                        finish()
                    }
                } else {
                    val errorMsg = response.body()?.mensaje ?: "Credenciales inválidas"
                    Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                val currentUrl = sessionManager.getServerUrl()
                val hint = if (Constants.isEmulator() && !currentUrl.contains("10.0.2.2")) {
                    "\nSugerencia: En emulador selecciona 10.0.2.2 tocando abajo."
                } else if (!Constants.isEmulator() && currentUrl.contains("10.0.2.2")) {
                    "\nSugerencia: En celular físico selecciona Wi-Fi (${Constants.SERVER_WIFI_IP})."
                } else {
                    "\nVerifica que el backend esté corriendo en la PC (puerto 8080)."
                }
                Toast.makeText(this@LoginActivity, "Error al conectar: ${e.localizedMessage}$hint", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnLogin.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun navigateByRole(role: String?) {
        val intent = when (role) {
            Constants.ROLE_REPARTIDOR -> Intent(this, ConductorRouteActivity::class.java)
            Constants.ROLE_TENDERO -> Intent(this, CatalogoActivity::class.java)
            else -> Intent(this, MainActivity::class.java)
        }
        startActivity(intent)
    }
}
