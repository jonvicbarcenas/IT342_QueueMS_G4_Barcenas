package edu.cit.barcenas.queuems.model;

/**
 * Role constants for Role-Based Access Control (RBAC)
 * Defines the available user roles in the Queue Management System
 */
public class Role {
    public static final String SUPERADMIN = "SUPERADMIN";
    public static final String TELLER = "TELLER";
    public static final String USER = "USER";

    // Private constructor to prevent instantiation
    private Role() {
        throw new AssertionError("Cannot instantiate Role class");
    }

    /**
     * Validates if the given role is a valid role
     * @param role the role to validate
     * @return true if the role is valid, false otherwise
     */
    public static boolean isValidRole(String role) {
        return SUPERADMIN.equals(role) || TELLER.equals(role) || USER.equals(role);
    }
}
