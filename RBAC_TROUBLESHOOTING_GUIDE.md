# RBAC Troubleshooting Guide
## Avoiding "You don't have permission to access this resource" Errors

## Problem Description
The error message "You don't have permission to access this resource. Please check your entitlements" typically occurs when:
1. User's role is not properly set during registration/login
2. JWT token doesn't include the role claim
3. JwtAuthenticationFilter is not properly extracting/populating the role
4. SecurityConfig authorization rules are not correctly configured
5. The role in the token doesn't match the endpoint's required role

---

## Common Issues & Solutions

### Issue 1: Role Not Set During User Registration
**Problem:** New users are registered without a proper role or with `null` role.

**Solution:**
```java
// In AuthService.register()
User user = new User(uid, dto.getEmail(), hashedPassword, 
                     dto.getFirstname(), dto.getLastname(), 
                     "USER");  // ✅ Explicitly set default role to USER
userRepository.save(user);
```

**Verification:**
- Check Firestore console
- Ensure each user document has a `role` field set to "USER", "TELLER", or "SUPERADMIN"

---

### Issue 2: JWT Token Missing Role Claim
**Problem:** The JWT token is generated without the `role` claim.

**Solution - Verify JwtService.generateToken():**
```java
public String generateToken(String uid, String email, String role) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + expirationMs);

    Claims claims = Jwts.claims().setSubject(uid);
    claims.put("email", email);
    claims.put("role", role);  // ✅ Role MUST be included

    return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
}
```

**Testing:**
1. Login and get the JWT token
2. Decode it at https://jwt.io/
3. Verify the payload includes:
   ```json
   {
     "sub": "user-uid",
     "email": "user@example.com",
     "role": "USER"
   }
   ```

---

### Issue 3: JwtAuthenticationFilter Not Extracting Role
**Problem:** Filter extracts token but fails to extract the `role` claim.

**Solution - Verify Filter Logic:**
```java
// In JwtAuthenticationFilter.doFilterInternal()
try {
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        String uid = claims.getSubject();
        String email = claims.get("email", String.class);
        String role = claims.get("role", String.class);  // ✅ Extract role

        if (role == null || role.isEmpty()) {
            logger.warn("Role is null or empty for user: " + uid);
            // Default to USER if role is missing
            role = "USER";
        }

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
        // Continue with authentication setup...
    }
} catch (Exception e) {
    logger.debug("Invalid JWT token: " + e.getMessage());
}
```

**Debugging:**
Add logging to verify role extraction:
```java
logger.info("Extracted role from JWT: " + role);
logger.info("Created authority: ROLE_" + role);
```

---

### Issue 4: SecurityConfig Missing Role-Based Rules
**Problem:** SecurityConfig doesn't properly configure authorization rules.

**Solution - Complete SecurityConfig:**
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            // Public endpoints - no authentication required
            .requestMatchers("/api/auth/**", "/login/oauth2/**", "/oauth2/**")
                .permitAll()
            
            // Teller endpoints - TELLER or SUPERADMIN only
            .requestMatchers("/api/teller/**")
                .hasAnyRole(Role.TELLER, Role.SUPERADMIN)
            
            // Admin endpoints - SUPERADMIN only
            .requestMatchers("/api/admin/**")
                .hasRole(Role.SUPERADMIN)
            
            // User endpoints - any authenticated user
            .requestMatchers("/api/requests/**")
                .authenticated()
            
            // All other endpoints require authentication
            .anyRequest()
                .authenticated())
        .oauth2Login(oauth2 -> oauth2
            .successHandler(oAuth2SuccessHandler));

    // ✅ Add JWT filter BEFORE UsernamePasswordAuthenticationFilter
    http.addFilterBefore(jwtAuthenticationFilter, 
                        UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
```

---

### Issue 5: Role String Mismatch in Authorization
**Problem:** Role values don't match between token and configuration.

**Solution - Use Consistent Role Values:**

In `Role.java`:
```java
public static final String SUPERADMIN = "SUPERADMIN";
public static final String TELLER = "TELLER";
public static final String USER = "USER";
```

In `SecurityConfig.java`:
```java
.requestMatchers("/api/teller/**")
    .hasAnyRole(Role.TELLER, Role.SUPERADMIN)  // ✅ Use Role class constants
```

In `AuthService.java`:
```java
User user = new User(..., "USER");  // ✅ Must exactly match Role.USER
```

---

## Step-by-Step Verification Process

### Step 1: Verify User in Firestore
```
Collection: users
Document: {user-uid}
Fields:
  - uid: "user-uid"
  - email: "user@example.com"
  - firstname: "John"
  - lastname: "Doe"
  - role: "USER"  ← This MUST exist and be correct
  - password: "hashed-password"
```

### Step 2: Test Login Endpoint
```bash
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}

Response:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 86400000
}
```

### Step 3: Decode JWT Token
Go to https://jwt.io/ and paste the token. Verify the payload:
```json
{
  "sub": "user-uid",
  "email": "user@example.com",
  "role": "USER",
  "iat": 1234567890,
  "exp": 1234654290
}
```

### Step 4: Test Protected Endpoint
```bash
GET /api/auth/me
Authorization: Bearer {token}

Response (200 OK):
{
  "uid": "user-uid",
  "email": "user@example.com",
  "firstname": "John",
  "lastname": "Doe",
  "role": "USER"
}
```

### Step 5: Test Role-Based Access
```bash
# Test 1: USER trying to access TELLER endpoint
GET /api/teller/serve/request-123
Authorization: Bearer {user-token}
Response: 403 Forbidden ✅

# Test 2: TELLER accessing TELLER endpoint
GET /api/teller/serve/request-123
Authorization: Bearer {teller-token}
Response: 200 OK ✅

# Test 3: SUPERADMIN accessing ADMIN endpoint
GET /api/admin/users
Authorization: Bearer {superadmin-token}
Response: 200 OK ✅

# Test 4: TELLER trying to access ADMIN endpoint
GET /api/admin/users
Authorization: Bearer {teller-token}
Response: 403 Forbidden ✅
```

---

## Advanced Debugging

### Enable Spring Security Debug Logging
Add to `application.properties`:
```properties
logging.level.org.springframework.security=DEBUG
logging.level.edu.cit.barcenas.queuems.config=DEBUG
```

### Add Custom Logging to JwtAuthenticationFilter
```java
@Override
protected void doFilterInternal(HttpServletRequest request, 
                               HttpServletResponse response, 
                               FilterChain filterChain)
        throws ServletException, IOException {
    try {
        String authHeader = request.getHeader("Authorization");
        logger.info("Processing request: " + request.getRequestURI());
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            logger.info("Found Bearer token");
            
            Claims claims = parseToken(token);
            String uid = claims.getSubject();
            String role = claims.get("role", String.class);
            
            logger.info("Extracted UID: " + uid);
            logger.info("Extracted Role: " + role);
            
            // Continue...
        } else {
            logger.info("No Bearer token found");
        }
    } catch (Exception e) {
        logger.error("JWT processing failed: " + e.getMessage(), e);
    }
    
    filterChain.doFilter(request, response);
}
```

### Monitor in Browser Console
When testing from frontend:
```javascript
const response = await fetch('/api/auth/me', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

if (!response.ok) {
  console.error('Status:', response.status);
  console.error('Text:', await response.text());
}
```

---

## Common Error Messages & Solutions

| Error Message | Cause | Solution |
|---------------|-------|----------|
| 401 Unauthorized | No token or invalid token | Check Authorization header format: `Bearer {token}` |
| 403 Forbidden | Valid token but insufficient role | Verify user's role in Firestore and JWT token |
| 500 Internal Server Error | JwtAuthenticationFilter exception | Check logs, verify JWT secret matches |
| null role in token | Role not included in JWT generation | Ensure JwtService.generateToken() includes role claim |
| ROLE_ prefix issue | Mismatch in role naming | Use Role class constants everywhere |

---

## Prevention Checklist

- [ ] All users have a `role` field in Firestore (USER, TELLER, or SUPERADMIN)
- [ ] JwtService.generateToken() includes the `role` claim
- [ ] JwtAuthenticationFilter properly extracts the role from JWT
- [ ] SecurityConfig uses hasRole() and hasAnyRole() correctly
- [ ] All role values use Role class constants
- [ ] JWT filter is added to the filter chain
- [ ] Authorization headers are sent as `Bearer {token}`
- [ ] Token is not expired
- [ ] Endpoints match the SecurityConfig path matchers

---

## Testing Code Examples

### Java Test Example
```java
@SpringBootTest
@AutoConfigureMockMvc
public class RbacTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void testUserCanAccessOwnProfile() throws Exception {
        String token = getValidUserToken();
        
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));
    }
    
    @Test
    public void testUserCannotAccessAdminEndpoint() throws Exception {
        String token = getValidUserToken();
        
        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
```

---

## Frontend Integration Example

```typescript
// Fetch user profile on app load
async function loadUserProfile(token: string) {
    try {
        const response = await fetch('/api/auth/me', {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });
        
        if (response.status === 401) {
            console.error('Token expired or invalid');
            // Redirect to login
            return;
        }
        
        if (response.status === 403) {
            console.error('User does not have permission');
            return;
        }
        
        const user = await response.json();
        console.log('User role:', user.role);
        
        // Store in context/state
        return user;
    } catch (error) {
        console.error('Failed to load user profile:', error);
    }
}
```

---

## Summary

To avoid permission errors:
1. **Ensure every user has a role** in Firestore
2. **Include role in JWT tokens** when generating them
3. **Extract and populate role** in SecurityContext via JwtAuthenticationFilter
4. **Configure authorization rules** properly in SecurityConfig
5. **Test thoroughly** using the verification process above
6. **Use consistent role values** throughout the codebase

For any remaining issues, check the debug logs and verify each step of the RBAC flow.
