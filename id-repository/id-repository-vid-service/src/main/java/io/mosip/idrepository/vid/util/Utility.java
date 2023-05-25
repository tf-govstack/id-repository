package io.mosip.idrepository.vid.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.assertj.core.util.Lists;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.vid.dto.JsonValue;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.util.StringUtils;

@Component
public class Utility {
	
	@Autowired
	RestHelper restHelper;
	
	@Autowired
	private Environment env;
	
	private static String regProcessorIdentityJson = "";
	
	@Value("${config.server.file.storage.uri}")
	private String configServerFileStorageURL;

    @Value("${registration.processor.identityjson}")
	private String identityJson;
	
	@PostConstruct
    private void loadRegProcessorIdentityJson() {
        try {
        	RestRequestDTO restRequest = new RestRequestDTO();
    		restRequest.setUri(configServerFileStorageURL + identityJson);
    		restRequest.setHttpMethod(HttpMethod.GET);
    		restRequest.setResponseType(String.class);
    		restRequest.setTimeout(1000);
			regProcessorIdentityJson = restHelper.requestSync(restRequest);
		} catch (RestServiceException e) {
			
		}
    }

	public String getMappingJson() {
        if (StringUtils.isBlank(regProcessorIdentityJson)) {
        	try {
            	RestRequestDTO restRequest = new RestRequestDTO();
        		restRequest.setUri(configServerFileStorageURL + identityJson);
        		restRequest.setHttpMethod(HttpMethod.GET);
        		restRequest.setResponseType(String.class);
        		restRequest.setTimeout(1000);
    			regProcessorIdentityJson = restHelper.requestSync(restRequest);
    		} catch (RestServiceException e) {
    			
    		}
        }
        return regProcessorIdentityJson;
    }
	
	@SuppressWarnings("unchecked")
	public Map<String, Object> getMailingAttributes(String id, Set<String> templateLangauges) {
		Map<String, Object> attributes = new HashMap<>();
		String mappingJsonString = getMappingJson();
		if(mappingJsonString==null || mappingJsonString.trim().isEmpty()) {
			//throw;
		}
		JSONObject mappingJsonObject;
		try {
			JSONObject demographicIdentity = retrieveIdrepoJson(id);
			mappingJsonObject = JsonUtil.readValue(mappingJsonString, JSONObject.class);
			JSONObject mapperIdentity = JsonUtil.getJSONObject(mappingJsonObject, "identity");
			List<String> mapperJsonKeys = new ArrayList<>(mapperIdentity.keySet());

			List<String> defaultTemplateLanguages = getDefaultTemplateLanguages();
			templateLangauges.addAll(defaultTemplateLanguages);

			for (String key : mapperJsonKeys) {
				LinkedHashMap<String, String> jsonObject = JsonUtil.getJSONValue(mapperIdentity, key);
				String values = jsonObject.get("value");
				for (String value : values.split(",")) {
					Object object = demographicIdentity.get(value);
					if (object instanceof ArrayList) {
						JSONArray node = JsonUtil.getJSONArray(demographicIdentity, value);
						JsonValue[] jsonValues = JsonUtil.mapJsonNodeToJavaObject(JsonValue.class, node);
						for (JsonValue jsonValue : jsonValues) {
							if (templateLangauges.contains(jsonValue.getLanguage()))
								attributes.put(value + "_" + jsonValue.getLanguage(), jsonValue.getValue());
						}
					} else if (object instanceof LinkedHashMap) {
						JSONObject json = JsonUtil.getJSONObject(demographicIdentity, value);
						attributes.put(value, (String) json.get("value"));
					} else {
						attributes.put(value, String.valueOf(object));
					}
				}
			}
		} catch (Exception e) {
//			throw new ResidentServiceCheckedException(ResidentErrorCode.RESIDENT_SYS_EXCEPTION.getErrorCode(),
//					ResidentErrorCode.RESIDENT_SYS_EXCEPTION.getErrorMessage(), e);
		}
		
		return attributes;
	}
	
	private List<String> getDefaultTemplateLanguages() {
		String defaultLanguages = env.getProperty("mosip.default.template-languages");
		List<String> strList = Collections.emptyList() ;
		if (defaultLanguages !=null && !StringUtils.isBlank(defaultLanguages)) {
			String[] lanaguages = defaultLanguages.split(",");
			if(lanaguages!=null && lanaguages.length >0 ) {
				 strList = Lists.newArrayList(lanaguages);
			}
			return strList;
		}
		return strList;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject retrieveIdrepoJson(String id) {
		IdResponseDTO response = null;
		try {
			RestRequestDTO restRequest = new RestRequestDTO();
			String uri = env.getProperty("IDREPOGETIDBYID");
    		restRequest.setUri(uri + "/" + id);
    		restRequest.setHttpMethod(HttpMethod.GET);
    		restRequest.setResponseType(IdResponseDTO.class);
    		restRequest.setTimeout(1000);
			response = (IdResponseDTO) restHelper.requestSync(restRequest);
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
		
		return retrieveErrorCode(response, id);
	}
	
	public JSONObject retrieveErrorCode(IdResponseDTO response, String id) {
//		ResidentErrorCode errorCode;
//		errorCode = ResidentErrorCode.INVALID_ID;
		try {
			if (response == null)
//				throw new IdRepoAppException(errorCode.getErrorCode(), errorCode.getErrorMessage(),
//						"In valid response while requesting ID Repositary");
			if (!response.getErrors().isEmpty()) {
//				List<ServiceError> error = response.getErrors();
//				throw new IdRepoAppException(errorCode.getErrorCode(), errorCode.getErrorMessage(),
//						error.get(0).getMessage());
			}

			String jsonResponse = JsonUtil.writeValueAsString(response.getResponse());
			JSONObject json = JsonUtil.readValue(jsonResponse, JSONObject.class);
			return JsonUtil.getJSONObject(json, "identity");
		} catch (IOException e) {
//			throw new ResidentServiceCheckedException(ResidentErrorCode.RESIDENT_SYS_EXCEPTION.getErrorCode(),
//					ResidentErrorCode.RESIDENT_SYS_EXCEPTION.getErrorMessage(), e);
		}
		return null;
	}
	
}
