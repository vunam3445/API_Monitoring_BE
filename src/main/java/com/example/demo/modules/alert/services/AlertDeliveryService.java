package com.example.demo.modules.alert.services;

import com.example.demo.modules.alert.entities.AlertDelivery;
import com.example.demo.modules.alert.entities.Incident;
import com.example.demo.modules.alert.enums.AlertChannelType;
import com.example.demo.modules.alert.enums.AlertDeliveryStatus;
import com.example.demo.modules.alert.repositories.AlertDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AlertDeliveryService implements IAlertDeliveryService {

    private final AlertDeliveryRepository deliveryRepository;

    @Transactional
    public AlertDelivery createPending(Incident incident, AlertChannelType channel, String destination) {
        AlertDelivery delivery = AlertDelivery.builder()
                .incident(incident)
                .channel(channel)
                .destination(destination)
                .status(AlertDeliveryStatus.PENDING)
                .build();
        return deliveryRepository.save(delivery);
    }

    @Transactional
    public void markSuccess(AlertDelivery delivery, String message) {
        delivery.setStatus(AlertDeliveryStatus.SENT);
        delivery.setMessage(message);
        delivery.setSentAt(LocalDateTime.now());
        deliveryRepository.save(delivery);
    }

    @Transactional
    public void markFailed(AlertDelivery delivery, String errorMessage) {
        delivery.setStatus(AlertDeliveryStatus.FAILED);
        delivery.setErrorMessage(errorMessage);
        delivery.setRetryCount(delivery.getRetryCount() + 1);
        deliveryRepository.save(delivery);
    }

    
}
