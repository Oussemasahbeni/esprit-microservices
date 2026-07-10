package com.esprit.delivery.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange this service publishes order lifecycle events to
    public static final String DELIVERY_EXCHANGE = "delivery.exchange";
    public static final String ORDER_EVENTS_QUEUE = "delivery.order-events.queue";
    public static final String ORDER_EVENTS_ROUTING_PATTERN = "order.*";

    // Routing keys for outbound events
    public static final String ROUTING_ORDER_PLACED = "order.placed";
    public static final String ROUTING_ORDER_STATUS_CHANGED = "order.status-changed";
    public static final String ROUTING_ORDER_DELIVERED = "order.delivered";
    public static final String ROUTING_ORDER_FLAGGED = "order.flagged-for-review";

    // Inbound: menu-service tells us a dish became unavailable
    public static final String MENU_EXCHANGE = "menu.exchange";
    public static final String DISH_UNAVAILABLE_QUEUE = "delivery.dish-unavailable.queue";
    public static final String ROUTING_DISH_UNAVAILABLE = "dish.unavailable";

    @Bean
    public TopicExchange deliveryExchange() {
        return new TopicExchange(DELIVERY_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange menuExchange() {
        return new TopicExchange(MENU_EXCHANGE, true, false);
    }

    @Bean
    public Queue dishUnavailableQueue() {
        // durable, with a dead-letter setup so poison messages don't loop forever
        return QueueBuilder.durable(DISH_UNAVAILABLE_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", DISH_UNAVAILABLE_QUEUE + ".dlq")
                .build();
    }

    @Bean
    public Queue dishUnavailableDlq() {
        return QueueBuilder.durable(DISH_UNAVAILABLE_QUEUE + ".dlq").build();
    }

    @Bean
    public Binding dishUnavailableBinding(Queue dishUnavailableQueue, TopicExchange menuExchange) {
        return BindingBuilder.bind(dishUnavailableQueue)
                .to(menuExchange)
                .with(ROUTING_DISH_UNAVAILABLE);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();

        DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
        typeMapper.setTrustedPackages("com.esprit.delivery.messaging.events");
        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        template.setMandatory(true);
        template.setReturnsCallback(returned ->
                org.slf4j.LoggerFactory.getLogger(RabbitMQConfig.class)
                        .error("Message returned (unroutable): exchange={}, routingKey={}, replyText={}",
                                returned.getExchange(), returned.getRoutingKey(), returned.getReplyText())
        );
        return template;
    }

    @Bean
    public Queue orderEventsQueue() {
        return QueueBuilder.durable(ORDER_EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", ORDER_EVENTS_QUEUE + ".dlq")
                .build();
    }

    @Bean
    public Queue orderEventsDlq() {
        return QueueBuilder.durable(ORDER_EVENTS_QUEUE + ".dlq").build();
    }

    @Bean
    public Binding orderEventsBinding(Queue orderEventsQueue, TopicExchange deliveryExchange) {
        return BindingBuilder.bind(orderEventsQueue)
                .to(deliveryExchange)
                .with(ORDER_EVENTS_ROUTING_PATTERN);
    }
}