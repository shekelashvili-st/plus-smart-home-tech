package ru.yandex.practicum.telemetry.analyzer.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AnalyzerRunner implements CommandLineRunner {
    final HubEventProcessor hubEventProcessor;
    final SnapshotEventProcessor snapshotEventProcessor;

    @Override
    public void run(String... args) throws Exception {
        Thread hubEventsThread = new Thread(hubEventProcessor);
        hubEventsThread.setName("HubEventProcessorThread");
        hubEventsThread.start();

        snapshotEventProcessor.start();
    }
}
