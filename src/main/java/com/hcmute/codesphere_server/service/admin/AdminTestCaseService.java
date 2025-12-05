package com.hcmute.codesphere_server.service.admin;

import com.hcmute.codesphere_server.exception.ResourceConflictException;
import com.hcmute.codesphere_server.model.entity.ProblemEntity;
import com.hcmute.codesphere_server.model.entity.TestCaseEntity;
import com.hcmute.codesphere_server.model.payload.request.CreateTestCaseRequest;
import com.hcmute.codesphere_server.model.payload.request.UpdateTestCaseRequest;
import com.hcmute.codesphere_server.model.payload.response.TestCaseResponse;
import com.hcmute.codesphere_server.repository.common.ProblemRepository;
import com.hcmute.codesphere_server.repository.common.SubmissionTestcaseRepository;
import com.hcmute.codesphere_server.repository.common.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminTestCaseService {

    private final TestCaseRepository testCaseRepository;
    private final ProblemRepository problemRepository;
    private final SubmissionTestcaseRepository submissionTestcaseRepository;

    @Transactional
    public TestCaseResponse createTestCase(CreateTestCaseRequest request) {
        // Kiểm tra problem tồn tại
        ProblemEntity problem = problemRepository.findById(request.getProblemId())
                .orElseThrow(() -> new RuntimeException("Problem không tồn tại"));

        // Tạo test case mới
        TestCaseEntity testCase = TestCaseEntity.builder()
                .problem(problem)
                .input(request.getInput())
                .expectedOutput(request.getExpectedOutput())
                .isSample(request.getIsSample() != null ? request.getIsSample() : false)
                .isHidden(request.getIsHidden() != null ? request.getIsHidden() : false)
                .weight(request.getWeight() != null ? request.getWeight() : 1)
                .isDeleted(false)
                .build();

        testCase = testCaseRepository.save(testCase);

        // Map sang response
        return TestCaseResponse.builder()
                .id(testCase.getId())
                .input(testCase.getInput())
                .expectedOutput(testCase.getExpectedOutput())
                .isSample(testCase.getIsSample())
                .isHidden(testCase.getIsHidden())
                .weight(testCase.getWeight())
                .build();
    }
    @Transactional(readOnly = true)
    public List<TestCaseResponse> getTestCasesByProblem(Long problemId) {
        if (!problemRepository.existsById(problemId)) {
            throw new ResourceNotFoundException("Problem không tồn tại");
        }
        return testCaseRepository.findAllTestCasesByProblemId(problemId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TestCaseResponse updateTestCase(Long id, UpdateTestCaseRequest req) {
        TestCaseEntity tc = testCaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Testcase không tồn tại"));
        if (req.getInput() != null) tc.setInput(req.getInput());
        if (req.getExpectedOutput() != null) tc.setExpectedOutput(req.getExpectedOutput());
        if (req.getIsSample() != null) tc.setIsSample(req.getIsSample());
        if (req.getIsHidden() != null) tc.setIsHidden(req.getIsHidden());
        if (req.getWeight() != null) tc.setWeight(req.getWeight());
        TestCaseEntity saved = testCaseRepository.save(tc);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteTestCase(Long id) {
        TestCaseEntity tc = testCaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Testcase không tồn tại"));
        // Nếu đã có kết quả submission liên quan, chặn xóa hoặc đánh dấu isDeleted = true
        long used = submissionTestcaseRepository.countByTestCase_Id(id);
        if (used > 0) {
            throw new ResourceConflictException("Không thể xóa testcase đã có kết quả chạy/submission");
        }
        testCaseRepository.delete(tc);
    }

    private TestCaseResponse mapToResponse(TestCaseEntity t) {
        return TestCaseResponse.builder()
                .id(t.getId())
                .input(t.getInput())
                .expectedOutput(t.getExpectedOutput())
                .isSample(t.getIsSample())
                .isHidden(t.getIsHidden())
                .weight(t.getWeight())
                .build();
    }
}

