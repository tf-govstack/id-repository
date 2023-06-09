package io.mosip.idrepository.vid.batch.config;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.logger.spi.Logger;

@Component
public class JobCompletionNotificationListener extends JobExecutionListenerSupport {

	private static final Logger LOGGER = IdRepoLogger.getLogger(JobCompletionNotificationListener.class);

	private static final String COMPLETIONNOTIFICATIONLISTENER = "JobCompletionNotificationListener";
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.core.listener.JobExecutionListenerSupport#afterJob(
	 * org.springframework.batch.core.JobExecution)
	 */
	@Override
	public void afterJob(JobExecution jobExecution) {
		if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
			LOGGER.debug(IdRepoSecurityManager.getUser(), COMPLETIONNOTIFICATIONLISTENER,
					jobExecution.getJobId(), "Job completed successfully");
		}
	}

}
