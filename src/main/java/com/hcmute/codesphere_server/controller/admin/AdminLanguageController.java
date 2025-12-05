package com.hcmute.codesphere_server.controller.admin;

import com.hcmute.codesphere_server.model.payload.request.CreateLanguageRequest;
import com.hcmute.codesphere_server.model.payload.request.UpdateLanguageRequest;
import com.hcmute.codesphere_server.model.payload.response.DataResponse;
import com.hcmute.codesphere_server.model.payload.response.LanguageResponse;
import com.hcmute.codesphere_server.security.authentication.UserPrinciple;
import com.hcmute.codesphere_server.service.admin.AdminLanguageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${base.url}/admin/languages")
@RequiredArgsConstructor
public class AdminLanguageController {

    private final AdminLanguageService adminLanguageService;

    @PostMapping
    public ResponseEntity<DataResponse<LanguageResponse>> createLanguage(
            @Valid @RequestBody CreateLanguageRequest request,
            Authentication authentication) {
        
        // Kiểm tra authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
        }

        // Kiểm tra quyền admin
        UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
        boolean isAdmin = userPrinciple.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin) {
            return ResponseEntity.status(403)
                    .body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
        }

        try {
            LanguageResponse response = adminLanguageService.createLanguage(request);
            return ResponseEntity.ok(DataResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(DataResponse.error(e.getMessage()));
        }
    }
    @PutMapping("/{id}")
    public ResponseEntity<DataResponse<LanguageResponse>> updateLanguage(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLanguageRequest request,
            Authentication authentication) {
                // auth check (same pattern as create)
                        if (authentication == null || !authentication.isAuthenticated()) {
                        return ResponseEntity.status(401).body(DataResponse.error("Unauthorized - Token không hợp lệ hoặc thiếu"));
                    }
                UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
                boolean isAdmin = userPrinciple.getAuthorities().stream()
                                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
                if (!isAdmin) {
                        return ResponseEntity.status(403).body(DataResponse.error("Forbidden - Chỉ admin mới có quyền thực hiện thao tác này"));
                    }

                        try {
                        LanguageResponse response = adminLanguageService.updateLanguage(id, request);
                        DataResponse<LanguageResponse> res = DataResponse.success(response);
                        res.setMessage("Cập nhật language thành công");
                        return ResponseEntity.ok(res);
                    } catch (RuntimeException e) {
                        return ResponseEntity.badRequest().body(DataResponse.error(e.getMessage()));
                    } catch (Exception e) {
                        return ResponseEntity.status(500).body(DataResponse.error("Lỗi server: " + e.getMessage()));
                    }
    }
}

