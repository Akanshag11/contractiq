package com.contractiq.controller;

import com.contractiq.dto.response.ApprovalStepResponse;
import com.contractiq.service.ApprovalQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApprovalController {
    private final ApprovalQueryService approvalQueryService;

    @GetMapping("/contracts/{id}/approval-steps")
    public List<ApprovalStepResponse> getApprovalStepsForContract(@PathVariable UUID id, Authentication authentication) {
        return approvalQueryService.getApprovalStepsForContract(id, authentication);
    }

    @GetMapping("/approval/my-pending")
    public List<ApprovalStepResponse> getMyPendingApprovals(Authentication authentication) {
        return approvalQueryService.getMyPendingApprovals(authentication);
    }
}
