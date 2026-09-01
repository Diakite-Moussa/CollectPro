package com.collectpro.backend.repository.projection;

public interface AgentRejectionProjection {
    Long getAgentId();
    String getFirstName();
    String getLastName();
    Long getTotalCount();
    Long getRejectedCount();
}