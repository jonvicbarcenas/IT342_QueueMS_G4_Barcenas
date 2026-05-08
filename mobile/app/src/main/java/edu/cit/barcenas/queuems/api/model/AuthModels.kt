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

data class UpdateProfileRequest(
    val firstname: String,
    val lastname: String
)

data class FcmTokenRequest(
    val fcmToken: String
)

data class HolidayStatus(
    val date: String?,
    val holiday: Boolean,
    val name: String?,
    val localName: String?
)

data class Counter(
    val id: String?,
    val name: String?,
    val serviceType: String?,
    val status: String?,
    val assignedTellerId: String?,
    val assignedTellerName: String?
)

data class CreateServiceRequest(
    val counterId: String,
    val serviceType: String,
    val notes: String?
)

data class ServiceRequest(
    val id: String?,
    val userId: String?,
    val counterId: String?,
    val counterName: String?,
    val serviceType: String?,
    val assignedTellerId: String?,
    val assignedTellerName: String?,
    val notes: String?,
    val attachmentOriginalName: String?,
    val attachmentStoredName: String?,
    val attachmentContentType: String?,
    val attachmentUrl: String?,
    val status: String?,
    val queueNumber: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class QueuePosition(
    val requestId: String?,
    val position: Int,
    val totalActive: Int,
    val peopleAhead: Int,
    val estimatedWaitMinutes: Int
)
