package com.esprit.employee.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
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

    public static final String ROUTING_KEY_SCHEDULE_UPDATED    = "employee.schedule.updated";
    public static final String ROUTING_KEY_RESERVATION_CONFIRMED = "reservation.confirmed";

    public static final String RESERVATION_CONFIRMED_QUEUE = "reservation.confirmed.for.employee";

    @Bean
    public TopicExchange espritEventsExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue reservationConfirmedQueue() {
        return QueueBuilder.durable(RESERVATION_CONFIRMED_QUEUE).build();
    }

    @Bean
    public Binding reservationConfirmedBinding(Queue reservationConfirmedQueue,
                                               TopicExchange espritEventsExchange) {
        return BindingBuilder
                .bind(reservationConfirmedQueue)
                .to(espritEventsExchange)
                .with(ROUTING_KEY_RESERVATION_CONFIRMED);
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
