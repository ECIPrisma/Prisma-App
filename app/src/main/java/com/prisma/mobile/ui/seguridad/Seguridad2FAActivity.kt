package com.prisma.mobile.ui.seguridad

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.prisma.mobile.PrismaApplication
import com.prisma.mobile.R
import com.prisma.mobile.data.api.ApiClient
import com.prisma.mobile.data.model.TotpCodeRequest
import com.prisma.mobile.data.model.TotpDisableRequest
import com.prisma.mobile.databinding.ActivitySeguridad2faBinding
import kotlinx.coroutines.launch

class Seguridad2FAActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySeguridad2faBinding
    private val sessionManager by lazy { PrismaApplication.instance.sessionManager }
    private var currentSecret: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeguridad2faBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.tvUserEmail.text = sessionManager.getUserEmail() ?: "Usuario PRISMA"

        binding.btnIniciarSetup.setOnClickListener {
            iniciarConfiguracion()
        }

        binding.btnCopySecret.setOnClickListener {
            val secret = currentSecret ?: binding.tvSecretKey.text.toString()
            if (secret.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Secreto TOTP PRISMA", secret))
                Toast.makeText(this, "Clave secreta copiada al portapapeles", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnConfirmarSetup.setOnClickListener {
            val code = binding.etCodeSetup.text.toString().trim()
            if (code.length != 6) {
                binding.tilCodeSetup.error = "Ingresa los 6 dígitos del autenticador"
                binding.etCodeSetup.requestFocus()
                return@setOnClickListener
            }
            binding.tilCodeSetup.error = null
            activarTotp(code)
        }

        binding.btnDesactivar2fa.setOnClickListener {
            mostrarDialogoDesactivar()
        }

        consultarEstado()
    }

    private fun consultarEstado() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = ApiClient.authService.getTotpStatus()
                if (response.isSuccessful && response.body()?.exitoso == true) {
                    val enabled = response.body()?.datos?.enabled == true
                    actualizarUIEstado(enabled)
                } else {
                    val msg = response.body()?.mensaje ?: "No fue posible consultar el estado de 2FA"
                    Toast.makeText(this@Seguridad2FAActivity, msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@Seguridad2FAActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun actualizarUIEstado(enabled: Boolean) {
        if (enabled) {
            binding.tvStatusBadge.text = "ACTIVO Y PROTEGIDO"
            binding.tvStatusBadge.setBackgroundResource(R.drawable.badge_pill_green)
            binding.tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_green_text))
            binding.tvStatusDescription.text = "Tu cuenta está protegida con autenticación de doble paso (TOTP). Se te solicitará un código de 6 dígitos cada vez que inicies sesión en cualquier dispositivo."
            binding.btnIniciarSetup.visibility = View.GONE
            binding.cardSetup.visibility = View.GONE
            binding.btnDesactivar2fa.visibility = View.VISIBLE
        } else {
            binding.tvStatusBadge.text = "INACTIVO"
            binding.tvStatusBadge.setBackgroundResource(R.drawable.badge_pill_promo)
            binding.tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.prisma_orange_dark))
            binding.tvStatusDescription.text = "La autenticación en dos pasos no está activada en tu cuenta. Te sugerimos activarla para proteger las transacciones y pedidos mayoristas."
            binding.btnIniciarSetup.visibility = View.VISIBLE
            binding.btnDesactivar2fa.visibility = View.GONE
        }
    }

    private fun iniciarConfiguracion() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = ApiClient.authService.setupTotp()
                if (response.isSuccessful && response.body()?.exitoso == true) {
                    val setupData = response.body()?.datos
                    if (setupData != null) {
                        currentSecret = setupData.secret
                        binding.tvSecretKey.text = setupData.secret
                        binding.cardSetup.visibility = View.VISIBLE
                        binding.btnIniciarSetup.visibility = View.GONE
                        binding.etCodeSetup.setText("")
                        binding.etCodeSetup.requestFocus()
                        Toast.makeText(this@Seguridad2FAActivity, "Clave secreta generada. Regístrala en tu autenticador.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val msg = response.body()?.mensaje ?: "No fue posible generar la clave de autenticador"
                    Toast.makeText(this@Seguridad2FAActivity, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@Seguridad2FAActivity, "Error al generar setup: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun activarTotp(code: String) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = ApiClient.authService.enableTotp(TotpCodeRequest(code))
                if (response.isSuccessful && response.body()?.exitoso == true) {
                    Toast.makeText(this@Seguridad2FAActivity, "¡Excelente! 2FA activado con éxito en tu cuenta.", Toast.LENGTH_LONG).show()
                    actualizarUIEstado(true)
                } else {
                    val msg = response.body()?.mensaje ?: "Código de autenticación inválido o expirado"
                    binding.tilCodeSetup.error = msg
                    Toast.makeText(this@Seguridad2FAActivity, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@Seguridad2FAActivity, "Error al confirmar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun mostrarDialogoDesactivar() {
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 20, 50, 10)
        }
        val etPass = EditText(this).apply {
            hint = "Contraseña actual"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val etCode = EditText(this).apply {
            hint = "Código actual del autenticador (6 dígitos)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        layout.addView(etPass)
        layout.addView(etCode)

        AlertDialog.Builder(this)
            .setTitle("Desactivar 2FA")
            .setMessage("Por seguridad, ingresa tu contraseña y el código actual para desactivar la protección:")
            .setView(layout)
            .setPositiveButton("Desactivar") { _, _ ->
                val pass = etPass.text.toString().trim()
                val code = etCode.text.toString().trim()
                if (pass.isNotEmpty() && code.isNotEmpty()) {
                    desactivarTotp(pass, code)
                } else {
                    Toast.makeText(this, "Completa todos los campos para desactivar", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun desactivarTotp(pass: String, code: String) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = ApiClient.authService.disableTotp(TotpDisableRequest(pass, code))
                if (response.isSuccessful && response.body()?.exitoso == true) {
                    Toast.makeText(this@Seguridad2FAActivity, "Autenticación 2FA desactivada.", Toast.LENGTH_LONG).show()
                    actualizarUIEstado(false)
                } else {
                    val msg = response.body()?.mensaje ?: "Credenciales o código inválido"
                    Toast.makeText(this@Seguridad2FAActivity, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@Seguridad2FAActivity, "Error al desactivar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnIniciarSetup.isEnabled = !loading
        binding.btnConfirmarSetup.isEnabled = !loading
        binding.btnDesactivar2fa.isEnabled = !loading
    }
}
