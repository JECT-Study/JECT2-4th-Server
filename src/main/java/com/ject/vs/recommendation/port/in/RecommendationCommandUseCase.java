package com.ject.vs.recommendation.port.in;

import java.util.List;

public interface RecommendationCommandUseCase {

    /**
     * 오늘의 추천 투표를 설정한다.
     * @param adminUserId 호출한 사용자 ID (admin 체크용)
     * @param voteIds 추천할 투표 ID 목록 (순서대로 displayOrder 부여)
     */
    void setTodayRecommendations(Long adminUserId, List<Long> voteIds);
}
