package com.ject.vs.vote.adapter.web;

import com.ject.vs.image.port.ImageService;
import com.ject.vs.user.domain.User;
import com.ject.vs.user.domain.UserRepository;
import com.ject.vs.vote.domain.Vote;
import com.ject.vs.vote.domain.VoteOption;
import com.ject.vs.vote.domain.VoteOptionRepository;
import com.ject.vs.vote.domain.VoteRepository;
import com.ject.vs.vote.domain.VoteStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Vote API 통합 테스트")
class VoteApiIntegrationTest {

    @MockitoBean
    private ImageService imageService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private VoteOptionRepository voteOptionRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testUser = userRepository.save(User.createWithSub("test-sub-" + System.currentTimeMillis()));
    }

    private void authenticateAs(Long userId) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, AuthorityUtils.NO_AUTHORITIES);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("POST /api/votes - 투표 생성")
    class CreateVote {

        @Test
        @DisplayName("인증된 사용자가 투표 생성 시 DB에 저장된다")
        void 인증된_사용자가_투표_생성시_DB에_저장된다() throws Exception {
            // given
            authenticateAs(testUser.getId());

            String requestBody = """
                {
                    "title": "테스트 투표 제목",
                    "content": "테스트 투표 내용",
                    "thumbnailUrl": "https://example.com/thumb.png",
                    "duration": "HOURS_12",
                    "optionA": "선택지 A",
                    "optionB": "선택지 B"
                }
                """;

            // when
            MvcResult result = mockMvc.perform(post("/api/votes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.voteId").isNumber())
                    .andExpect(jsonPath("$.status").value("ONGOING"))
                    .andExpect(jsonPath("$.endAt").isNotEmpty())
                    .andReturn();

            // then - DB에 저장 확인
            List<Vote> votes = voteRepository.findAll();
            assertThat(votes).hasSize(1);

            Vote savedVote = votes.get(0);
            assertThat(savedVote.getTitle()).isEqualTo("테스트 투표 제목");
            assertThat(savedVote.getContent()).isEqualTo("테스트 투표 내용");
            assertThat(savedVote.getThumbnailUrl()).isEqualTo("https://example.com/thumb.png");
            assertThat(savedVote.getEndAt()).isNotNull();

            // VoteOption도 저장 확인
            List<VoteOption> options = voteOptionRepository.findByVoteIdOrderByPosition(savedVote.getId());
            assertThat(options).hasSize(2);
            assertThat(options.get(0).getLabel()).isEqualTo("선택지 A");
            assertThat(options.get(1).getLabel()).isEqualTo("선택지 B");
        }

        @Test
        @DisplayName("몰입형 투표 생성 시 imageUrl도 함께 저장된다")
        void 몰입형_투표_생성시_imageUrl도_저장된다() throws Exception {
            // given
            authenticateAs(testUser.getId());

            String requestBody = """
                {
                    "title": "몰입형 투표",
                    "content": "몰입형 내용",
                    "thumbnailUrl": "https://example.com/thumb.png",
                    "imageUrl": "https://example.com/image.png",
                    "duration": "HOURS_24",
                    "optionA": "A",
                    "optionB": "B"
                }
                """;

            // when
            mockMvc.perform(post("/api/votes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated());

            // then
            Vote savedVote = voteRepository.findAll().get(0);
            assertThat(savedVote.getImageUrl()).isEqualTo("https://example.com/image.png");
        }

        @Test
        @DisplayName("인증되지 않은 사용자는 4xx 에러")
        void 인증되지_않은_사용자는_4xx() throws Exception {
            // given
            clearAuthentication();

            String requestBody = """
                {
                    "title": "테스트",
                    "thumbnailUrl": "https://example.com/thumb.png",
                    "duration": "HOURS_12",
                    "optionA": "A",
                    "optionB": "B"
                }
                """;

            // when & then
            mockMvc.perform(post("/api/votes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().is4xxClientError());

            // DB에 저장되지 않음
            assertThat(voteRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("다양한 duration으로 투표 생성 시 endAt이 정확히 계산된다")
        void 다양한_duration으로_투표_생성시_endAt_계산_확인() throws Exception {
            // given
            authenticateAs(testUser.getId());

            String[] durations = {"HOURS_12", "HOURS_24"};

            for (String duration : durations) {
                String requestBody = String.format("""
                    {
                        "title": "투표 %s",
                        "thumbnailUrl": "https://example.com/thumb.png",
                        "duration": "%s",
                        "optionA": "A",
                        "optionB": "B"
                    }
                    """, duration, duration);

                // when
                mockMvc.perform(post("/api/votes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.endAt").isNotEmpty());
            }

            // then
            List<Vote> votes = voteRepository.findAll();
            assertThat(votes).hasSize(2);

            // 모든 투표의 endAt이 설정되어 있는지 확인
            for (Vote vote : votes) {
                assertThat(vote.getEndAt()).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("GET /api/votes/{voteId} - 투표 상세 조회")
    class GetVoteDetail {

        @Test
        @DisplayName("투표 상세 조회 시 endAt이 응답에 포함된다")
        void 투표_상세_조회시_endAt_포함() throws Exception {
            // given - 투표 생성
            authenticateAs(testUser.getId());

            String createRequest = """
                {
                    "title": "상세 조회 테스트",
                    "content": "내용",
                    "thumbnailUrl": "https://example.com/thumb.png",
                    "duration": "HOURS_12",
                    "optionA": "A",
                    "optionB": "B"
                }
                """;

            MvcResult createResult = mockMvc.perform(post("/api/votes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createRequest))
                    .andExpect(status().isCreated())
                    .andReturn();

            // voteId 추출
            String responseBody = createResult.getResponse().getContentAsString();
            Long voteId = voteRepository.findAll().get(0).getId();

            // when & then - 상세 조회
            mockMvc.perform(get("/api/votes/{voteId}", voteId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.voteId").value(voteId))
                    .andExpect(jsonPath("$.title").value("상세 조회 테스트"))
                    .andExpect(jsonPath("$.endAt").isNotEmpty())
                    .andExpect(jsonPath("$.status").value("ONGOING"));
        }
    }

    @Nested
    @DisplayName("전체 플로우 테스트")
    class FullFlowTest {

        @Test
        @DisplayName("투표 생성 → 조회 → DB 저장 전체 플로우")
        void 투표_생성_조회_DB_저장_전체_플로우() throws Exception {
            // 1. 투표 생성
            authenticateAs(testUser.getId());

            String createRequest = """
                {
                    "title": "전체 플로우 테스트",
                    "content": "테스트 내용입니다",
                    "thumbnailUrl": "https://example.com/thumb.png",
                    "duration": "HOURS_24",
                    "optionA": "찬성",
                    "optionB": "반대"
                }
                """;

            mockMvc.perform(post("/api/votes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createRequest))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.voteId").isNumber())
                    .andExpect(jsonPath("$.status").value("ONGOING"))
                    .andExpect(jsonPath("$.endAt").isNotEmpty());

            // 2. DB 저장 확인
            List<Vote> votes = voteRepository.findAll();
            assertThat(votes).hasSize(1);

            Vote savedVote = votes.get(0);
            assertThat(savedVote.getTitle()).isEqualTo("전체 플로우 테스트");
            assertThat(savedVote.getContent()).isEqualTo("테스트 내용입니다");
            assertThat(savedVote.getEndAt()).isNotNull();
            assertThat(savedVote.getStatus()).isEqualTo(VoteStatus.ONGOING);

            // 3. VoteOption 저장 확인
            List<VoteOption> options = voteOptionRepository.findByVoteIdOrderByPosition(savedVote.getId());
            assertThat(options).hasSize(2);
            assertThat(options.get(0).getLabel()).isEqualTo("찬성");
            assertThat(options.get(1).getLabel()).isEqualTo("반대");

            // 4. 투표 상세 조회
            mockMvc.perform(get("/api/votes/{voteId}", savedVote.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("전체 플로우 테스트"))
                    .andExpect(jsonPath("$.endAt").isNotEmpty())
                    .andExpect(jsonPath("$.options[0].label").value("찬성"))
                    .andExpect(jsonPath("$.options[1].label").value("반대"));
        }
    }
}
