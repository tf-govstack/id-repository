package io.mosip.idrepository.vid.service.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import io.mosip.idrepository.core.constant.AuditEvents;
import io.mosip.idrepository.core.constant.AuditModules;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.vid.dto.NotificationResponseDTO;
import io.mosip.idrepository.vid.dto.SMSRequestDTO;
import io.mosip.idrepository.vid.dto.TemplateDto;
import io.mosip.idrepository.vid.dto.TemplateResponseDto;
import io.mosip.idrepository.vid.util.JsonUtil;
import io.mosip.idrepository.vid.util.Utility;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.templatemanager.spi.TemplateManager;

@Component
public class NotificationService {
	
	private static final Logger mosipLogger = IdRepoLogger.getLogger(NotificationService.class);
	
	private static final String CLASS_NOTIFICATION_SERVICE = "NotificationService";
	private static final String METHOD_SEND_NOTIFICATION = "sendNotification";
	private static final String METHOD_SEND_EMAIL_NOTIFICATION = "sendEmailNotification";
	private static final String METHOD_SEND_SMS_NOTIFICATION = "sendSMSNotification";
	private static final String METHOD_TEMPLATE_MERGE = "templateMerge";
	
	@Autowired
	private TemplateManager templateManager;
	
	@Autowired
	private Utility utility;
	
	@Autowired
	RestHelper restHelper;
	
	@Autowired
	private Environment env;
	
	@Autowired
	private AuditHelper audit;
	
	@Value("${vid.notification.emails}")
	private String notificationEmails;
	
	@Value("${mosip.id.validation.identity.email}")
	private String emailRegex;
	
	@Value("${mosip.id.validation.identity.phone}")
	private String phoneRegex;
	
	private static final String LINE_BREAK = "<br>";
	private static final String LINE_SEPARATOR = " "/*new  StringBuilder().append(LINE_BREAK).append(LINE_BREAK).toString()*/;
	private static final String SEPARATOR = "/";
	private static final String EMAIL = "_EMAIL";
	private static final String SMS = "_SMS";
	private static final String SUBJECT = "_SUB";
	private static final String SMS_EMAIL_SUCCESS = "Notification has been sent to the provided contact detail(s)";
	private static final String SMS_SUCCESS = "Notification has been sent to the provided contact phone number";
	private static final String EMAIL_SUCCESS = "Notification has been sent to the provided email ";
	private static final String SUCCESS = "success";
	private static final String SMS_EMAIL_FAILED = "Invalid phone number and email";
	private static final String NOTIFICATION_TEMPLATE_CODE = "VS_VID_EXPIRE";
	private static final String IS_SMS_NOTIFICATION_SUCCESS = "NotificationService::sendSMSNotification()::isSuccess?::";
	private static final String IS_EMAIL_NOTIFICATION_SUCCESS = "NotificationService::sendEmailNotification()::isSuccess?::";
	private static final String TEMPLATE_CODE = "Template Code";
	private static final String VID = "vid";
	private static final String EXPIRY_DATE = "expiry_date";
	private static final String PHONE = "phone";
	private static final String SMSNOTIFIER = "SMSNOTIFIER";
	private static final String EMAILNOTIFIER = "EMAILNOTIFIER";
	private static final String TEMPLATES = "TEMPLATES";

	public NotificationResponseDTO sendNotification(String vid, boolean isSMSSent, boolean isEmailSent, LocalDate expiryDate) throws IdRepoAppException {
		mosipLogger.debug(IdRepoSecurityManager.getUser(), CLASS_NOTIFICATION_SERVICE, METHOD_SEND_NOTIFICATION,
				vid);
		boolean smsStatus = isSMSSent;
		boolean emailStatus = isEmailSent;
		Set<String> templateLangauges = new HashSet<String>();
		
		Map<String, Object> notificationAttributes = utility.getMailingAttributes(vid, templateLangauges);
		notificationAttributes.put(VID, utility.convertToMaskDataFormat(vid));
		notificationAttributes.put(EXPIRY_DATE, expiryDate.plusDays(1).toString());
		
		if (!isSMSSent) {
			smsStatus = sendSMSNotification(notificationAttributes, templateLangauges);
		}
		
		if (!isEmailSent) {
			emailStatus = sendEmailNotification(notificationAttributes, templateLangauges, null);
		}
		mosipLogger.info(IdRepoSecurityManager.getUser(), CLASS_NOTIFICATION_SERVICE, METHOD_SEND_NOTIFICATION,
				IS_SMS_NOTIFICATION_SUCCESS + smsStatus);
		mosipLogger.info(IdRepoSecurityManager.getUser(), CLASS_NOTIFICATION_SERVICE, METHOD_SEND_NOTIFICATION,
				IS_EMAIL_NOTIFICATION_SUCCESS + emailStatus);
		
		NotificationResponseDTO notificationResponse = new NotificationResponseDTO();
		
		if (smsStatus && emailStatus) {
			notificationResponse.setMessage(SMS_EMAIL_SUCCESS);
			notificationResponse.setStatus(SUCCESS);
		} else if (smsStatus) {	
			notificationResponse.setMessage(SMS_SUCCESS);
			//notificationResponse.setMaskedPhone(utility.maskPhone((String)notificationAttributes.get(PHONE)));
		} else if (emailStatus) {
			notificationResponse.setMessage(EMAIL_SUCCESS);
			//notificationResponse.setMaskedEmail(utility.maskEmail((String)notificationAttributes.get(utility.getEmailAttribute())));
		} else {
			notificationResponse.setMessage(SMS_EMAIL_FAILED);
			throw new IdRepoAppException(IdRepoErrorConstants.NOTIFICATION_FAILURE.getErrorCode(),
					IdRepoErrorConstants.NOTIFICATION_FAILURE.getErrorMessage() + " " + SMS_EMAIL_FAILED);
		}
		
		mosipLogger.info(IdRepoSecurityManager.getUser(), CLASS_NOTIFICATION_SERVICE, METHOD_SEND_NOTIFICATION,
				"NotificationService::sendSMSNotification()::isSuccess?::" + notificationResponse.getMessage());
		mosipLogger.debug(IdRepoSecurityManager.getUser(), CLASS_NOTIFICATION_SERVICE, METHOD_SEND_NOTIFICATION,
				"NotificationService::sendNotification()::exit");
		
		return notificationResponse;
	}
	
	private boolean sendEmailNotification(Map<String, Object> mailingAttributes, Set<String> templateLangauges, MultipartFile[] attachment) throws IdRepoAppException {
		mosipLogger.debug(IdRepoSecurityManager.getUser(), CLASS_NOTIFICATION_SERVICE, METHOD_SEND_EMAIL_NOTIFICATION,
				"NotificationService::sendEmailNotification()::entry");
		String email = String.valueOf(mailingAttributes.get(utility.getEmailAttribute()));
		
		if (email == null || email.isEmpty() || !(email.matches(emailRegex))) {
			mosipLogger.info(IdRepoSecurityManager.getUser(), CLASS_NOTIFICATION_SERVICE, METHOD_SEND_EMAIL_NOTIFICATION,
					"NotificationService::sendEmailNotification()::emailValidation::" + "false :: invalid email");
			return false;
		}
		
		String mergedEmailSubject = "";
		String mergedTemplate = "";
		for (String language : templateLangauges) {
			String emailSubject = "";
			String languageTemplate = "";
			
			emailSubject = getTemplate(language, NOTIFICATION_TEMPLATE_CODE + EMAIL + SUBJECT);
			languageTemplate = templateMerge(getTemplate(language, NOTIFICATION_TEMPLATE_CODE + EMAIL),
					mailingAttributes);
			
			if(languageTemplate.trim().endsWith(LINE_BREAK)) {
				languageTemplate = languageTemplate.substring(0, languageTemplate.length() - LINE_BREAK.length()).trim();
			}
			if (mergedTemplate.isBlank() || mergedEmailSubject.isBlank()) {
				mergedTemplate = languageTemplate;
				mergedEmailSubject = emailSubject;
			} else {
				mergedTemplate = mergedTemplate + LINE_SEPARATOR + languageTemplate;
				mergedEmailSubject = mergedEmailSubject + SEPARATOR + emailSubject;
			}
		}
		
		LinkedMultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
		String[] mailTo = new String[1];
		mailTo[0] = email;

		String[] mailCc = notificationEmails.split("\\|");

		for (String item : mailTo) {
			params.add("mailTo", item);
		}

		if (mailCc != null) {
			for (String item : mailCc) {
				params.add("mailCc", item);
			}
		}

		try {
			params.add("mailSubject", mergedEmailSubject);
			params.add("mailContent", mergedTemplate);
			params.add("attachments", attachment);
			ResponseWrapper<NotificationResponseDTO> response;
			
			response = restHelper.postApi(env.getProperty(EMAILNOTIFIER), MediaType.MULTIPART_FORM_DATA, params,
					ResponseWrapper.class);
			
			if (response == null || response.getResponse() == null || response.getErrors() != null && !response.getErrors().isEmpty()) {
				throw new IdRepoAppException(IdRepoErrorConstants.INVALID_API_RESPONSE.getErrorCode(),
						IdRepoErrorConstants.INVALID_API_RESPONSE.getErrorMessage() + " EMAILNOTIFIER API"
								+ (response != null ? response.getErrors().get(0) : ""));
			}
			NotificationResponseDTO notifierResponse = JsonUtil
					.readValue(JsonUtil.writeValueAsString(response.getResponse()), NotificationResponseDTO.class);
			mosipLogger.info(IdRepoSecurityManager.getUser(), CLASS_NOTIFICATION_SERVICE, METHOD_SEND_EMAIL_NOTIFICATION,
					"NotificationService::sendEmailNotification()::response::"
							+ JsonUtil.writeValueAsString(notifierResponse));
			
			if (SUCCESS.equals(notifierResponse.getStatus())) {
				mosipLogger.debug(IdRepoSecurityManager.getUser(), CLASS_NOTIFICATION_SERVICE, METHOD_SEND_EMAIL_NOTIFICATION,
						"NotificationService::sendEmailNotification()::exit");
				return true;
			}
		} catch (IOException e) {
			audit.auditError(AuditModules.ID_REPO_VID_SERVICE, AuditEvents.TOKEN_GENERATION_FAILED, "IDR-TOK", IdType.VID, e);
			throw new IdRepoAppException(IdRepoErrorConstants.TOKEN_GENERATION_FAILED.getErrorCode(),
					IdRepoErrorConstants.TOKEN_GENERATION_FAILED.getErrorMessage(), e);
		} catch (Exception e) {
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				throw new IdRepoAppException(
						IdRepoErrorConstants.API_RESOURCE_ACCESS_EXCEPTION.getErrorCode(),
						httpClientException.getResponseBodyAsString());

			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				throw new IdRepoAppException(
						IdRepoErrorConstants.API_RESOURCE_ACCESS_EXCEPTION.getErrorCode(),
						httpServerException.getResponseBodyAsString());
			} else {
				throw new IdRepoAppException(
						IdRepoErrorConstants.API_RESOURCE_ACCESS_EXCEPTION.getErrorCode(),
						IdRepoErrorConstants.API_RESOURCE_ACCESS_EXCEPTION.getErrorMessage() + e.getMessage(), e);
			}
		}
		mosipLogger.debug(IdRepoSecurityManager.getUser(), CLASS_NOTIFICATION_SERVICE, METHOD_SEND_EMAIL_NOTIFICATION,
				"NotificationService::sendEmailNotification()::exit");
		
		return false;

	}
	
	private boolean sendSMSNotification(Map<String, Object> mailingAttributes, Set<String> templateLangauges) throws IdRepoAppException {
		mosipLogger.debug(IdRepoSecurityManager.getUser(), CLASS_NOTIFICATION_SERVICE, METHOD_SEND_SMS_NOTIFICATION,
				"NotificationService::sendSMSNotification()::entry");
		String phone = (String)mailingAttributes.get(PHONE);
		
		if(phone == null){
			phone = (String) mailingAttributes.get(utility.getPhoneAttribute());
		}
		
		if (phone == null || phone.isEmpty() || !phone.matches(phoneRegex)) {
			mosipLogger.info(IdRepoSecurityManager.getUser(), CLASS_NOTIFICATION_SERVICE, METHOD_SEND_SMS_NOTIFICATION,
					"NotificationService::sendSMSNotification()::phoneValidation::" + "false :: invalid phone number");
			return false;
		}
		String mergedTemplate = "";
		for (String language : templateLangauges) {
			String languageTemplate = "";
			languageTemplate = templateMerge(getTemplate(language, NOTIFICATION_TEMPLATE_CODE + SMS),
					mailingAttributes);
			if(languageTemplate.trim().endsWith(LINE_BREAK)) {
				languageTemplate = languageTemplate.substring(0, languageTemplate.length() - LINE_BREAK.length()).trim();
			}
			if (mergedTemplate.isBlank()) {
				mergedTemplate = languageTemplate;
			}else {
				mergedTemplate = mergedTemplate + LINE_SEPARATOR
						+ languageTemplate;
			}
		}
		
		SMSRequestDTO smsRequestDTO = new SMSRequestDTO();
		smsRequestDTO.setMessage(mergedTemplate);
		smsRequestDTO.setNumber(phone);
		RequestWrapper<SMSRequestDTO> req = new RequestWrapper<>();
		req.setRequest(smsRequestDTO);
		ResponseWrapper<NotificationResponseDTO> response;
		try {
			response = restHelper.postApi(env.getProperty(SMSNOTIFIER), MediaType.APPLICATION_JSON, req,
					ResponseWrapper.class);
			if (response == null || response.getResponse() == null || response.getErrors() != null && !response.getErrors().isEmpty()) {
				throw new IdRepoAppException(IdRepoErrorConstants.INVALID_API_RESPONSE.getErrorCode(),
						IdRepoErrorConstants.INVALID_API_RESPONSE.getErrorMessage() + " SMSNOTIFIER API"
								+ (response != null ? response.getErrors().get(0) : ""));
			}
			NotificationResponseDTO notifierResponse = JsonUtil
					.readValue(JsonUtil.writeValueAsString(response.getResponse()), NotificationResponseDTO.class);
			mosipLogger.info(IdRepoSecurityManager.getUser(), CLASS_NOTIFICATION_SERVICE, METHOD_SEND_SMS_NOTIFICATION,
					"NotificationService::sendSMSNotification()::response::"
							+ JsonUtil.writeValueAsString(notifierResponse));

			if (SUCCESS.equalsIgnoreCase(notifierResponse.getStatus())) {
				mosipLogger.debug(IdRepoSecurityManager.getUser(), CLASS_NOTIFICATION_SERVICE, METHOD_SEND_SMS_NOTIFICATION,
						"NotificationService::sendSMSNotification()::entry");
				return true;
			}
		} catch (IOException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_NOTIFICATION_SERVICE, METHOD_SEND_SMS_NOTIFICATION,
					e.getMessage() + ExceptionUtils.getStackTrace(e));
			audit.auditError(AuditModules.ID_REPO_VID_SERVICE, AuditEvents.TOKEN_GENERATION_FAILED, "IDR-TOK", IdType.VID, e);
			throw new IdRepoAppException(IdRepoErrorConstants.TOKEN_GENERATION_FAILED.getErrorCode(),
					IdRepoErrorConstants.TOKEN_GENERATION_FAILED.getErrorMessage(), e);
		} catch (Exception e) {
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_NOTIFICATION_SERVICE, METHOD_SEND_SMS_NOTIFICATION,
						e.getMessage() + httpClientException.getResponseBodyAsString());
				throw new IdRepoAppException(
						IdRepoErrorConstants.API_RESOURCE_ACCESS_EXCEPTION.getErrorCode(),
						httpClientException.getResponseBodyAsString());

			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_NOTIFICATION_SERVICE, METHOD_SEND_SMS_NOTIFICATION,
						e.getMessage() + httpServerException.getResponseBodyAsString());
				throw new IdRepoAppException(
						IdRepoErrorConstants.API_RESOURCE_ACCESS_EXCEPTION.getErrorCode(),
						httpServerException.getResponseBodyAsString());
			} else {
				mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_NOTIFICATION_SERVICE, METHOD_SEND_SMS_NOTIFICATION,
						e.getMessage() + ExceptionUtils.getStackTrace(e));
				throw new IdRepoAppException(
						IdRepoErrorConstants.API_RESOURCE_ACCESS_EXCEPTION.getErrorCode(),
						IdRepoErrorConstants.API_RESOURCE_ACCESS_EXCEPTION.getErrorMessage() + e.getMessage(), e);
			}
		}
		mosipLogger.debug(IdRepoSecurityManager.getUser(), CLASS_NOTIFICATION_SERVICE, METHOD_SEND_SMS_NOTIFICATION,
				"NotificationService::sendSMSNotification()::exit");

		return false;
	}
	
	@SuppressWarnings("unchecked")
	private String getTemplate(String langCode, String templatetypecode) throws IdRepoAppException  {
		mosipLogger.debug(IdRepoSecurityManager.getUser(), TEMPLATE_CODE, templatetypecode,
				"NotificationService::getTemplate()::entry");
		List<String> pathSegments = new ArrayList<>();
		pathSegments.add(langCode);
		pathSegments.add(templatetypecode);
		try {
			ResponseWrapper<TemplateResponseDto> resp = (ResponseWrapper<TemplateResponseDto>) restHelper.getApi(
					env.getProperty(TEMPLATES), pathSegments, "", null, ResponseWrapper.class);
			if (resp == null || resp.getErrors() != null && !resp.getErrors().isEmpty()) {
				audit.audit(AuditModules.ID_REPO_VID_SERVICE, AuditEvents.TEMPLATE_EXCEPTION, "IDR-TEMP", IdType.VID, "Template Exception");
				throw new IdRepoAppException(IdRepoErrorConstants.TEMPLATE_EXCEPTION.getErrorCode(),
						IdRepoErrorConstants.TEMPLATE_EXCEPTION.getErrorMessage()
								+ (resp != null ? resp.getErrors().get(0) : ""));
			}
			TemplateResponseDto templateResponse = JsonUtil.readValue(JsonUtil.writeValueAsString(resp.getResponse()),
					TemplateResponseDto.class);
			mosipLogger.info(IdRepoSecurityManager.getUser(), TEMPLATE_CODE, templatetypecode,
					"NotificationService::getTemplate()::getTemplateResponse::" + JsonUtil.writeValueAsString(resp));
			List<TemplateDto> response = templateResponse.getTemplates();
			mosipLogger.debug(IdRepoSecurityManager.getUser(), TEMPLATE_CODE, templatetypecode,
					"NotificationService::getTemplate()::exit");
			return response.get(0).getFileText().replaceAll("(^\")|(\"$)", "");
		} catch (IOException e) {
			audit.auditError(AuditModules.ID_REPO_VID_SERVICE, AuditEvents.TOKEN_GENERATION_FAILED, "IDR-TOK", IdType.VID, e);
			throw new IdRepoAppException(IdRepoErrorConstants.TOKEN_GENERATION_FAILED.getErrorCode(),
					IdRepoErrorConstants.TOKEN_GENERATION_FAILED.getErrorMessage(), e);
		} catch (Exception e) {
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				throw new IdRepoAppException(
						IdRepoErrorConstants.API_RESOURCE_ACCESS_EXCEPTION.getErrorCode(),
						httpClientException.getResponseBodyAsString());

			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				throw new IdRepoAppException(
						IdRepoErrorConstants.API_RESOURCE_ACCESS_EXCEPTION.getErrorCode(),
						httpServerException.getResponseBodyAsString());
			} else {
				throw new IdRepoAppException(
						IdRepoErrorConstants.API_RESOURCE_ACCESS_EXCEPTION.getErrorCode(),
						IdRepoErrorConstants.API_RESOURCE_ACCESS_EXCEPTION.getErrorMessage() + e.getMessage(), e);
			}
		}
	}
	
	private String templateMerge(String fileText, Map<String, Object> mailingAttributes)
	 throws IdRepoAppException  {
		mosipLogger.debug(IdRepoSecurityManager.getUser(), CLASS_NOTIFICATION_SERVICE, METHOD_TEMPLATE_MERGE,
				"NotificationService::templateMerge()::entry");
		try {
			String mergeTemplate;
			InputStream templateInputStream = new ByteArrayInputStream(fileText.getBytes(Charset.forName("UTF-8")));

			InputStream resultedTemplate = templateManager.merge(templateInputStream, mailingAttributes);

			mergeTemplate = IOUtils.toString(resultedTemplate, StandardCharsets.UTF_8.name());
			mosipLogger.debug(IdRepoSecurityManager.getUser(), CLASS_NOTIFICATION_SERVICE, METHOD_TEMPLATE_MERGE,
					"NotificationService::templateMerge()::exit");
			return mergeTemplate;
		} catch (IOException e) {
			throw new IdRepoAppException(IdRepoErrorConstants.IO_EXCEPTION.getErrorCode(),
					IdRepoErrorConstants.IO_EXCEPTION.getErrorMessage(), e);
		}
	}
	
}
