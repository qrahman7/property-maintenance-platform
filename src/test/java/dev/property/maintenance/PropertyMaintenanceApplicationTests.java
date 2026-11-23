package dev.property.maintenance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@MockitoBean(types = ChatModel.class)
class PropertyMaintenanceApplicationTests {

	@Test
	void contextLoads() {
	}

}
