package io.mosip.idrepository.vid.util;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.assertj.core.util.Lists;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.util.IOUtils;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.vid.dto.JsonValue;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;

@Component
public class Utility {

	private static final Logger mosipLogger = IdRepoLogger.getLogger(Utility.class);

	private static final String CLASS_UTILITY = "Utility";
	private static final String METHOD_LOAD_IDENTITY_JSON = "loadRegProcessorIdentityJson";
	private static final String METHOD_GET_MAILING_ATTRIBUTES = "loadRegProcessorIdentityJson";
	private static final String METHOD_IDREPO_JSON = "retrieveIdrepoJson";
	private static final String METHOD_REGISTRATION_PROCESSOR_MAPPING = "getRegistrationProcessorMappingJson";

	private static final String IDENTITY = "identity";
	private static final String VALUE = "value";
	private static final String IDREPOGETIDBYID = "IDREPOGETIDBYID";
	private static final String NAME = "name";
	private static final String COMMA = ",";
	private static final String EMAIL = "email";
	private static final String PHONE = "phone";

	@Autowired
	RestHelper restHelper;

	@Autowired
	@Qualifier("selfTokenRestTemplate")
	private RestTemplate restTemplate;

	@Autowired(required = true)
	@Qualifier("varres")
	private VariableResolverFactory functionFactory;

	@Autowired
	private Environment env;

	@Autowired
	private ObjectMapper objMapper;

	private static String regProcessorIdentityJson = "";

	@Value("${config.server.file.storage.uri}")
	private String configServerFileStorageURL;

	@Value("${registration.processor.identityjson}")
	private String identityJson;

	@Value("${vid.email.mask.function}")
	private String emailMaskFunction;

	@Value("${vid.phone.mask.function}")
	private String phoneMaskFunction;

	@Value("${vid.data.mask.function}")
	private String maskingFunction;

	@PostConstruct
	private void loadRegProcessorIdentityJson() {
		regProcessorIdentityJson = restTemplate.getForObject(configServerFileStorageURL + identityJson, String.class);
		mosipLogger.info(IdRepoSecurityManager.getUser(), CLASS_UTILITY, METHOD_LOAD_IDENTITY_JSON,
				"loadRegProcessorIdentityJson completed successfully");
	}

	public String getMappingJson() {
		if (StringUtils.isBlank(regProcessorIdentityJson)) {
			return restTemplate.getForObject(configServerFileStorageURL + identityJson, String.class);
		}
		return regProcessorIdentityJson;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getMailingAttributes(String id, Set<String> templateLangauges)
			throws IdRepoAppException {
		mosipLogger.debug(IdRepoSecurityManager.getUser(), CLASS_UTILITY, METHOD_GET_MAILING_ATTRIBUTES,
				"Utility::getMailingAttributes()::entry");
		if (id == null || id.isEmpty()) {
			throw new IdRepoAppException(IdRepoErrorConstants.UNABLE_TO_PROCESS.getErrorCode(),
					IdRepoErrorConstants.UNABLE_TO_PROCESS.getErrorMessage() + ": individual_id is not available.");
		}

		Map<String, Object> attributes = new HashMap<>();
		String mappingJsonString = getMappingJson();
		if (mappingJsonString == null || mappingJsonString.trim().isEmpty()) {
			throw new IdRepoAppException(IdRepoErrorConstants.JSON_PROCESSING_EXCEPTION.getErrorCode(),
					IdRepoErrorConstants.JSON_PROCESSING_EXCEPTION.getErrorMessage());
		}
		JSONObject mappingJsonObject;
		try {
			JSONObject demographicIdentity = retrieveIdrepoJson(id);
			mappingJsonObject = JsonUtil.readValue(mappingJsonString, JSONObject.class);
			JSONObject mapperIdentity = JsonUtil.getJSONObject(mappingJsonObject, IDENTITY);
			List<String> mapperJsonKeys = new ArrayList<>(mapperIdentity.keySet());

			Set<String> preferredLanguage = getPreferredLanguage(demographicIdentity);
			if (preferredLanguage.isEmpty()) {
				List<String> defaultTemplateLanguages = getDefaultTemplateLanguages();
				if (CollectionUtils.isEmpty(defaultTemplateLanguages)) {
					Set<String> dataCapturedLanguages = getDataCapturedLanguages(mapperIdentity, demographicIdentity);
					templateLangauges.addAll(dataCapturedLanguages);
				} else {
					templateLangauges.addAll(defaultTemplateLanguages);
				}
			} else {
				templateLangauges.addAll(preferredLanguage);
			}

			for (String key : mapperJsonKeys) {
				LinkedHashMap<String, String> jsonObject = JsonUtil.getJSONValue(mapperIdentity, key);
				String values = jsonObject.get(VALUE);
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
						attributes.put(value, (String) json.get(VALUE));
					} else {
						attributes.put(value, String.valueOf(object));
					}
				}
			}
		} catch (IOException | ReflectiveOperationException e) {
			throw new IdRepoAppException(IdRepoErrorConstants.VID_SYS_EXCEPTION.getErrorCode(),
					IdRepoErrorConstants.VID_SYS_EXCEPTION.getErrorMessage(), e);
		}

		mosipLogger.debug(IdRepoSecurityManager.getUser(), CLASS_UTILITY, METHOD_GET_MAILING_ATTRIBUTES,
				"Utility::getMailingAttributes()::exit");

		return attributes;
	}

	private Set<String> getPreferredLanguage(JSONObject demographicIdentity) {
		String preferredLang = null;
		String preferredLangAttribute = env.getProperty("mosip.default.user-preferred-language-attribute");
		if (!StringUtils.isBlank(preferredLangAttribute)) {
			Object object = demographicIdentity.get(preferredLangAttribute);
			if (object != null) {
				preferredLang = String.valueOf(object);
				if (preferredLang.contains(COMMA)) {
					String[] preferredLangArray = preferredLang.split(COMMA);
					return Set.of(preferredLangArray);
				}
			}
		}
		if (preferredLang != null) {
			return Set.of(preferredLang);
		}
		return Set.of();
	}

	private Set<String> getDataCapturedLanguages(JSONObject mapperIdentity, JSONObject demographicIdentity)
			throws ReflectiveOperationException {
		Set<String> dataCapturedLangauges = new HashSet<String>();
		LinkedHashMap<String, String> jsonObject = JsonUtil.getJSONValue(mapperIdentity, NAME);
		String values = jsonObject.get(VALUE);
		for (String value : values.split(",")) {
			Object object = demographicIdentity.get(value);
			if (object instanceof ArrayList) {
				JSONArray node = JsonUtil.getJSONArray(demographicIdentity, value);
				JsonValue[] jsonValues = JsonUtil.mapJsonNodeToJavaObject(JsonValue.class, node);
				for (JsonValue jsonValue : jsonValues) {
					dataCapturedLangauges.add(jsonValue.getLanguage());
				}
			}
		}
		return dataCapturedLangauges;
	}

	private List<String> getDefaultTemplateLanguages() {
		String defaultLanguages = env.getProperty("mosip.default.template-languages");
		List<String> strList = Collections.emptyList();
		if (defaultLanguages != null && !StringUtils.isBlank(defaultLanguages)) {
			String[] lanaguages = defaultLanguages.split(",");
			if (lanaguages != null && lanaguages.length > 0) {
				strList = Lists.newArrayList(lanaguages);
			}
			return strList;
		}
		return strList;
	}

	@SuppressWarnings("unchecked")
	public JSONObject retrieveIdrepoJson(String id) throws IdRepoAppException {
		mosipLogger.debug(IdRepoSecurityManager.getUser(), CLASS_UTILITY, METHOD_IDREPO_JSON,
				"Utility::retrieveIdrepoJson()::entry");
		List<String> pathsegments = new ArrayList<>();
		pathsegments.add(id);
		IdResponseDTO response = null;
		try {
			response = (IdResponseDTO) restHelper.getApi(env.getProperty(IDREPOGETIDBYID), pathsegments, "", null,
					IdResponseDTO.class);
		} catch (Exception e) {
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				throw new IdRepoAppException(IdRepoErrorConstants.API_RESOURCE_ACCESS_EXCEPTION.getErrorCode(),
						httpClientException.getResponseBodyAsString());

			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				throw new IdRepoAppException(IdRepoErrorConstants.API_RESOURCE_ACCESS_EXCEPTION.getErrorCode(),
						httpServerException.getResponseBodyAsString());
			} else {
				throw new IdRepoAppException(IdRepoErrorConstants.API_RESOURCE_ACCESS_EXCEPTION.getErrorCode(),
						IdRepoErrorConstants.API_RESOURCE_ACCESS_EXCEPTION.getErrorMessage() + e.getMessage(), e);
			}
		}

		return retrieveErrorCode(response, id);
	}

	public JSONObject retrieveErrorCode(IdResponseDTO response, String id) throws IdRepoAppException {
		IdRepoErrorConstants errorCode = IdRepoErrorConstants.INVALID_ID;
		try {
			if (response == null)
				throw new IdRepoAppException(errorCode.getErrorCode(), errorCode.getErrorMessage(),
						"In valid response while requesting ID Repositary");
			if (!response.getErrors().isEmpty()) {
				List<ServiceError> error = response.getErrors();
				throw new IdRepoAppException(errorCode.getErrorCode(), errorCode.getErrorMessage(),
						error.get(0).getMessage());
			}

			String jsonResponse = JsonUtil.writeValueAsString(response.getResponse());
			JSONObject json = JsonUtil.readValue(jsonResponse, JSONObject.class);
			mosipLogger.debug(IdRepoSecurityManager.getUser(), CLASS_UTILITY, METHOD_IDREPO_JSON,
					"Utility::retrieveIdrepoJson()::exit");
			return JsonUtil.getJSONObject(json, IDENTITY);
		} catch (IOException e) {
			throw new IdRepoAppException(IdRepoErrorConstants.VID_SYS_EXCEPTION.getErrorCode(),
					IdRepoErrorConstants.VID_SYS_EXCEPTION.getErrorMessage(), e);
		}
	}

	public String maskData(Object object, String maskingFunctionName) {
		Map context = new HashMap();
		context.put("value", String.valueOf(object));
		VariableResolverFactory myVarFactory = new MapVariableResolverFactory(context);
		myVarFactory.setNextFactory(functionFactory);
		Serializable serializable = MVEL.compileExpression(maskingFunctionName + "(value);");
		String formattedObject = MVEL.executeExpression(serializable, context, myVarFactory, String.class);
		return formattedObject;
	}

	public String maskEmail(String email) {
		return maskData(email, emailMaskFunction);
	}

	public String maskPhone(String phone) {
		return maskData(phone, phoneMaskFunction);
	}

	public String convertToMaskDataFormat(String maskData) {
		return maskData(maskData, maskingFunction);
	}

	public static String readResourceContent(Resource resFile) {
		try {
			return IOUtils.readInputStreamToString(resFile.getInputStream(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			mosipLogger.error(e.getMessage());
			return null;
		}
	}

	public String getPhoneAttribute() throws IdRepoAppException {
		return getIdMappingAttributeForKey(PHONE);
	}

	public String getEmailAttribute() throws IdRepoAppException {
		return getIdMappingAttributeForKey(EMAIL);
	}

	private String getIdMappingAttributeForKey(String attributeKey) throws IdRepoAppException {
		try {
			JSONObject regProcessorIdentityJson = getRegistrationProcessorMappingJson();
			String phoneAttribute = JsonUtil
					.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, attributeKey), VALUE);
			return phoneAttribute;
		} catch (IOException e) {
			throw new IdRepoAppException(IdRepoErrorConstants.IO_EXCEPTION.getErrorCode(),
					IdRepoErrorConstants.IO_EXCEPTION.getErrorMessage(), e);
		}
	}

	public JSONObject getRegistrationProcessorMappingJson() throws IOException {
		mosipLogger.debug(IdRepoSecurityManager.getUser(), CLASS_UTILITY, METHOD_REGISTRATION_PROCESSOR_MAPPING,
				"Utility::getRegistrationProcessorMappingJson()::entry");

		String mappingJsonString = getMappingJson();
		mosipLogger.debug(IdRepoSecurityManager.getUser(), CLASS_UTILITY, METHOD_REGISTRATION_PROCESSOR_MAPPING,
				"Utility::getRegistrationProcessorMappingJson()::exit");
		return JsonUtil.getJSONObject(objMapper.readValue(mappingJsonString, JSONObject.class), IDENTITY);

	}

}
