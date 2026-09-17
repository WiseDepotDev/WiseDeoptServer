package com.huicang.wise.infrastructure.websocket;

import com.huicang.wise.domain.inspection.InspectionProgressEvent;
import com.huicang.wise.domain.inspection.InspectionProgressPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class InspectionProgressPublisherImpl implements InspectionProgressPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public InspectionProgressPublisherImpl(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void publishProgress(InspectionProgressEvent event) {
        eventPublisher.publishEvent(event);
    }
}
