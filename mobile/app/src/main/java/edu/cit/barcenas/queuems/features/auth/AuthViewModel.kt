package edu.cit.barcenas.queuems.features.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.cit.barcenas.queuems.api.model.AuthResponse
import edu.cit.barcenas.queuems.api.model.LoginRequest
import edu.cit.barcenas.queuems.api.model.RegisterRequest
import edu.cit.barcenas.queuems.api.model.UserProfile
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import retrofit2.Response

sealed class AuthState<out T> {
    object Idle : AuthState<Nothing>()
    object Loading : AuthState<Nothing>()
    data class Success<T>(val data: T) : AuthState<T>()
    data class Error(val message: String) : AuthState<Nothing>()
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _registerState = MutableLiveData<AuthState<String>>(AuthState.Idle)
    val registerState: LiveData<AuthState<String>> get() = _registerState

    private val _loginState = MutableLiveData<AuthState<AuthResponse>>(AuthState.Idle)
    val loginState: LiveData<AuthState<AuthResponse>> get() = _loginState

    private val _userProfileState = MutableLiveData<AuthState<UserProfile>>(AuthState.Idle)
    val userProfileState: LiveData<AuthState<UserProfile>> get() = _userProfileState

    fun register(request: RegisterRequest) {
        viewModelScope.launch {
            _registerState.value = AuthState.Loading
            try {
                val response = repository.register(request)
                if (response.isSuccessful) {
                    _registerState.value = AuthState.Success(response.body() ?: "User created")
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Registration failed"
                    _registerState.value = AuthState.Error(errorBody)
                }
            } catch (e: Exception) {
                _registerState.value = AuthState.Error(toAuthErrorMessage(e))
            }
        }
    }

    fun login(request: LoginRequest) {
        viewModelScope.launch {
            _loginState.value = AuthState.Loading
            try {
                val response = repository.login(request)
                if (response.isSuccessful) {
                    _loginState.value = AuthState.Success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Login failed"
                    _loginState.value = AuthState.Error(errorBody)
                }
            } catch (e: Exception) {
                _loginState.value = AuthState.Error(toAuthErrorMessage(e))
            }
        }
    }

    fun getMe(token: String) {
        viewModelScope.launch {
            _userProfileState.value = AuthState.Loading
            try {
                val response = repository.getMe(token)
                if (response.isSuccessful) {
                    _userProfileState.value = AuthState.Success(response.body()!!)
                } else {
                    _userProfileState.value = AuthState.Error("Failed to fetch profile")
                }
            } catch (e: Exception) {
                _userProfileState.value = AuthState.Error(toAuthErrorMessage(e))
            }
        }
    }

    private fun toAuthErrorMessage(error: Exception): String {
        return when (error) {
            is ConnectException -> "Cannot reach QueueMS backend. Check that the backend is running and the mobile API base URL is correct."
            is SocketTimeoutException -> "QueueMS backend timed out. Check your network and backend server."
            else -> error.message ?: "An error occurred"
        }
    }
}
