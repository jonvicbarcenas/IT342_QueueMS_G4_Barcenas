package edu.cit.barcenas.queuems.dto;

public class AuthResponseDTO {
    private String backendToken;
    private long expiresInMs;

    public AuthResponseDTO(String backendToken, long expiresInMs) {
        this.backendToken = backendToken;
        this.expiresInMs = expiresInMs;
    }

    public String getBackendToken() {
        return backendToken;
    }

    public void setBackendToken(String backendToken) {
        this.backendToken = backendToken;
    }

    public long getExpiresInMs() {
        return expiresInMs;
    }

    public void setExpiresInMs(long expiresInMs) {
        this.expiresInMs = expiresInMs;
    }
}
