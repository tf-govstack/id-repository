package io.mosip.idreository.vid.batch.config;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.vid.batch.config.VidItemTasklet;
import io.mosip.idrepository.vid.dto.NotificationResponseDTO;
import io.mosip.idrepository.vid.entity.ExpiredVid;
import io.mosip.idrepository.vid.repository.ExpiredVidRepo;
import io.mosip.idrepository.vid.service.impl.NotificationService;

@RunWith(SpringRunner.class)
@WebMvcTest @Import(EnvUtil.class)
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
public class VidItemTaskletTest {
	
	@InjectMocks
	private  VidItemTasklet  vidItemTasklet;

	@Mock
	private ObjectMapper mapper;
	
	@Mock
	private ExpiredVidRepo expiredVidRepo;
	
	@Mock
	private NotificationService notificationService;
	
	@Mock
	private AuditHelper audit;
	
	private NotificationResponseDTO notificationResponse;	
	private ExpiredVid expiredVid;	
	private List<ExpiredVid> expiredVids;
	
	private static final String SMS_EMAIL_FAILED = "Invalid phone number and email";
	private static final String EMAIL_SUCCESS = "Notification has been sent to the provided email ";
	private static final String SMS_SUCCESS = "Notification has been sent to the provided contact phone number";

	@Before
	public void setUp() {

		notificationResponse = new NotificationResponseDTO();
		notificationResponse.setStatus("status");
		notificationResponse.setMessage("message");
		notificationResponse.setMaskedEmail("maskedEmail");
		notificationResponse.setMaskedPhone("maskedPhone");
		
		expiredVid = new ExpiredVid();
		expiredVid.setId(1);
		expiredVid.setVid("vid");
		expiredVid.setCreatedDate(LocalDate.EPOCH);
		expiredVid.setIsSMSSent(true);
		expiredVid.setIsEmailSent(true);
		
		ReflectionTestUtils.setField(vidItemTasklet, "threadCount", 1);
		vidItemTasklet.init();
		
		expiredVids = new ArrayList<ExpiredVid>();
		expiredVids.add(expiredVid);
	}

	@Test
	public void testProcessSuccess() throws Exception {
		
		notificationResponse.setStatus("success");
		when(notificationService.sendNotification(Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any()))
				.thenReturn(notificationResponse);
		
		when(expiredVidRepo.findNotSentVids()).thenReturn(expiredVids);
		
		RepeatStatus repeatStatus = vidItemTasklet.execute(null, null);
		assertEquals(RepeatStatus.FINISHED, repeatStatus);
	}
	
	@Test
	public void testProcessEmailSuccess() throws Exception {
		
		notificationResponse.setMessage(EMAIL_SUCCESS);
		when(notificationService.sendNotification(Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any()))
				.thenReturn(notificationResponse);
		
		when(expiredVidRepo.findNotSentVids()).thenReturn(expiredVids);
		
		RepeatStatus repeatStatus = vidItemTasklet.execute(null, null);
		assertEquals(RepeatStatus.FINISHED, repeatStatus);
	}
	
	@Test
	public void testProcessSMSSuccess() throws Exception {
		
		notificationResponse.setMessage(SMS_SUCCESS);
		when(notificationService.sendNotification(Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any()))
				.thenReturn(notificationResponse);
		
		when(expiredVidRepo.findNotSentVids()).thenReturn(expiredVids);
		
		RepeatStatus repeatStatus = vidItemTasklet.execute(null, null);
		assertEquals(RepeatStatus.FINISHED, repeatStatus);
	}
	
	@Test
	public void testNotificationException() throws Exception {
		
		notificationResponse.setMessage(SMS_EMAIL_FAILED);
		IdRepoAppException idRepoAppException = new IdRepoAppException(IdRepoErrorConstants.NOTIFICATION_FAILURE.getErrorCode(),
				IdRepoErrorConstants.NOTIFICATION_FAILURE.getErrorMessage() + " " + SMS_EMAIL_FAILED);
		
		when(notificationService.sendNotification(Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any()))
		.thenThrow(idRepoAppException);
		
		when(expiredVidRepo.findNotSentVids()).thenReturn(expiredVids);
		
		RepeatStatus repeatStatus = vidItemTasklet.execute(null, null);
		assertEquals(RepeatStatus.FINISHED, repeatStatus);
	}
	
}