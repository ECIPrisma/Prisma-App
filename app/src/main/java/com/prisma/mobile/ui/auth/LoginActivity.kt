package com.prisma.mobile.ui.auth

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

        // Botón rápido para rol Tendero
        binding.btnQuickTendero.setOnClickListener {
            binding.etEmail.setText("dona.marta@labendicion.com")
            binding.etPassword.setText("admin123")
            binding.tilTotp.visibility = View.GONE
            binding.tilTotp.error = null
            binding.btnLogin.text = "INGRESAR A PRISMA"
        }

        // Botón rápido para rol Conductor
        binding.btnQuickConductor.setOnClickListener {
            binding.etEmail.setText("conductor.juan@femsa.com.co")
            binding.etPassword.setText("admin123")
            binding.tilTotp.visibility = View.GONE
            binding.tilTotp.error = null
            binding.btnLogin.text = "INGRESAR A PRISMA"
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val totpCode = if (binding.tilTotp.visibility == View.VISIBLE) {
                binding.etTotp.text.toString().trim()
            } else {
                null
            }

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (binding.tilTotp.visibility == View.VISIBLE) {
                if (totpCode.isNullOrEmpty() || totpCode.length != 6) {
                    binding.tilTotp.error = "Ingresa los 6 dígitos de tu aplicación autenticadora"
                    binding.etTotp.requestFocus()
                    return@setOnClickListener
                } else {
                    binding.tilTotp.error = null
                }
            }

            executeLogin(email, password, totpCode)
        }
    }

    private fun executeLogin(email: String, pass: String, totpCode: String? = null) {
        binding.btnLogin.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = ApiClient.authService.login(LoginRequest(email, pass, totpCode))
                if (response.isSuccessful && response.body()?.exitoso == true) {
                    val authData = response.body()?.datos
                    if (authData != null) {
                        sessionManager.saveAuthSession(authData)
                        Toast.makeText(this@LoginActivity, "¡Bienvenido, ${authData.nombreCompleto}!", Toast.LENGTH_SHORT).show()
                        navigateByRole(authData.rol)
                        finish()
                    }
                } else {
                    var errorMsg = response.body()?.mensaje
                    if (errorMsg.isNullOrEmpty()) {
                        try {
                            val errorStr = response.errorBody()?.string() ?: ""
                            val json = org.json.JSONObject(errorStr)
                            errorMsg = json.optString("mensaje", "")
                        } catch (_: Exception) {}
                    }
                    if (errorMsg.isNullOrEmpty()) {
                        errorMsg = if (response.code() == 401) "Credenciales inválidas" else "Error del servidor (${response.code()})"
                    }

                    val requiresTotp = errorMsg.contains("TOTP", ignoreCase = true) ||
                            (errorMsg.contains("código", ignoreCase = true) && errorMsg.contains("6", ignoreCase = true))

                    if (requiresTotp) {
                        binding.tilTotp.visibility = View.VISIBLE
                        binding.etTotp.requestFocus()
                        binding.btnLogin.text = "CONFIRMAR CÓDIGO 2FA"
                        Toast.makeText(this@LoginActivity, "Seguridad PRISMA: Ingresa tu código de autenticación en dos pasos", Toast.LENGTH_LONG).show()
                    } else {
                        if (binding.tilTotp.visibility == View.VISIBLE) {
                            binding.etTotp.setText("")
                            binding.tilTotp.error = errorMsg
                        }
                        Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
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
