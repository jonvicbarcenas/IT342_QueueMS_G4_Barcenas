package edu.cit.barcenas.queuems.api.model

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    val email: String,
    val password: String,
    val firstname: String,
    val lastname: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    @SerializedName("backendToken")
    val token: String,
    @SerializedName("expiresInMs")
    val expiresAt: Long
)

data class UserProfile(
    val uid: String,
    val email: String,
    val firstname: String,
    val lastname: String,
    val role: String
)
