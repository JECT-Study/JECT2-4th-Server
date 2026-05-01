package com.ject.vs.chat.adapter.web;

import com.ject.vs.chat.adapter.web.dto.ChatListResponse;
import com.ject.vs.chat.adapter.web.dto.ChatRoomResponse;
import com.ject.vs.chat.adapter.web.dto.GaugeResponse;
import com.ject.vs.chat.adapter.web.dto.MarkAsReadRequest;
import com.ject.vs.chat.adapter.web.dto.MessagePageResponse;
import com.ject.vs.chat.adapter.web.dto.MessageResponse;
import com.ject.vs.chat.adapter.web.dto.SendMessageRequest;
import com.ject.vs.vote.port.in.dto.VoteStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Chat", description = "채팅 API")
public interface ChatDocs {

    @Operation(
            summary = "채팅방 목록 조회",
            description = "로그인 사용자가 참여한 채팅방을 투표 진행 상태별로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅방 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = ChatListResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    ChatListResponse getChatList(
            @Parameter(hidden = true) Long userId,
            @Parameter(description = "조회할 투표 상태", required = true, example = "ONGOING", schema = @Schema(allowableValues = {"ONGOING", "ENDED"}))
            VoteStatus status
    );

    @Operation(
            summary = "채팅방 상세 조회",
            description = "채팅방 상단에 표시할 투표 제목, 진행 상태, 선택지, 참여자 수, 종료 시간을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅방 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = ChatRoomResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 투표 또는 채팅방", content = @Content)
    })
    ChatRoomResponse getChatRoom(
            @Parameter(hidden = true) Long userId,
            @Parameter(description = "채팅방이 연결된 투표 ID", required = true, example = "1") Long voteId
    );

    @Operation(
            summary = "투표 게이지 조회",
            description = "채팅방에서 표시할 A/B 선택지 투표 비율과 참여자 수를 조회합니다. 비율 값은 0부터 100까지의 정수입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "투표 게이지 조회 성공",
                    content = @Content(schema = @Schema(implementation = GaugeResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 투표", content = @Content)
    })
    GaugeResponse getGauge(
            @Parameter(description = "게이지를 조회할 투표 ID", required = true, example = "1") Long voteId
    );

    @Operation(
            summary = "채팅 메시지 목록 조회",
            description = "채팅방 메시지를 커서 기반으로 조회합니다. cursor가 없으면 최신 메시지 기준으로 조회하고, nextCursor와 hasNext로 다음 페이지 요청 여부를 판단합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅 메시지 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = MessagePageResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
            @ApiResponse(responseCode = "403", description = "채팅방 접근 권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 투표 또는 채팅방", content = @Content)
    })
    MessagePageResponse getMessages(
            @Parameter(description = "메시지를 조회할 투표 ID", required = true, example = "1") Long voteId,
            @Parameter(hidden = true) Long userId,
            @Parameter(description = "이전 페이지의 nextCursor. 첫 조회 시 생략합니다.", example = "128") Long cursor,
            @Parameter(description = "조회할 메시지 수", example = "30") int size
    );

    @Operation(
            summary = "채팅 메시지 전송",
            description = "채팅방에 새 메시지를 전송하고 저장된 메시지 정보를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "채팅 메시지 전송 성공",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "메시지 내용이 비어 있거나 유효하지 않음", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
            @ApiResponse(responseCode = "403", description = "채팅방 접근 권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 투표 또는 채팅방", content = @Content)
    })
    MessageResponse sendMessage(
            @Parameter(description = "메시지를 전송할 투표 ID", required = true, example = "1") Long voteId,
            @Parameter(hidden = true) Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "전송할 메시지 내용",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SendMessageRequest.class))
            )
            SendMessageRequest request
    );

    @Operation(
            summary = "채팅방 읽음 처리",
            description = "사용자가 마지막으로 읽은 메시지 ID를 저장해 이후 채팅방 목록의 unreadCount 계산에 사용합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "읽음 처리 성공", content = @Content),
            @ApiResponse(responseCode = "400", description = "마지막 읽은 메시지 ID가 유효하지 않음", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
            @ApiResponse(responseCode = "403", description = "채팅방 접근 권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 투표, 채팅방 또는 메시지", content = @Content)
    })
    void markAsRead(
            @Parameter(description = "읽음 처리할 투표 ID", required = true, example = "1") Long voteId,
            @Parameter(hidden = true) Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "마지막으로 읽은 메시지 ID",
                    required = true,
                    content = @Content(schema = @Schema(implementation = MarkAsReadRequest.class))
            )
            MarkAsReadRequest request
    );
}
