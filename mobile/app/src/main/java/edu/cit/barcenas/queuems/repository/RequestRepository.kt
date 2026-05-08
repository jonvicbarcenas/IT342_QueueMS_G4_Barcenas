package edu.cit.barcenas.queuems.repository

import edu.cit.barcenas.queuems.api.ApiService
import edu.cit.barcenas.queuems.api.model.Counter
import edu.cit.barcenas.queuems.api.model.CreateServiceRequest
import edu.cit.barcenas.queuems.api.model.HolidayStatus
import edu.cit.barcenas.queuems.api.model.QueuePosition
import edu.cit.barcenas.queuems.api.model.ServiceRequest
import okhttp3.MultipartBody
import retrofit2.Response

class RequestRepository(private val apiService: ApiService) {
    suspend fun getTodayHoliday(token: String): Response<HolidayStatus> {
        return apiService.getTodayHoliday("Bearer $token")
    }

    suspend fun getCounters(token: String): Response<List<Counter>> {
        return apiService.getCounters("Bearer $token")
    }

    suspend fun getMyRequests(token: String): Response<List<ServiceRequest>> {
        return apiService.getMyRequests("Bearer $token")
    }

    suspend fun createRequest(token: String, request: CreateServiceRequest): Response<ServiceRequest> {
        return apiService.createRequest("Bearer $token", request)
    }

    suspend fun cancelRequest(token: String, requestId: String): Response<ServiceRequest> {
        return apiService.cancelRequest("Bearer $token", requestId)
    }

    suspend fun getQueuePosition(token: String, requestId: String): Response<QueuePosition> {
        return apiService.getQueuePosition("Bearer $token", requestId)
    }

    suspend fun uploadAttachment(token: String, requestId: String, file: MultipartBody.Part): Response<ServiceRequest> {
        return apiService.uploadAttachment("Bearer $token", requestId, file)
    }
}
