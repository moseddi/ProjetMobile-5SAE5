package com.hangout.ai;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final ReviewRagService ragService;
    private final ReviewIndexService indexService;
    private final SimpleRateLimiter rateLimiter;

    @PostMapping("/recommend")
    public RagResponse recommend(@Valid @RequestBody RagRequest request, HttpServletRequest http) {
        rateLimiter.check("rag-" + http.getRemoteAddr());
        return ragService.recommend(request);
    }

    @PostMapping("/reindex")
    public ReviewIndexService.SyncResult reindex(HttpServletRequest http) {
        rateLimiter.check("rag-index-" + http.getRemoteAddr());
        return indexService.sync();
    }
}