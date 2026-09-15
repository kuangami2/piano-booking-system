package com.roomreservation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomreservation.common.EventTypes;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阶段 C 消息拓扑：业务交换机、提醒队列、死信交换机与死信队列。
 * 队列与交换机在 broker 可用时由 RabbitAdmin 自动声明，不可用时不阻塞应用启动。
 */
@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange roomEventsExchange() {
        return new TopicExchange(EventTypes.EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange roomEventsDlx() {
        return new DirectExchange(EventTypes.DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue vacancyQueue() {
        return QueueBuilder.durable(EventTypes.VACANCY_QUEUE)
                .deadLetterExchange(EventTypes.DLX_EXCHANGE)
                .deadLetterRoutingKey(EventTypes.VACANCY_QUEUE)
                .build();
    }

    @Bean
    public Queue vacancyDlq() {
        return QueueBuilder.durable(EventTypes.DLQ).build();
    }

    @Bean
    public Binding vacancyBinding(Queue vacancyQueue, TopicExchange roomEventsExchange) {
        return BindingBuilder.bind(vacancyQueue).to(roomEventsExchange).with(EventTypes.BOOKING_CANCELLED);
    }

    @Bean
    public Binding vacancyDlqBinding(Queue vacancyDlq, DirectExchange roomEventsDlx) {
        return BindingBuilder.bind(vacancyDlq).to(roomEventsDlx).with(EventTypes.VACANCY_QUEUE);
    }

    /**
     * 转换器只给发布端使用，不做全局 Bean 暴露。
     * 原因：全局 MessageConverter 会被监听容器先用于消息转换，转换失败时
     * ConditionalRejectingErrorHandler 判定为 fatal 并直接拒绝；重投带 x-death 的消息
     * 还会被它 ImmediateAcknowledge 丢弃，导致自研的 DLQ 重投机制失效。
     * 消费者端因此保持原始 Message，由业务代码解析并按自身策略处理异常。
     */
    @Bean
    public RabbitTemplateCustomizer rabbitTemplateCustomizer(ObjectMapper objectMapper) {
        return template -> template.setMessageConverter(new Jackson2JsonMessageConverter(objectMapper));
    }
}
