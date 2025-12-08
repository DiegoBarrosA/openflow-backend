package com.openflow.model;

/**
 * Access levels for board sharing.
 * - READ: View-only access to board and tasks
 * - WRITE: Can create and modify tasks
 * - ADMIN: Can manage columns, access, and board settings
 */
public enum AccessLevel {
    READ,   // View-only access
    WRITE,  // Create and modify tasks
    ADMIN   // Full management access
}

