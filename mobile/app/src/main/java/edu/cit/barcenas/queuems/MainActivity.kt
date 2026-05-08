package edu.cit.barcenas.queuems

import android.Manifest
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.messaging.FirebaseMessaging
import edu.cit.barcenas.queuems.api.RetrofitClient
import edu.cit.barcenas.queuems.api.model.Counter
import edu.cit.barcenas.queuems.api.model.CreateServiceRequest
import edu.cit.barcenas.queuems.api.model.QueuePosition
import edu.cit.barcenas.queuems.api.model.ServiceRequest
import edu.cit.barcenas.queuems.api.model.UpdateProfileRequest
import edu.cit.barcenas.queuems.api.model.UserProfile
import edu.cit.barcenas.queuems.databinding.ActivityMainBinding
import edu.cit.barcenas.queuems.repository.AuthRepository
import edu.cit.barcenas.queuems.repository.RequestRepository
import edu.cit.barcenas.queuems.service.QueueRealtimeClient
import edu.cit.barcenas.queuems.ui.login.LoginActivity
import edu.cit.barcenas.queuems.utils.SessionManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var authRepository: AuthRepository
    private lateinit var requestRepository: RequestRepository

    private var token: String = ""
    private var currentUser: UserProfile? = null
    private var counters: List<Counter> = emptyList()
    private var requests: List<ServiceRequest> = emptyList()
    private var queuePositions: Map<String, QueuePosition> = emptyMap()
    private var selectedDocumentUri: Uri? = null
    private var selectedDocumentName: String? = null
    private var bookingDisabled = false
    private var realtimeClient: QueueRealtimeClient? = null
    private val notifications = mutableListOf<String>()
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshRequests(showErrors = false)
            refreshHandler.postDelayed(this, 10000)
        }
    }

    private val documentPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedDocumentUri = uri
        selectedDocumentName = uri?.let { getDisplayName(it) }
        binding.tvSelectedFile.text = selectedDocumentName ?: "No supporting document selected"
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        registerFcmToken()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)
        token = sessionManager.fetchAuthToken().orEmpty()
        if (token.isBlank()) {
            goToLogin()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(20.dp + systemBars.left, 20.dp + systemBars.top, 20.dp + systemBars.right, 20.dp + systemBars.bottom)
            insets
        }

        authRepository = AuthRepository(RetrofitClient.instance)
        requestRepository = RequestRepository(RetrofitClient.instance)

        setupListeners()
        loadInitialData()
        requestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        refreshHandler.post(refreshRunnable)
    }

    override fun onPause() {
        refreshHandler.removeCallbacks(refreshRunnable)
        super.onPause()
    }

    override fun onDestroy() {
        realtimeClient?.disconnect()
        super.onDestroy()
    }

    private fun setupListeners() {
        binding.btnPickDocument.setOnClickListener {
            documentPicker.launch("*/*")
        }

        binding.btnCreateRequest.setOnClickListener {
            createRequest()
        }

        binding.btnAccount.setOnClickListener {
            showAccountDialog()
        }

        binding.btnNotifications.setOnClickListener {
            showNotificationsDialog()
        }
    }

    private fun loadInitialData() {
        lifecycleScope.launch {
            loadProfile()
            loadHoliday()
            loadCounters()
            refreshRequests(showErrors = true)
        }
    }

    private suspend fun loadProfile() {
        try {
            val response = authRepository.getMe(token)
            if (response.isSuccessful && response.body() != null) {
                currentUser = response.body()
                renderProfile()
                connectRealtimeQueue()
            } else if (response.code() == 401 || response.code() == 403) {
                logout()
            } else {
                toast("Failed to load profile")
            }
        } catch (e: Exception) {
            toast(e.message ?: "Failed to load profile")
        }
    }

    private suspend fun loadHoliday() {
        try {
            val response = requestRepository.getTodayHoliday(token)
            val holiday = response.body()
            bookingDisabled = response.isSuccessful && holiday?.holiday == true
            if (bookingDisabled) {
                val name = holiday?.localName ?: holiday?.name ?: "Public holiday"
                binding.holidayBanner.visibility = View.VISIBLE
                binding.tvHoliday.text = "Today is $name. Bookings are disabled."
            } else {
                binding.holidayBanner.visibility = View.GONE
            }
            binding.btnCreateRequest.isEnabled = !bookingDisabled
            binding.btnPickDocument.isEnabled = !bookingDisabled
        } catch (_: Exception) {
            bookingDisabled = false
            binding.holidayBanner.visibility = View.GONE
        }
    }

    private suspend fun loadCounters() {
        try {
            val response = requestRepository.getCounters(token)
            counters = response.body().orEmpty().filter { !it.id.isNullOrBlank() }
            val labels = if (counters.isEmpty()) {
                listOf("No open counters")
            } else {
                counters.map { "${it.name ?: "Counter"} - ${it.serviceType ?: "Service"}" }
            }
            binding.spinnerCounters.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
            binding.btnCreateRequest.isEnabled = counters.isNotEmpty() && !bookingDisabled
        } catch (e: Exception) {
            binding.spinnerCounters.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Unable to load counters"))
            toast(e.message ?: "Unable to load counters")
        }
    }

    private fun refreshRequests(showErrors: Boolean) {
        lifecycleScope.launch {
            try {
                val response = requestRepository.getMyRequests(token)
                if (response.isSuccessful) {
                    val next = response.body().orEmpty().sortedByDescending { it.createdAt ?: "" }
                    queuePositions = loadQueuePositions(next)
                    pushStatusNotifications(requests, next)
                    requests = next
                    renderRequests()
                } else if (showErrors) {
                    toast("Failed to load requests")
                }
            } catch (e: Exception) {
                if (showErrors) toast(e.message ?: "Failed to load requests")
            }
        }
    }

    private suspend fun loadQueuePositions(nextRequests: List<ServiceRequest>): Map<String, QueuePosition> {
        return nextRequests
            .filter { it.status == "PENDING" || it.status == "SERVING" }
            .mapNotNull { request ->
                val requestId = request.id ?: return@mapNotNull null
                try {
                    val response = requestRepository.getQueuePosition(token, requestId)
                    val position = response.body()
                    if (response.isSuccessful && position != null) requestId to position else null
                } catch (_: Exception) {
                    null
                }
            }
            .toMap()
    }

    private fun createRequest() {
        if (bookingDisabled) {
            toast("Bookings are disabled today")
            return
        }
        val counter = counters.getOrNull(binding.spinnerCounters.selectedItemPosition)
        val counterId = counter?.id
        if (counterId.isNullOrBlank()) {
            toast("Select an open counter")
            return
        }

        setCreateEnabled(false)
        lifecycleScope.launch {
            try {
                val response = requestRepository.createRequest(
                    token,
                    CreateServiceRequest(counterId, counter.serviceType ?: "General Service", binding.etNotes.text?.toString()?.trim())
                )
                if (response.isSuccessful && response.body() != null) {
                    val created = response.body()!!
                    if (selectedDocumentUri != null) {
                        uploadAttachment(created)
                    } else {
                        afterCreate(created)
                    }
                } else {
                    toast(response.errorBody()?.string() ?: "Failed to create request")
                }
            } catch (e: Exception) {
                toast(e.message ?: "Failed to create request")
            } finally {
                setCreateEnabled(true)
            }
        }
    }

    private suspend fun uploadAttachment(request: ServiceRequest) {
        val uri = selectedDocumentUri ?: return afterCreate(request)
        val requestId = request.id ?: return afterCreate(request)
        try {
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null) {
                toast("Unable to read selected document")
                afterCreate(request)
                return
            }
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", selectedDocumentName ?: "supporting-document", body)
            val response = requestRepository.uploadAttachment(token, requestId, part)
            afterCreate(response.body() ?: request)
        } catch (e: Exception) {
            toast(e.message ?: "Request created, but document upload failed")
            afterCreate(request)
        }
    }

    private fun afterCreate(request: ServiceRequest) {
        selectedDocumentUri = null
        selectedDocumentName = null
        binding.etNotes.setText("")
        binding.tvSelectedFile.text = "No supporting document selected"
        notifications.add(0, "Created request ${request.queueNumber ?: request.id ?: ""}".trim())
        refreshRequests(showErrors = true)
    }

    private fun renderProfile() {
        val user = currentUser ?: return
        binding.tvUserName.text = "${user.firstname} ${user.lastname}".trim()
        binding.tvRoleEmail.text = "${user.role} - ${user.email}"
    }

    private fun connectRealtimeQueue() {
        val uid = currentUser?.uid ?: return
        if (realtimeClient != null) return
        realtimeClient = QueueRealtimeClient(uid) {
            runOnUiThread {
                notifications.add(0, "Queue update received")
                refreshRequests(showErrors = false)
            }
        }.also { it.connect() }
    }

    private fun renderRequests() {
        binding.activeQueueContainer.removeAllViews()
        binding.requestListContainer.removeAllViews()

        val active = requests.filter { it.status == "PENDING" || it.status == "SERVING" }
        if (active.isEmpty()) {
            binding.activeQueueContainer.addView(emptyState("No active queue request"))
        } else {
            active.forEach { request ->
                val position = request.id?.let { queuePositions[it] }
                binding.activeQueueContainer.addView(
                    requestCard(
                        request,
                        showPosition = true,
                        position = position?.position ?: 0,
                        total = position?.totalActive ?: active.size,
                        estimatedWaitMinutes = position?.estimatedWaitMinutes ?: 0,
                        peopleAhead = position?.peopleAhead ?: 0
                    )
                )
            }
        }

        if (requests.isEmpty()) {
            binding.requestListContainer.addView(emptyState("No requests yet"))
        } else {
            requests.forEach { request ->
                binding.requestListContainer.addView(requestCard(request, showPosition = false))
            }
        }
    }

    private fun requestCard(
        request: ServiceRequest,
        showPosition: Boolean,
        position: Int = 0,
        total: Int = 0,
        estimatedWaitMinutes: Int = 0,
        peopleAhead: Int = 0
    ): View {
        val card = MaterialCardView(this).apply {
            radius = 8.dp.toFloat()
            cardElevation = 0f
            setCardBackgroundColor(getColor(R.color.surface))
            strokeWidth = 1.dp
            strokeColor = getColor(R.color.border)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 10.dp
            }
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14.dp, 14.dp, 14.dp, 14.dp)
        }

        val title = TextView(this).apply {
            text = request.queueNumber ?: "Request"
            setTextColor(getColor(R.color.text_primary))
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val subtitle = TextView(this).apply {
            text = listOfNotNull(request.counterName, request.serviceType).joinToString(" - ").ifBlank { "Service request" }
            setTextColor(getColor(R.color.text_secondary))
            textSize = 13f
        }
        val status = TextView(this).apply {
            text = "Status: ${request.status ?: "PENDING"}"
            setTextColor(statusTextColor(request.status))
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        content.addView(title)
        content.addView(subtitle)
        content.addView(status)
        if (showPosition) {
            content.addView(TextView(this).apply {
                text = if (position > 0) {
                    "Position $position of $total - $peopleAhead ahead - about $estimatedWaitMinutes min"
                } else {
                    "Position unavailable"
                }
                setTextColor(getColor(R.color.text_primary))
                textSize = 14f
                setPadding(0, 8.dp, 0, 0)
            })
        }

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 10.dp, 0, 0)
        }
        actions.addView(actionButton("Details") { showRequestDetail(request) })
        if (!request.attachmentUrl.isNullOrBlank()) {
            actions.addView(actionButton("Document") { openAttachment(request) })
        }
        if (request.status == "PENDING") {
            actions.addView(actionButton("Cancel") { confirmCancel(request) })
        }
        content.addView(actions)
        card.addView(content)
        return card
    }

    private fun actionButton(label: String, onClick: () -> Unit): MaterialButton {
        return MaterialButton(this).apply {
            text = label
            isAllCaps = false
            minHeight = 40.dp
            strokeWidth = 1.dp
            strokeColor = ColorStateList.valueOf(getColor(R.color.border))
            setTextColor(getColor(R.color.text_primary))
            backgroundTintList = ColorStateList.valueOf(getColor(R.color.surface))
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 8.dp
            }
        }
    }

    private fun emptyState(message: String): TextView {
        return TextView(this).apply {
            text = message
            setTextColor(getColor(R.color.text_secondary))
            setPadding(14.dp, 14.dp, 14.dp, 14.dp)
            textSize = 13f
        }
    }

    private fun showRequestDetail(request: ServiceRequest) {
        val position = request.id?.let { queuePositions[it] }
        val detail = """
            Queue: ${request.queueNumber ?: "N/A"}
            Status: ${request.status ?: "PENDING"}
            Counter: ${request.counterName ?: "N/A"}
            Service: ${request.serviceType ?: "N/A"}
            Position: ${position?.position ?: "N/A"} of ${position?.totalActive ?: "N/A"}
            Estimated Wait: ${position?.estimatedWaitMinutes ?: 0} minutes
            Teller: ${request.assignedTellerName ?: "Unassigned"}
            Notes: ${request.notes ?: "None"}
            Document: ${request.attachmentOriginalName ?: "None"}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Request Details")
            .setMessage(detail)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun confirmCancel(request: ServiceRequest) {
        AlertDialog.Builder(this)
            .setTitle("Cancel request?")
            .setMessage("This will cancel ${request.queueNumber ?: "this request"}.")
            .setNegativeButton("Keep", null)
            .setPositiveButton("Cancel Request") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val id = request.id ?: return@launch
                        val response = requestRepository.cancelRequest(token, id)
                        if (response.isSuccessful) {
                            notifications.add(0, "Cancelled ${request.queueNumber ?: id}")
                            refreshRequests(showErrors = true)
                        } else {
                            toast(response.errorBody()?.string() ?: "Unable to cancel request")
                        }
                    } catch (e: Exception) {
                        toast(e.message ?: "Unable to cancel request")
                    }
                }
            }
            .show()
    }

    private fun openAttachment(request: ServiceRequest) {
        val url = request.attachmentUrl ?: "${RetrofitClient.BASE_URL}api/requests/${request.id}/attachment"
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun showAccountDialog() {
        val user = currentUser ?: return
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp, 8.dp, 24.dp, 0)
        }
        val firstName = EditText(this).apply {
            hint = "First name"
            setText(user.firstname)
        }
        val lastName = EditText(this).apply {
            hint = "Last name"
            setText(user.lastname)
        }
        content.addView(firstName)
        content.addView(lastName)

        AlertDialog.Builder(this)
            .setTitle(user.email)
            .setView(content)
            .setNeutralButton("Sign Out") { _, _ -> logout() }
            .setNegativeButton("Close", null)
            .setPositiveButton("Save") { _, _ ->
                updateProfile(firstName.text.toString().trim(), lastName.text.toString().trim())
            }
            .show()
    }

    private fun updateProfile(firstName: String, lastName: String) {
        if (firstName.isBlank() || lastName.isBlank()) {
            toast("First and last name are required")
            return
        }
        lifecycleScope.launch {
            try {
                val response = authRepository.updateMe(token, UpdateProfileRequest(firstName, lastName))
                if (response.isSuccessful && response.body() != null) {
                    currentUser = response.body()
                    renderProfile()
                    toast("Profile updated")
                } else {
                    toast(response.errorBody()?.string() ?: "Unable to update profile")
                }
            } catch (e: Exception) {
                toast(e.message ?: "Unable to update profile")
            }
        }
    }

    private fun showNotificationsDialog() {
        val feed = notifications.ifEmpty { listOf("No notifications yet") }.joinToString("\n\n")
        AlertDialog.Builder(this)
            .setTitle("Notifications")
            .setMessage(feed)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun pushStatusNotifications(old: List<ServiceRequest>, next: List<ServiceRequest>) {
        val oldById = old.mapNotNull { request -> request.id?.let { it to request.status } }.toMap()
        next.forEach { request ->
            val previous = oldById[request.id]
            if (previous != null && previous != request.status) {
                notifications.add(0, "${request.queueNumber ?: "Request"} changed to ${request.status ?: "UPDATED"}")
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            registerFcmToken()
        }
    }

    private fun registerFcmToken() {
        try {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { fcmToken ->
                    sessionManager.saveFcmToken(fcmToken)
                    lifecycleScope.launch {
                        try {
                            authRepository.updateFcmToken(token, fcmToken)
                        } catch (_: Exception) {
                        }
                    }
                }
                .addOnFailureListener {
                    sessionManager.fetchFcmToken()?.let { cached ->
                        lifecycleScope.launch {
                            try {
                                authRepository.updateFcmToken(token, cached)
                            } catch (_: Exception) {
                            }
                        }
                    }
                }
        } catch (_: IllegalStateException) {
            sessionManager.fetchFcmToken()?.let { cached ->
                lifecycleScope.launch {
                    try {
                        authRepository.updateFcmToken(token, cached)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    private fun logout() {
        sessionManager.clearAuthToken()
        goToLogin()
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun setCreateEnabled(enabled: Boolean) {
        binding.btnCreateRequest.isEnabled = enabled && counters.isNotEmpty() && !bookingDisabled
        binding.btnPickDocument.isEnabled = enabled && !bookingDisabled
    }

    private fun getDisplayName(uri: Uri): String {
        val fallback = uri.lastPathSegment ?: "supporting-document"
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else fallback
        } ?: fallback
    }

    private fun statusTextColor(status: String?): Int {
        return when (status) {
            "SERVING" -> getColor(R.color.serving_text)
            "COMPLETED" -> getColor(R.color.completed_text)
            "CANCELLED" -> getColor(R.color.cancelled_text)
            else -> getColor(R.color.pending_text)
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}
