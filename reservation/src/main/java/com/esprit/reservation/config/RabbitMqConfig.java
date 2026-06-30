package com.esprit.reservation.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE = "esprit.events";

    public static final String ROUTING_KEY_CONFIRMED  = "reservation.confirmed";
    public static final String ROUTING_KEY_CANCELLED  = "reservation.cancelled";
    public static final String ROUTING_KEY_WAITLISTED = "reservation.waitlisted";

    public static final String SCHEDULE_UPDATES_QUEUE       = "schedule.updates.for.reservation";
    public static final String ROUTING_KEY_SCHEDULE_UPDATED = "employee.schedule.updated";
    public static final String ROUTING_KEY_MANAGER_ALERT    = "manager.staff.alert";

    public static final String DISH_SYNC_QUEUE      = "dish.sync.for.reservation";
    public static final String DISH_SYNC_QUEUE_DLQ  = "dish.sync.for.reservation.dlq";
    // Bound explicitly (not "dish.*") because dish.unavailable carries a different,
    // narrower payload shape than DishSyncEvent — "became unavailable" is instead
    // detected from dish.updated's `available=false` field.
    public static final String[] DISH_ROUTING_KEYS = {"dish.created", "dish.updated", "dish.deleted"};

    @Bean
    public TopicExchange espritEventsExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue scheduleUpdatesQueue() {
        return QueueBuilder.durable(SCHEDULE_UPDATES_QUEUE).build();
    }

    @Bean
    public Binding scheduleUpdatesBinding(Queue scheduleUpdatesQueue, TopicExchange espritEventsExchange) {
        return BindingBuilder
                .bind(scheduleUpdatesQueue)
                .to(espritEventsExchange)
                .with(ROUTING_KEY_SCHEDULE_UPDATED);
    }

    @Bean
    public Queue dishSyncQueueDlq() {
        return QueueBuilder.durable(DISH_SYNC_QUEUE_DLQ).build();
    }

    @Bean
    public Queue dishSyncQueue() {
        return QueueBuilder.durable(DISH_SYNC_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(DISH_SYNC_QUEUE_DLQ)
                .build();
    }

    @Bean
    public Declarables dishSyncBindings(Queue dishSyncQueue, TopicExchange espritEventsExchange) {
        return new Declarables(java.util.Arrays.stream(DISH_ROUTING_KEYS)
                .map(key -> BindingBuilder.bind(dishSyncQueue).to(espritEventsExchange).with(key))
                .map(Binding.class::cast)
                .toList());
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
