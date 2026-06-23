package com.ject.vs.user.port.in;

import com.ject.vs.user.adapter.web.dto.UserProfileBottomSheetResponse;

public interface UserProfileQueryUseCase {

    UserProfileBottomSheetResponse getProfileBottomSheet(Long targetUserId, Long viewerUserId);
}