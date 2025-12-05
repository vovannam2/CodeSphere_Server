package com.hcmute.codesphere_server.service.admin;

import com.hcmute.codesphere_server.model.entity.LanguageEntity;
import com.hcmute.codesphere_server.model.payload.request.CreateLanguageRequest;
import com.hcmute.codesphere_server.model.payload.request.UpdateLanguageRequest;
import com.hcmute.codesphere_server.model.payload.response.LanguageResponse;
import com.hcmute.codesphere_server.repository.common.LanguageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminLanguageService {

    private final LanguageRepository languageRepository;

    @Transactional
    public LanguageResponse createLanguage(CreateLanguageRequest request) {
        // Kiểm tra code đã tồn tại chưa
        if (languageRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Ngôn ngữ với code '" + request.getCode() + "' đã tồn tại");
        }

        // Tạo language mới
        LanguageEntity language = LanguageEntity.builder()
                .code(request.getCode().toLowerCase()) // Chuyển về lowercase
                .name(request.getName())
                .version(request.getVersion())
                .build();

        language = languageRepository.save(language);

        // Map sang response
        return LanguageResponse.builder()
                .id(language.getId())
                .code(language.getCode())
                .name(language.getName())
                .version(language.getVersion())
                .build();
    }
    @Transactional
    public LanguageResponse updateLanguage(Long id, UpdateLanguageRequest request) {
                LanguageEntity language = languageRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy language với id: " + id));

                        if (request.getName() != null) language.setName(request.getName());
                if (request.getVersion() != null) language.setVersion(request.getVersion());

                        language = languageRepository.save(language);

                        return LanguageResponse.builder()
                                .id(language.getId())
                                .code(language.getCode())
                                .name(language.getName())
                                .version(language.getVersion())
                                .build();
    }
}

