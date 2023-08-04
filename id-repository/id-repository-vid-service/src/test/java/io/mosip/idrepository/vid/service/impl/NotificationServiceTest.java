package io.mosip.idrepository.vid.service.impl;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.vid.dto.NotificationResponseDTO;
import io.mosip.idrepository.vid.dto.TemplateDto;
import io.mosip.idrepository.vid.dto.TemplateResponseDto;
import io.mosip.idrepository.vid.util.JsonUtil;
import io.mosip.idrepository.vid.util.Utility;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.templatemanager.spi.TemplateManager;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
@PrepareForTest({ JsonUtil.class, IOUtils.class, HashSet.class })
public class NotificationServiceTest {

	@InjectMocks
	private NotificationService notificationService;

	@Mock
	private Utility utility;

	@Mock
	private Environment env;

	@Mock
	private AuditHelper audit;

	@Mock
	RestHelper restHelper;

	@Mock
	private TemplateManager templateManager;

	@Mock
	private Map<String, Object> mailingAttributes;
	private ResponseWrapper<NotificationResponseDTO> smsNotificationResponse;

	private static final String EMAILNOTIFIER = "EMAILNOTIFIER";
	private static final String SMS_EMAIL_SUCCESS = "Notification has been sent to the provided contact detail(s)";
	private static final String SMS_SUCCESS = "Notification has been sent to the provided contact phone number";
	private static final String EMAIL_SUCCESS = "Notification has been sent to the provided email ";

	@Before
	public void setUp() throws Exception {
		Map<String, Object> additionalAttributes = new HashMap<>();
		additionalAttributes.put("RID", "10008200070004420191203104356");
		mailingAttributes = new HashMap<String, Object>();
		mailingAttributes.put("fullName_eng", "Test");
		mailingAttributes.put("fullName_ara", "Test");
		mailingAttributes.put("phone", "9876543210");
		mailingAttributes.put("email", "test@test.com");
		Set<String> templateLangauges = new HashSet<String>();
		templateLangauges.add("eng");
		templateLangauges.add("ara");
		ReflectionTestUtils.setField(notificationService, "notificationEmails", "test@test.com|test1@test1.com");
		Mockito.when(utility.getPhoneAttribute()).thenReturn("phone");
		Mockito.when(utility.getEmailAttribute()).thenReturn("email");
		String vid = ("6392586435291409");
		boolean isSMSSent = true;
		boolean isEmailSent = true;
		LocalDate today = LocalDate.now();
		LocalDate expiryDate = today.plusDays(1);
		Mockito.when(env.getProperty(EMAILNOTIFIER)).thenReturn("https://int.mosip.io/template/email");
		ReflectionTestUtils.setField(notificationService, "emailRegex",
				"^[\\w-\\+]+(\\.[\\w]+)*@[\\w-]+(\\.[\\w]+)*(\\.[a-zA-Z]{2,})$");
		ReflectionTestUtils.setField(notificationService, "phoneRegex", "^([6-9]{1})([0-9]{9})$");
		ResponseWrapper<TemplateResponseDto> primaryLangResp = new ResponseWrapper<>();
		TemplateResponseDto primaryTemplateResp = new TemplateResponseDto();
		TemplateDto primaryTemplateDto = new TemplateDto();
		primaryTemplateDto.setDescription("re print uin");
		primaryTemplateDto.setFileText(
				"Hi $name_eng,Your request for \"Reprint Of UIN\" has been successfully placed. Your RID (Req Number) is $RID.");
		List<TemplateDto> primaryTemplateList = new ArrayList<>();
		primaryTemplateList.add(primaryTemplateDto);
		primaryTemplateResp.setTemplates(primaryTemplateList);
		primaryLangResp.setResponse(primaryTemplateResp);
		Mockito.when(restHelper.getApi(Mockito.any(), Mockito.any(), Mockito.anyString(), Mockito.any(),
				Mockito.any(Class.class))).thenReturn(primaryLangResp);

		String primaryTemplatetext = "Hi Test,Your request for \"Reprint Of UIN\" has been successfully placed. Your RID (Req Number) is 10008200070004420191203104356.";
		InputStream primaryIs = new ByteArrayInputStream(primaryTemplatetext.getBytes(StandardCharsets.UTF_8));
		Mockito.when(templateManager.merge(Mockito.any(), Mockito.any())).thenReturn(primaryIs);
		smsNotificationResponse = new ResponseWrapper<>();
		NotificationResponseDTO notificationResp = new NotificationResponseDTO();
		notificationResp.setMessage("Notification has been sent to provided contact details");
		notificationResp.setStatus("success");
		smsNotificationResponse.setResponse(notificationResp);
		Mockito.when(restHelper.postApi(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(Class.class)))
				.thenReturn(smsNotificationResponse);
	}

	@Test
	public void sendNotificationTest() throws Exception, IdRepoAppException, IOException {
		Mockito.when(utility.getMailingAttributes(Mockito.any(), Mockito.any())).thenReturn(mailingAttributes);
		LocalDate today = LocalDate.now();
		LocalDate expiryDate = today.plusDays(1);
		NotificationResponseDTO response = notificationService.sendNotification("6392586435291409", true, true,
				expiryDate);
		assertEquals(SMS_EMAIL_SUCCESS, response.getMessage());

	}

	@Test
	public void sendEmailNotificationTest() throws Exception, IdRepoAppException, IOException {
		LocalDate today = LocalDate.now();
		LocalDate expiryDate = today.plusDays(1);
		mailingAttributes = new HashMap<String, Object>();
		mailingAttributes.put("fullName_eng", "Test");
		mailingAttributes.put("fullName_ara", "Test");
		mailingAttributes.put("phone", null);
		mailingAttributes.put("email", "test@test.com");
		Mockito.when(utility.getMailingAttributes(Mockito.any(), Mockito.any())).thenReturn(mailingAttributes);
		NotificationResponseDTO response = notificationService.sendNotification("6392586435291409", false, true,
				expiryDate);
		assertEquals(EMAIL_SUCCESS, response.getMessage());

	}

	@Test
	public void sendSMSNotificationTest() throws Exception, IdRepoAppException, IOException {
		LocalDate today = LocalDate.now();
		LocalDate expiryDate = today.plusDays(1);
		mailingAttributes = new HashMap<String, Object>();
		mailingAttributes.put("fullName_eng", "Test");
		mailingAttributes.put("fullName_ara", "Test");
		mailingAttributes.put("phone", "9876543210");
		mailingAttributes.put("email", null);
		Mockito.when(utility.getMailingAttributes(Mockito.any(), Mockito.any())).thenReturn(mailingAttributes);
		NotificationResponseDTO response = notificationService.sendNotification("6392586435291409", true, false,
				expiryDate);
		assertEquals(SMS_SUCCESS, response.getMessage());

	}

	@Test(expected = IdRepoAppException.class)
	public void getTemplateNullResponseTest() throws IdRepoAppException {
		LocalDate today = LocalDate.now();
		LocalDate expiryDate = today.plusDays(1);
		ReflectionTestUtils.setField(notificationService, "emailRegex", "");
		ReflectionTestUtils.setField(notificationService, "phoneRegex", "");
		Mockito.when(restHelper.getApi(Mockito.any(), Mockito.any(), Mockito.anyString(), Mockito.any(),
				Mockito.any(Class.class))).thenReturn(null);
		notificationService.sendNotification("6392586435291409", false, false, expiryDate);
	}

}
