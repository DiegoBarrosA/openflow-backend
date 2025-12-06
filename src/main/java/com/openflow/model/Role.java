package com.openflow.model;

/**
 * User roles for role-based access control (RBAC).
 * Maps to Azure AD groups and App Roles:
 * - ADMIN: Openflow-Admins group or Admin app role
 * - USER: Openflow-Users group or User app role
 */
public enum Role {
    ADMIN,      // Full access - create/manage boards, columns, users
    USER        // Create/move/modify tasks, comment
}

