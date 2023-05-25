package io.mosip.idrepository.vid.service.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;

import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.vid.dto.NotificationResponseDTO;
import io.mosip.idrepository.vid.util.JsonUtil;
import io.mosip.idrepository.vid.util.Utility;
import io.mosip.kernel.core.http.ResponseWrapper;

@Component
public class NotificationService {
	
	@Autowired
	private Utility utility;
	
	@Autowired
	RestHelper restHelper;
	
	@Autowired
	private Environment env;
	
	@Value("${vid.notification.emails}")
	private String notificationEmails;
	
	@Value("${mosip.id.validation.identity.email}")
	private String emailRegex;

	public void sendNotification(String vid) {
		boolean smsStatus = false;
		boolean emailStatus = false;
		Set<String> templateLangauges = new HashSet<String>();
		
		Map<String, Object> notificationAttributes = utility.getMailingAttributes(vid, templateLangauges);
		
		//smsStatus = sendSMSNotification(notificationAttributes, templateLangauges);
		emailStatus = sendEmailNotification(notificationAttributes, templateLangauges);
	}
	
	private boolean sendEmailNotification(Map<String, Object> mailingAttributes, Set<String> templateLangauges) {
		//String eventId=(String) mailingAttributes.get(TemplateVariablesConstants.EVENT_ID);
		String email = String.valueOf(mailingAttributes.get("email"));
		String otp="";
		
		if (email == null || email.isEmpty() || !(email.matches(emailRegex))) {
//			logger.info(LoggerFileConstant.APPLICATIONID.toString(), LoggerFileConstant.UIN.name(), " ",
//					"NotificationService::sendEmailNotification()::emailValidation::" + "false :: invalid email");
			return false;
		}
		
		String mergedEmailSubject = "subject";
		String mergedTemplate = "content";
//		for (String language : templateLangauges) {
//			String emailSubject = "";
//			String languageTemplate = "";
//			if(notificationTemplate==null) {
//				if(newEmail==null) {
//					emailSubject = templateMerge(getTemplate(language, templateUtil.getEmailSubjectTemplateTypeCode(requestType, templateType)),
//							requestType.getNotificationTemplateVariables(templateUtil, new NotificationTemplateVariableDTO(eventId, requestType, templateType, language)));
//
//					languageTemplate = templateMerge(getTemplate(language, templateUtil.getEmailContentTemplateTypeCode(requestType, templateType)),
//							requestType.getNotificationTemplateVariables(templateUtil, new NotificationTemplateVariableDTO(eventId, requestType, templateType, language)));
//				}
//				else {
//					emailSubject = templateMerge(getTemplate(language, templateUtil.getEmailSubjectTemplateTypeCode(requestType, templateType)),
//							requestType.getNotificationTemplateVariables(templateUtil, new NotificationTemplateVariableDTO(eventId, requestType, templateType, language, otp)));
//
//					languageTemplate = templateMerge(getTemplate(language, templateUtil.getEmailContentTemplateTypeCode(requestType, templateType)),
//							requestType.getNotificationTemplateVariables(templateUtil, new NotificationTemplateVariableDTO(eventId, requestType, templateType, language, otp)));
//				}
//			} else {
//				emailSubject = getTemplate(language, notificationTemplate + EMAIL + SUBJECT);
//				languageTemplate = templateMerge(getTemplate(language, notificationTemplate + EMAIL),
//						mailingAttributes);
//			}
//			if(languageTemplate.trim().endsWith(LINE_BREAK)) {
//				languageTemplate = languageTemplate.substring(0, languageTemplate.length() - LINE_BREAK.length()).trim();
//			}
//			if (mergedTemplate.isBlank() || mergedEmailSubject.isBlank()) {
//				mergedTemplate = languageTemplate;
//				mergedEmailSubject = emailSubject;
//			} else {
//				mergedTemplate = mergedTemplate + LINE_SEPARATOR + languageTemplate;
//				mergedEmailSubject = mergedEmailSubject + SEPARATOR + emailSubject;
//			}
//		}
		
		LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
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
			params.add("attachments", null);
			ResponseWrapper<NotificationResponseDTO> response;

			RestRequestDTO restRequest = new RestRequestDTO();
    		restRequest.setUri(env.getProperty("EMAILNOTIFIER"));
    		restRequest.setHttpMethod(HttpMethod.POST);
    		restRequest.setResponseType(ResponseWrapper.class);
    		restRequest.setTimeout(1000);
    		restRequest.setParams(params);
    		HttpHeaders headers = new HttpHeaders();
    		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    		headers.add("Authorization", "futureproof");
    		restRequest.setHeaders(headers);
			response = restHelper.requestSync(restRequest);
			
//			response = restClient.postApi(env.getProperty(ApiName.EMAILNOTIFIER.name()), MediaType.MULTIPART_FORM_DATA, params,
//					ResponseWrapper.class);
			if (response == null || response.getResponse() == null || response.getErrors() != null && !response.getErrors().isEmpty()) {
//				throw new ResidentServiceException(ResidentErrorCode.INVALID_API_RESPONSE.getErrorCode(),
//						ResidentErrorCode.INVALID_API_RESPONSE.getErrorMessage() + " EMAILNOTIFIER API"
//								+ (response != null ? response.getErrors().get(0) : ""));
			}
			NotificationResponseDTO notifierResponse = JsonUtil
					.readValue(JsonUtil.writeValueAsString(response.getResponse()), NotificationResponseDTO.class);

			if ("success".equals(notifierResponse.getStatus())) {
//				logger.debug(LoggerFileConstant.APPLICATIONID.toString(), LoggerFileConstant.UIN.name(), " ",
//						"NotificationService::sendEmailNotification()::exit");
				return true;
			}
		} catch (Exception e) {
//			if (e.getCause() instanceof HttpClientErrorException) {
//				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
//				throw new ResidentServiceCheckedException(
//						ResidentErrorCode.API_RESOURCE_ACCESS_EXCEPTION.getErrorCode(),
//						httpClientException.getResponseBodyAsString());
//
//			} else if (e.getCause() instanceof HttpServerErrorException) {
//				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
//				throw new ResidentServiceCheckedException(
//						ResidentErrorCode.API_RESOURCE_ACCESS_EXCEPTION.getErrorCode(),
//						httpServerException.getResponseBodyAsString());
//			} else {
//				throw new ResidentServiceCheckedException(
//						ResidentErrorCode.API_RESOURCE_ACCESS_EXCEPTION.getErrorCode(),
//						ResidentErrorCode.API_RESOURCE_ACCESS_EXCEPTION.getErrorMessage() + e.getMessage(), e);
//			}
		}
//		} catch (IOException e) {
//			audit.setAuditRequestDto(EventEnum.TOKEN_GENERATION_FAILED);
//			throw new ResidentServiceCheckedException(ResidentErrorCode.TOKEN_GENERATION_FAILED.getErrorCode(),
//					ResidentErrorCode.TOKEN_GENERATION_FAILED.getErrorMessage(), e);
//		}
		
		return false;

	}
	
}
