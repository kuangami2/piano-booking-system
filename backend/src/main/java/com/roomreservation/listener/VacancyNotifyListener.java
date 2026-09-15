package com.roomreservation.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.roomreservation.common.EventTypes;
import com.roomreservation.event.EventEnvelope;
import com.roomreservation.service.EventConsumeService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 退约提醒消费者：手动确认，处理成功确认，异常不重回队列并转入死信队列。
 */
@Component
public class VacancyNotifyListener {

    private static final Logger log = LoggerFactory.getLogger(VacancyNotifyListener.class);

    @Resource
    private EventConsumeService eventConsumeService;
    @Resource
    private ObjectMapper objectMapper;

    @RabbitListener(queues = EventTypes.VACANCY_QUEUE, ackMode = "MANUAL")
    public void onBookingCancelled(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            EventEnvelope envelope = objectMapper.readValue(message.getBody(), EventEnvelope.class);
            eventConsumeService.handleBookingCancelled(envelope);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("退约事件处理失败，转入死信队列：{}", e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
