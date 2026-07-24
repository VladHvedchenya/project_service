package faang.school.projectservice.config.redis;

import faang.school.projectservice.dto.stage.StageInvitationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, StageInvitationEvent> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, StageInvitationEvent> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());

        Jackson2JsonRedisSerializer<StageInvitationEvent> serializer =
                new Jackson2JsonRedisSerializer<>(StageInvitationEvent.class);
        template.setValueSerializer(serializer);

        return template;
    }
}