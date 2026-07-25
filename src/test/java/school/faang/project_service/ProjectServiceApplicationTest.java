package school.faang.project_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = faang.school.projectservice.ProjectServiceApplication.class)
@ActiveProfiles("test")
class ProjectServiceApplicationTest {

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    @SuppressWarnings("squid:S2699")
    void contextLoads() {
    }
}