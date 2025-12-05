package com.hcmute.codesphere_server.controller.admin;

import com.hcmute.codesphere_server.exception.ResourceConflictException;
import com.hcmute.codesphere_server.exception.ResourceNotFoundException;
import com.hcmute.codesphere_server.model.payload.request.CreateTestCaseRequest;
import com.hcmute.codesphere_server.model.payload.request.UpdateTestCaseRequest;
import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.model.payload.response.TestCaseResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.admin.AdminTestCaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${base.url}/admin/testcases")
@RequiredArgsConstructor
public class AdminTestCaseController {

    private final AdminTestCaseService adminTestCaseService;

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
        return userPrinciple.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @PostMapping
    public ResponseEntity<DataResponse<TestCaseResponse>> createTestCase(
            @Valid @RequestBody CreateTestCaseRequest request,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).body(DataResponse.error("Forbidden - Chỉ admin mới có quyền"));
        }

        try {
            TestCaseResponse response = adminTestCaseService.createTestCase(request);
            return ResponseEntity.ok(DataResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(DataResponse.error(e.getMessage()));
        }
    }

    // GET list by problemId
    @GetMapping
    public ResponseEntity<DataResponse<List<TestCaseResponse>>> getTestCases(
            @RequestParam Long problemId,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).body(DataResponse.error("Forbidden - Chỉ admin mới có quyền"));
        }

        try {
            List<TestCaseResponse> list = adminTestCaseService.getTestCasesByProblem(problemId);
            return ResponseEntity.ok(DataResponse.success(list));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(DataResponse.error("Lỗi server"));
        }
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<DataResponse<TestCaseResponse>> updateTestCase(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTestCaseRequest request,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).body(DataResponse.error("Forbidden - Chỉ admin mới có quyền"));
        }

        try {
            TestCaseResponse updated = adminTestCaseService.updateTestCase(id, request);
            return ResponseEntity.ok(DataResponse.success(updated));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(DataResponse.error(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(DataResponse.error("Lỗi server"));
        }
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<DataResponse<String>> deleteTestCase(
            @PathVariable Long id,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).body(DataResponse.error("Forbidden - Chỉ admin mới có quyền"));
        }

        try {
            adminTestCaseService.deleteTestCase(id);
            return ResponseEntity.ok(DataResponse.success("Deleted"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(DataResponse.error(e.getMessage()));
        } catch (ResourceConflictException e) {
            return ResponseEntity.status(409).body(DataResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(DataResponse.error("Lỗi server"));
        }
    }
}