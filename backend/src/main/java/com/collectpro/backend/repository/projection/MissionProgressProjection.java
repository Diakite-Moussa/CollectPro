package com.collectpro.backend.repository.projection;

public interface MissionProgressProjection {
    Long getMissionId();
    Long getReceivedCount();
    Long getActiveAgentsCount();
}