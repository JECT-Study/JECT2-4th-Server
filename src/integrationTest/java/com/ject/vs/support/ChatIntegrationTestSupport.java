package com.ject.vs.support;

import com.ject.vs.image.port.ImageService;
import com.ject.vs.user.domain.ImageColor;
import com.ject.vs.user.domain.User;
import com.ject.vs.user.domain.UserRepository;
import com.ject.vs.vote.domain.Vote;
import com.ject.vs.vote.domain.VoteOption;
import com.ject.vs.vote.domain.VoteOptionRepository;
import com.ject.vs.vote.domain.VoteParticipation;
import com.ject.vs.vote.domain.VoteParticipationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Chat API Integration Test에서 공통으로 사용하는 테스트 데이터 생성 및 인증 지원.
 */
public abstract class ChatIntegrationTestSupport extends VoteIntegrationTestSupport {

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected VoteOptionRepository voteOptionRepository;

    @Autowired
    protected VoteParticipationRepository voteParticipationRepository;

    @MockitoBean
    protected ImageService imageService;

    protected void authenticateAs(Long userId) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, AuthorityUtils.NO_AUTHORITIES);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    protected void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    protected RequestPostProcessor asUser(Long userId) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(userId, null, AuthorityUtils.NO_AUTHORITIES));
    }

    protected User createUserWithNickname(String nickname) {
        User user = User.createWithSub("sub-" + System.nanoTime());
        User.modifyAccount(user, nickname, ImageColor.GREEN);
        return userRepository.save(user);
    }

    protected ChatRoomFixture createChatRoomFixture(String title) {
        Vote vote = createOngoingVote(title);
        VoteOption optionA = voteOptionRepository.save(VoteOption.of(vote, "옵션 A", 0));
        VoteOption optionB = voteOptionRepository.save(VoteOption.of(vote, "옵션 B", 1));

        User userA = createUserWithNickname("참여자A");
        User userB = createUserWithNickname("참여자B");

        voteParticipationRepository.save(
                VoteParticipation.ofMember(vote.getId(), userA.getId(), optionA.getId()));
        voteParticipationRepository.save(
                VoteParticipation.ofMember(vote.getId(), userB.getId(), optionB.getId()));

        entityManager.flush();
        entityManager.clear();

        User reloadedUserA = userRepository.findById(userA.getId()).orElseThrow();
        User reloadedUserB = userRepository.findById(userB.getId()).orElseThrow();

        return new ChatRoomFixture(vote.getId(), reloadedUserA, reloadedUserB, optionA, optionB);
    }

    protected record ChatRoomFixture(
            Long voteId,
            User userA,
            User userB,
            VoteOption optionA,
            VoteOption optionB
    ) {}
}