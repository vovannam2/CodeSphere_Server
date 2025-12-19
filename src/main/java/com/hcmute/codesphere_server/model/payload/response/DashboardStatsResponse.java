package com.hcmute.codesphere_server.model.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private Long totalUsers;
    private Long totalProblems;
    private Long totalContests;
    private Long totalSubmissions;
    private Long activeUsers; // Last 30 days
    private Long newUsersThisMonth;
    private Long submissionsToday;
}

