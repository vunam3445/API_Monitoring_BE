package com.example.demo.modules.alert.services;

import com.example.demo.modules.alert.entities.AlertDelivery;
import com.example.demo.modules.alert.entities.Incident;
import com.example.demo.modules.alert.enums.AlertChannelType;

public interface IAlertDeliveryService {
    AlertDelivery createPending(Incident incident, AlertChannelType channel, String destination);
    void markSuccess(AlertDelivery delivery, String message);
    void markFailed(AlertDelivery delivery, String errorMessage);


}
