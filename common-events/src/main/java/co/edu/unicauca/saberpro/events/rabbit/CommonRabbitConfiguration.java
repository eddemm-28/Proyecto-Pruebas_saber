package co.edu.unicauca.saberpro.events.rabbit;

import co.edu.unicauca.saberpro.events.EventTopology;
import co.edu.unicauca.saberpro.events.messaging.EventPublisher;
import co.edu.unicauca.saberpro.events.messaging.EventSerializer;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración compartida de RabbitMQ. Cada servicio la importa con
 * {@code @Import(CommonRabbitConfiguration.class)} y declara sus propias colas con
 * {@link #durableQueueWithDeadLetter(String, String)}.
 */
@Configuration(proxyBeanMethods = false)
public class CommonRabbitConfiguration {

    @Bean
    public TopicExchange saberproEventsExchange() {
        return new TopicExchange(EventTopology.EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange saberproDeadLetterExchange() {
        return new TopicExchange(EventTopology.DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    @ConditionalOnMissingBean
    public EventSerializer eventSerializer() {
        return new EventSerializer();
    }

    @Bean
    @ConditionalOnMissingBean(EventPublisher.class)
    public EventPublisher eventPublisher(RabbitTemplate rabbitTemplate, EventSerializer serializer) {
        rabbitTemplate.setMandatory(true);
        return new RabbitEventPublisher(rabbitTemplate, serializer);
    }

    @Bean
    @ConditionalOnMissingBean
    public RabbitEventDispatcher rabbitEventDispatcher(EventSerializer serializer) {
        return new RabbitEventDispatcher(serializer);
    }

    /**
     * Cola durable ligada a {@code saberpro.events} con su DLQ ligada al DLX con la misma routing key.
     */
    public static Declarables durableQueueWithDeadLetter(String queueName, String routingKey) {
        String dlqName = EventTopology.deadLetterQueueOf(queueName);
        TopicExchange exchange = new TopicExchange(EventTopology.EXCHANGE, true, false);
        TopicExchange dlx = new TopicExchange(EventTopology.DEAD_LETTER_EXCHANGE, true, false);

        Queue queue = QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", EventTopology.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", routingKey)
                .build();
        Queue dlq = QueueBuilder.durable(dlqName).build();

        Binding binding = BindingBuilder.bind(queue).to(exchange).with(routingKey);
        Binding dlqBinding = BindingBuilder.bind(dlq).to(dlx).with(routingKey);
        return new Declarables(queue, dlq, binding, dlqBinding);
    }
}
