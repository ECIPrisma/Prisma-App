package com.prisma.mobile.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.prisma.mobile.PrismaApplication
import com.prisma.mobile.databinding.ActivityMainBinding
import com.prisma.mobile.ui.auth.LoginActivity
import com.prisma.mobile.ui.conductor.ConductorRouteActivity
import com.prisma.mobile.ui.tendero.CatalogoActivity
import com.prisma.mobile.util.Constants

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val sessionManager by lazy { PrismaApplication.instance.sessionManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val rol = sessionManager.getUserRole()
        val nombre = sessionManager.getUserName()

        binding.tvWelcome.text = "Hola, $nombre"
        binding.tvUserRole.text = "Rol: ${rol ?: "Usuario"}"

        binding.btnGoModule.setOnClickListener {
            val intent = when (rol) {
                Constants.ROLE_REPARTIDOR -> Intent(this, ConductorRouteActivity::class.java)
                Constants.ROLE_TENDERO -> Intent(this, CatalogoActivity::class.java)
                else -> Intent(this, CatalogoActivity::class.java)
            }
            startActivity(intent)
        }

        binding.btnLogout.setOnClickListener {
            sessionManager.clearSession()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
