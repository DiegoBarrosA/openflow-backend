package com.openflow.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertSubscriptionDto {
    private Long id;
    private Long userId;
    private String entityType;
    private Long entityId;
    private Boolean emailEnabled;
    private Boolean inAppEnabled;
    
    // Indicates if the current user is subscribed (used in API responses)
    private Boolean isSubscribed;
}

