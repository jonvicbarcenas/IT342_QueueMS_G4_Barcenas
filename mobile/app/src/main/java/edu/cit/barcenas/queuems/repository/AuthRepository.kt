package edu.cit.barcenas.queuems.repository

import edu.cit.barcenas.queuems.api.ApiService
import edu.cit.barcenas.queuems.api.model.AuthResponse
import edu.cit.barcenas.queuems.api.model.LoginRequest
import edu.cit.barcenas.queuems.api.model.RegisterRequest
import edu.cit.barcenas.queuems.api.model.UserProfile
import retrofit2.Response

class AuthRepository(private val apiService: ApiService) {
    suspend fun register(request: RegisterRequest): Response<String> {
        return apiService.register(request)
    }

    suspend fun login(request: LoginRequest): Response<AuthResponse> {
        return apiService.login(request)
    }

    suspend fun getMe(token: String): Response<UserProfile> {
        return apiService.getMe("Bearer $token")
    }
}
