package io.mosip.idreository.vid.batch.config;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;

import io.mosip.idrepository.vid.batch.config.BatchConfiguration;

@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
public class BatchConfigurationTest {

	@InjectMocks
	private BatchConfiguration batchConfiguration;

	@Mock
	public JobBuilderFactory jobBuilderFactory;

	@Mock
	public StepBuilderFactory stepBuilderFactory;

	@Mock
	private JobLauncher jobLauncher;
	
	@Mock
	private Job vidProcessJob;

	
	@Before
	public void before() {
	}
	
	@Test
	public void processJobTest() {
		batchConfiguration.processJob();
	}

}
