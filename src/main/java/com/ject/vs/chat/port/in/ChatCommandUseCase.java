package com.ject.vs.chat.port.in;

import com.ject.vs.chat.port.in.dto.MarkAsReadCommand;
import com.ject.vs.chat.port.in.dto.MessageResult;
import com.ject.vs.chat.port.in.dto.SendMessageCommand;

public interface ChatCommandUseCase {
    MessageResult sendMessage(SendMessageCommand command);
    void markAsRead(MarkAsReadCommand command);
}
