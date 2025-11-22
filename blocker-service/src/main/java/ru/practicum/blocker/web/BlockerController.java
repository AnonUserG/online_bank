package ru.practicum.blocker.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.blocker.service.BlockerService;
import ru.practicum.blocker.web.dto.BlockCheckRequest;
import ru.practicum.blocker.web.dto.BlockCheckResponse;

@RestController
@RequestMapping("/api/blocker")
@RequiredArgsConstructor
public class BlockerController {

    private final BlockerService blockerService;

    @PostMapping("/check")
    public ResponseEntity<BlockCheckResponse> check(@RequestBody @Valid BlockCheckRequest request) {
        return ResponseEntity.ok(blockerService.check(request));
    }
}
