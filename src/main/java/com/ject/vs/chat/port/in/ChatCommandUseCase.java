package com.ject.vs.chat.port.in;

public interface ChatCommandUseCase {
    MessageResult sendMessage(SendMessageCommand command);
    void markAsRead(MarkAsReadCommand command);
}
