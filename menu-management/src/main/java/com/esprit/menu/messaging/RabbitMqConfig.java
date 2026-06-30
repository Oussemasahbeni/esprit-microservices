package com.esprit.menu.messaging;

import org.springframework.amqp.core.ExchangeBuilder;
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
    public static final String ROUTING_KEY_DISH_UNAVAILABLE = "dish.unavailable";
    public static final String ROUTING_KEY_PROMO_CREATED = "promo.created";

    public static final String ROUTING_KEY_DISH_CREATED = "dish.created";
    public static final String ROUTING_KEY_DISH_UPDATED = "dish.updated";
    public static final String ROUTING_KEY_DISH_DELETED = "dish.deleted";

    @Bean
    public TopicExchange espritEventsExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
