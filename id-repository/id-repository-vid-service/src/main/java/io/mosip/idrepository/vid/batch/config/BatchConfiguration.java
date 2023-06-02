package io.mosip.idrepository.vid.batch.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableBatchProcessing
@ConditionalOnProperty(name="mosip.vid.expiry.notification.enabled", havingValue = "true")
public class BatchConfiguration {
	
	@Autowired
	public VidItemTasklet vidItemTasklet;
	
	/** The job builder factory. */
	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	/** The step builder factory. */
	@Autowired
	public StepBuilderFactory stepBuilderFactory;

	/** The job launcher. */
	@Autowired
	private JobLauncher jobLauncher;

	/** The vid process job. */
	@Autowired
	@Qualifier("vidProcessJob")
	private Job vidProcessJob;
	
	/**
	 * Process job.
	 */
	//@Scheduled(fixedDelayString = "10000")
	@Scheduled(cron="${vid.batch.cron.expression:0 */10 * * * ?}")
	public void processJob() {
		try {
			JobParameters jobParameters = new JobParametersBuilder().addLong("time", System.currentTimeMillis())
					.toJobParameters();
			jobLauncher.run(vidProcessJob, jobParameters);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Vid process job.
	 *
	 * @param listener the listener
	 * @return the job
	 */
	@Bean
	public Job vidProcessJob(JobCompletionNotificationListener listener) throws Exception {
		return jobBuilderFactory.get("vidProcessJob").incrementer(new RunIdIncrementer()).listener(listener)
				.flow(vidProcessStep()).end().build();
	}
	
	@Bean	
	public Step vidProcessStep() {
		return stepBuilderFactory.get("vidProcessJob").tasklet(vidItemTasklet).build();
	}
	
}
