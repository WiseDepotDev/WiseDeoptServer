package com.huicang.wise.application.session;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionDTO {
    private Long sessionId;
    private String sessionIdStr;
    private String deviceName;
    private String deviceType;
    private String ipAddress;
    private String location;
    private Boolean isCurrent;
    private Boolean isActive;
    private LocalDateTime lastActivityAt;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
