package com.hotelbooking.ai.controller;

import com.hotelbooking.ai.dto.ChatRequest;
import com.hotelbooking.ai.dto.ChatResponse;
import com.hotelbooking.ai.service.AiAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class AiAgentController {

    private final AiAgentService aiAgentService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @RequestBody ChatRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        ChatResponse response = aiAgentService.processMessage(request, authorization);
        return ResponseEntity.ok(response);
    }
}
