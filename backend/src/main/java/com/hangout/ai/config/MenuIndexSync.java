package com.hangout.ai.config;

import com.hangout.ai.service.IndexingService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class MenuIndexSync implements ApplicationRunner {
    private final IndexingService indexing;

    public MenuIndexSync(IndexingService indexing) {
        this.indexing = indexing;
    }

    @Override
    public void run(ApplicationArguments args) {
        indexing.syncSharedMenus();
    }
}
