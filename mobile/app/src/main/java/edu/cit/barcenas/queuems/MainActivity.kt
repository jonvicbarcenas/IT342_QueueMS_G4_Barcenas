package edu.cit.barcenas.queuems

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import edu.cit.barcenas.queuems.api.RetrofitClient
import edu.cit.barcenas.queuems.repository.AuthRepository
import edu.cit.barcenas.queuems.ui.login.LoginActivity
import edu.cit.barcenas.queuems.utils.SessionManager
import edu.cit.barcenas.queuems.viewmodel.AuthState
import edu.cit.barcenas.queuems.viewmodel.AuthViewModel
import edu.cit.barcenas.queuems.viewmodel.AuthViewModelFactory

class MainActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sessionManager = SessionManager(this)
        val token = sessionManager.fetchAuthToken()
        
        // Check if user is logged in
        if (token == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val repository = AuthRepository(RetrofitClient.instance)
        val factory = AuthViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]

        setupObservers()
        viewModel.getMe(token)

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            sessionManager.clearAuthToken()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupObservers() {
        viewModel.userProfileState.observe(this) { state ->
            when (state) {
                is AuthState.Success -> {
                    val user = state.data
                    findViewById<TextView>(R.id.tvUserName).text = 
                        "${user.firstname}!"
                }
                is AuthState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    // If unauthorized, logout
                    if (state.message.contains("401") || state.message.contains("profile")) {
                        sessionManager.clearAuthToken()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                }
                else -> {}
            }
        }
    }
}
