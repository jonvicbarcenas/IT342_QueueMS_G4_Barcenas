package edu.cit.barcenas.queuems.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp");

    private final RestTemplate restTemplate = new RestTemplate();
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final String folder;

    public FileStorageService(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret,
            @Value("${cloudinary.folder:queuems/service-request-attachments}") String folder) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.folder = folder;
    }

    public StoredFile store(MultipartFile file) throws IOException {
        ensureConfigured();

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Attachment file is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Attachment must be a PDF, JPG, PNG, or WEBP file");
        }

        String originalName = Paths.get(file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment")
                .getFileName()
                .toString();
        String extension = extensionOf(originalName);
        String resourceType = resourceTypeFor(contentType);
        String publicId = publicIdFor(resourceType, extension);
        long timestamp = Instant.now().getEpochSecond();

        Map<String, String> signatureParams = new HashMap<>();
        signatureParams.put("folder", folder);
        signatureParams.put("public_id", publicId);
        signatureParams.put("timestamp", String.valueOf(timestamp));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new MultipartFileResource(file));
        body.add("api_key", apiKey);
        body.add("folder", folder);
        body.add("public_id", publicId);
        body.add("timestamp", timestamp);
        body.add("signature", sign(signatureParams));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        String uploadUrl = String.format("https://api.cloudinary.com/v1_1/%s/%s/upload", cloudName, resourceType);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
                uploadUrl,
                new HttpEntity<>(body, headers),
                Map.class);

        if (response == null || response.get("secure_url") == null || response.get("public_id") == null) {
            throw new IllegalStateException("Cloudinary upload did not return a secure URL");
        }

        return new StoredFile(
                originalName,
                response.get("public_id").toString(),
                contentType,
                response.get("secure_url").toString());
    }

    private String extensionOf(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index).toLowerCase();
    }

    private String resourceTypeFor(String contentType) {
        return "application/pdf".equals(contentType) ? "raw" : "image";
    }

    private String publicIdFor(String resourceType, String extension) {
        String id = UUID.randomUUID().toString();
        if ("raw".equals(resourceType)) {
            return id + extension;
        }
        return id;
    }

    private void ensureConfigured() {
        if (isBlank(cloudName) || isBlank(apiKey) || isBlank(apiSecret)) {
            throw new IllegalStateException(
                    "Cloudinary is not configured. Set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, and CLOUDINARY_API_SECRET.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String sign(Map<String, String> params) {
        String payload = params.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&")) + apiSecret;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte value : hash) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is not available", e);
        }
    }

    private static class MultipartFileResource extends ByteArrayResource {
        private final String filename;

        MultipartFileResource(MultipartFile file) throws IOException {
            super(file.getBytes());
            this.filename = Paths.get(file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment")
                    .getFileName()
                    .toString();
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }

    public record StoredFile(String originalName, String storedName, String contentType, String secureUrl) {
    }
}
