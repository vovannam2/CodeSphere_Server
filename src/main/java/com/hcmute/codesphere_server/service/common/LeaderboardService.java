package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.SubmissionEntity;
import com.hcmute.codesphere_server.model.entity.UserProblemBestEntity;
import com.hcmute.codesphere_server.model.payload.response.LeaderboardResponse;
import com.hcmute.codesphere_server.repository.common.ProblemRepository;
import com.hcmute.codesphere_server.repository.common.UserProblemBestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final UserProblemBestRepository userProblemBestRepository;
    private final ProblemRepository problemRepository;

    @Transactional(readOnly = true)
    public List<LeaderboardResponse> getLeaderboard(Long problemId) {
        // Kiểm tra problem tồn tại
        problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));

        // Lấy tất cả best submissions từ bảng user_problem_best (đã được tính toán sẵn)
        // Đã được sắp xếp theo điểm DESC, thời gian ASC
        List<UserProblemBestEntity> bestSubmissions = userProblemBestRepository
                .findAllByProblemIdOrderByBestScoreDesc(problemId);

        if (bestSubmissions.isEmpty()) {
            return new ArrayList<>();
        }

        // Map sang LeaderboardResponse và gán rank
        List<LeaderboardResponse> leaderboard = new ArrayList<>();
        for (int i = 0; i < bestSubmissions.size(); i++) {
            UserProblemBestEntity best = bestSubmissions.get(i);
            SubmissionEntity bestSubmission = best.getBestSubmission();
            
            LeaderboardResponse response = LeaderboardResponse.builder()
                    .rank(i + 1)
                    .userId(best.getUser().getId())
                    .username(best.getUser().getUsername())
                    .bestScore(best.getBestScore())
                    .bestSubmissionId(bestSubmission.getId())
                    .totalSubmissions(best.getTotalSubmissions())
                    .bestSubmissionTime(bestSubmission.getCreatedAt())
                    .statusMsg(bestSubmission.getStatusMsg())
                    .statusRuntime(bestSubmission.getStatusRuntime())
                    .statusMemory(bestSubmission.getStatusMemory())
                    .isAccepted(bestSubmission.getIsAccepted())
                    .build();
            
            leaderboard.add(response);
        }

        return leaderboard;
    }

    @Transactional(readOnly = true)
    public LeaderboardResponse getMyRank(Long problemId, Long userId) {
        // Kiểm tra problem tồn tại
        problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));

        // Tìm best submission của user cho problem này
        return userProblemBestRepository.findByUserIdAndProblemId(userId, problemId)
                .map(best -> {
                    // Tìm rank của user trong leaderboard
                    List<LeaderboardResponse> leaderboard = getLeaderboard(problemId);
                    return leaderboard.stream()
                            .filter(entry -> entry.getUserId().equals(userId))
                            .findFirst()
                            .orElse(null);
                })
                .orElse(null); // null nếu user chưa nộp bài nào
    }
}

