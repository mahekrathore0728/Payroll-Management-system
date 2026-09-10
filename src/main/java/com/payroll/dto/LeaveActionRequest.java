package com.payroll.dto;

import jakarta.validation.constraints.NotBlank;

public class LeaveActionRequest {
    @NotBlank(message = "Status is required (APPROVED or REJECTED)")
    private String status;

    private String managerRemarks;

    public LeaveActionRequest() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getManagerRemarks() { return managerRemarks; }
    public void setManagerRemarks(String managerRemarks) { this.managerRemarks = managerRemarks; }
}
