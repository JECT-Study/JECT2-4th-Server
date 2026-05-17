package com.ject.vs.notification.port;

import com.ject.vs.notification.domain.NotificationSetting;
import com.ject.vs.notification.domain.NotificationSettingRepository;
import com.ject.vs.notification.port.in.NotificationPromptUseCase;
import com.ject.vs.vote.port.in.VoteQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationPromptService implements NotificationPromptUseCase {

    private final NotificationSettingRepository settingRepository;
    private final VoteQueryUseCase voteQueryUseCase;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public PromptStatusResult getStatus(Long userId) {
        NotificationSetting s = settingRepository.findById(userId)
                .orElseGet(() -> NotificationSetting.createDefault(userId));

        if (s.isPushEnabled()) {
            return new PromptStatusResult(false, 0);
        }
        long count = voteQueryUseCase.countParticipationByUserId(userId);
        boolean shouldShow = (count == 1) || (count > 0 && (count - 1) % 10 == 0);
        return new PromptStatusResult(shouldShow, count);
    }

    @Override
    public void recordDismissed(Long userId) {
        NotificationSetting s = settingRepository.findById(userId)
                .orElseGet(() -> NotificationSetting.createDefault(userId));
        s.recordPromptDismissed(clock);
        settingRepository.save(s);
    }
}
