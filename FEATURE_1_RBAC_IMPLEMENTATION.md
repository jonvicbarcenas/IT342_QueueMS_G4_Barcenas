# Feature 1: Role-Based Access Control (RBAC) Implementation

## Overview
This document describes the implementation of Role-Based Access Control (RBAC) for the Queue Management System backend, as outlined in the development plan.

## Feature Goals
- Implement access control for three roles: `SUPERADMIN`, `TELLER`, and `USER`
- Extract and populate roles from JWT tokens
- Secure endpoints based on user roles
- Provide a user profile endpoint for authenticated users

## Implementation Details

### Step 1: Define Role Constants ✅
**File:** `src/main/java/edu/cit/barcenas/queuems/model/Role.java`

Created a `Role` class containing three constants:
- `SUPERADMIN` - System administrator with full access
- `TELLER` - Counter staff member
- `USER` - Regular user

The class includes a utility method `isValidRole(String role)` to validate role strings.

### Step 2: Implement JWT Authentication Filter ✅
**File:** `src/main/java/edu/cit/barcenas/queuems/config/JwtAuthenticationFilter.java`

Created `JwtAuthenticationFilter` that:
- Extends `OncePerRequestFilter` to run on every request
- Extracts JWT token from the `Authorization` header (Bearer token format)
- Parses the JWT token using HMAC SHA-256
- Extracts user claims: `uid`, `email`, and `role`
- Creates `SimpleGrantedAuthority` from the role (prefixed with `ROLE_`)
- Populates the `SecurityContext` with authentication details
- Includes a helper class `JwtAuthenticationDetails` to store user info

Key features:
- Graceful error handling for invalid tokens
- Filter continues on invalid tokens (token validation is optional)

### Step 3: Update SecurityConfig with RBAC ✅
**File:** `src/main/java/edu/cit/barcenas/queuems/config/SecurityConfig.java`

Updated the `SecurityConfig` class to:
1. **Inject JwtAuthenticationFilter** into the constructor
2. **Configure Authorization Rules:**
   - Public endpoints: `/api/auth/**`, `/login/oauth2/**`, `/oauth2/**` (permitAll)
   - Teller endpoints: `/api/teller/**` (requires TELLER or SUPERADMIN role)
   - Admin endpoints: `/api/admin/**` (requires SUPERADMIN role)
   - User endpoints: `/api/requests/**` (requires authentication)
   - Other endpoints: require authentication
3. **Add JwtAuthenticationFilter** to the filter chain before `UsernamePasswordAuthenticationFilter`

This ensures:
- JWT tokens are processed on every request
- User roles are populated in the security context
- Endpoints enforce proper authorization based on roles

### Step 4: Secure AuthController & Add /auth/me Endpoint ✅
**File:** `src/main/java/edu/cit/barcenas/queuems/controller/AuthController.java`

Added new imports for security annotations:
- `@PreAuthorize` for method-level authorization
- `Authentication` for accessing current user
- `SecurityContextHolder` for retrieving security context

Added new endpoint:
```java
@GetMapping("/me")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<?> getMe()
```

This endpoint:
- Retrieves the authenticated user's profile information
- Extracts the `uid` from the security context
- Fetches user data from Firestore
- Returns user profile information
- Requires authentication (works for any authenticated role)

### Step 5: Extend AuthService ✅
**File:** `src/main/java/edu/cit/barcenas/queuems/service/AuthService.java`

Added new method:
```java
public User getUserById(String uid) throws ExecutionException, InterruptedException
```

This method:
- Queries the Firestore database for user by UID
- Returns user object with all profile information
- Handles Firestore async operations

## JWT Token Structure
The JWT tokens now include the following claims:
```json
{
  "sub": "uid-value",
  "email": "user@example.com",
  "role": "USER|TELLER|SUPERADMIN",
  "iat": 1234567890,
  "exp": 1234654290
}
```

## Role Hierarchy & Authorization

| Endpoint | USER | TELLER | SUPERADMIN |
|----------|------|--------|-----------|
| `/api/auth/**` | ✅ | ✅ | ✅ |
| `/api/auth/me` | ✅ | ✅ | ✅ |
| `/api/requests/**` | ✅ | ✅ | ✅ |
| `/api/teller/**` | ❌ | ✅ | ✅ |
| `/api/admin/**` | ❌ | ❌ | ✅ |

## Testing Recommendations

1. **Test User Registration & Login**
   - Register a new user (default role: USER)
   - Login and verify JWT token contains correct role

2. **Test /auth/me Endpoint**
   - Call `/api/auth/me` with valid JWT token
   - Verify user profile data is returned
   - Test without token (should return 401 Unauthorized)

3. **Test Role-Based Authorization**
   - Create test users with different roles (USER, TELLER, SUPERADMIN)
   - Try accessing `/api/teller/**` with USER role (should return 403 Forbidden)
   - Try accessing `/api/admin/**` with TELLER role (should return 403 Forbidden)
   - Verify SUPERADMIN can access all endpoints

4. **Test JWT Validation**
   - Test with invalid/expired tokens
   - Test with malformed Authorization header
   - Verify requests without Authorization header still work for public endpoints

## Files Modified/Created

### Created Files
- `model/Role.java` - Role constants
- `config/JwtAuthenticationFilter.java` - JWT authentication filter

### Modified Files
- `config/SecurityConfig.java` - Added RBAC configuration and filter integration
- `controller/AuthController.java` - Added `/auth/me` endpoint with authorization
- `service/AuthService.java` - Added `getUserById()` method

## Next Steps
1. Create test endpoints for `/api/teller/**` and `/api/admin/**`
2. Implement feature 2: ServiceRequest & Queue Management
3. Add integration tests for RBAC
4. Frontend integration to use `/auth/me` endpoint

## References
- Plan: `plan/plan.md` - Feature 1
- SDD: `docs/SDD_QueueMS_Barcenas.docx` - Section 3.1 (Development Timeline)
