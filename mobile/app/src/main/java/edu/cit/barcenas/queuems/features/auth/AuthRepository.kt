package edu.cit.barcenas.queuems.features.auth

import edu.cit.barcenas.queuems.api.ApiService
import edu.cit.barcenas.queuems.api.model.AuthResponse
import edu.cit.barcenas.queuems.api.model.FcmTokenRequest
import edu.cit.barcenas.queuems.api.model.LoginRequest
import edu.cit.barcenas.queuems.api.model.RegisterRequest
import edu.cit.barcenas.queuems.api.model.UpdateProfileRequest
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

    suspend fun updateMe(token: String, request: UpdateProfileRequest): Response<UserProfile> {
        return apiService.updateMe("Bearer $token", request)
    }

    suspend fun updateFcmToken(token: String, fcmToken: String): Response<Unit> {
        return apiService.updateFcmToken("Bearer $token", FcmTokenRequest(fcmToken))
    }
}
