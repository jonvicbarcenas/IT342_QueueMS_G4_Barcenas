package edu.cit.barcenas.queuems.api

import edu.cit.barcenas.queuems.api.model.AuthResponse
import edu.cit.barcenas.queuems.api.model.Counter
import edu.cit.barcenas.queuems.api.model.CreateServiceRequest
import edu.cit.barcenas.queuems.api.model.FcmTokenRequest
import edu.cit.barcenas.queuems.api.model.HolidayStatus
import edu.cit.barcenas.queuems.api.model.LoginRequest
import edu.cit.barcenas.queuems.api.model.QueuePosition
import edu.cit.barcenas.queuems.api.model.RegisterRequest
import edu.cit.barcenas.queuems.api.model.ServiceRequest
import edu.cit.barcenas.queuems.api.model.UpdateProfileRequest
import edu.cit.barcenas.queuems.api.model.UserProfile
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<String>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("api/auth/me")
    suspend fun getMe(@Header("Authorization") token: String): Response<UserProfile>

    @PUT("api/auth/me")
    suspend fun updateMe(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): Response<UserProfile>

    @PUT("api/auth/fcm-token")
    suspend fun updateFcmToken(
        @Header("Authorization") token: String,
        @Body request: FcmTokenRequest
    ): Response<Unit>

    @GET("api/holidays/today")
    suspend fun getTodayHoliday(@Header("Authorization") token: String): Response<HolidayStatus>

    @GET("api/counters")
    suspend fun getCounters(@Header("Authorization") token: String): Response<List<Counter>>

    @GET("api/requests/me")
    suspend fun getMyRequests(@Header("Authorization") token: String): Response<List<ServiceRequest>>

    @POST("api/requests")
    suspend fun createRequest(
        @Header("Authorization") token: String,
        @Body request: CreateServiceRequest
    ): Response<ServiceRequest>

    @DELETE("api/requests/{id}")
    suspend fun cancelRequest(
        @Header("Authorization") token: String,
        @Path("id") requestId: String
    ): Response<ServiceRequest>

    @GET("api/requests/{id}/position")
    suspend fun getQueuePosition(
        @Header("Authorization") token: String,
        @Path("id") requestId: String
    ): Response<QueuePosition>

    @Multipart
    @POST("api/requests/{id}/attachment")
    suspend fun uploadAttachment(
        @Header("Authorization") token: String,
        @Path("id") requestId: String,
        @Part file: MultipartBody.Part
    ): Response<ServiceRequest>
}
