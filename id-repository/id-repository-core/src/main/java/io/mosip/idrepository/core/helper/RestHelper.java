package io.mosip.idrepository.core.helper;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.CLIENT_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.CONNECTION_TIMED_OUT;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.SERVER_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.UNKNOWN_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.API_RESOURCE_ACCESS_EXCEPTION;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.exception.AuthenticationException;
import io.mosip.idrepository.core.exception.IdRepoRetryException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.RestUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.retry.WithRetry;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * The Class RestHelper - to send/receive HTTP requests and return the response.
 *
 * @author Manoj SP
 */
@NoArgsConstructor
public class RestHelper {

	private static final String CHECK_ERROR_RESPONSE = "checkErrorResponse";

	private static final String UNKNOWN_ERROR_LOG = "- UNKNOWN_ERROR - ";

	/** The Constant ERRORS. */
	private static final String ERRORS = "errors";

	/** The mapper. */
	@Autowired
	private ObjectMapper mapper;
	
	private RestTemplate restTemplate;
	
	@Autowired
	private ApplicationContext ctx;

	/** The Constant METHOD_REQUEST_SYNC. */
	private static final String METHOD_REQUEST_SYNC = "requestSync";

	/** The Constant METHOD_HANDLE_STATUS_ERROR. */
	private static final String METHOD_HANDLE_STATUS_ERROR = "handleStatusError";

	/** The Constant PREFIX_REQUEST. */
	private static final String PREFIX_REQUEST = "Request : ";

	/** The Constant METHOD_REQUEST_ASYNC. */
	private static final String METHOD_REQUEST_ASYNC = "requestAsync";
	
	/** The Constant METHOD_POST_API. */
	private static final String METHOD_POST_API = "postApi";
	
	/** The Constant METHOD_GET_API. */
	private static final String METHOD_GET_API = "getApi";

	/** The Constant CLASS_REST_HELPER. */
	private static final String CLASS_REST_HELPER = "RestHelper";

	/** The Constant THROWING_REST_SERVICE_EXCEPTION. */
	private static final String THROWING_REST_SERVICE_EXCEPTION = "Throwing RestServiceException";

	/** The Constant REQUEST_SYNC_RUNTIME_EXCEPTION. */
	private static final String REQUEST_SYNC_RUNTIME_EXCEPTION = "requestSync-RuntimeException";

	/** The mosipLogger. */
	private static Logger mosipLogger = IdRepoLogger.getLogger(RestHelper.class);
	
	private WebClient webClient;
	
	public RestHelper(WebClient webClient, RestTemplate restTemplate) {
		this.webClient = webClient;
		this.restTemplate = restTemplate;
	}
	
	@PostConstruct
	public void init() {
		if (Objects.isNull(webClient))
			webClient = ctx.getBean("webClient", WebClient.class);
	}
	
	/**
	 * Request to send/receive HTTP requests and return the response synchronously.
	 *
	 * @param         <T> the generic type
	 * @param request the request
	 * @return the response object or null in case of exception
	 * @throws RestServiceException the rest service exception
	 */
	@SuppressWarnings("unchecked")
	@WithRetry
	public <T> T requestSync(@Valid RestRequestDTO request) throws RestServiceException {
		Object response;
		try {
			mosipLogger.debug(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, METHOD_REQUEST_SYNC,
					request.getUri());
			if (request.getTimeout() != null) {
				response = request(request).timeout(Duration.ofSeconds(request.getTimeout())).block();
			} else {
				response = request(request).block();
			}
			if(!String.class.equals(request.getResponseType())) {
				checkErrorResponse(response, request.getResponseType());
				if(RestUtil.containsError(response.toString(), mapper)) {
					mosipLogger.debug("Error in response %s", response.toString());
				}
			}	
			mosipLogger.debug(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, METHOD_REQUEST_SYNC,
					"Received valid response");
			return (T) response;
		} catch (WebClientResponseException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, METHOD_REQUEST_SYNC,
					THROWING_REST_SERVICE_EXCEPTION + "- Http Status error - \n " + e.getMessage()
							+ " \n Response Body : \n" + e.getResponseBodyAsString());
			throw handleStatusError(e, request.getResponseType());
		} catch (RuntimeException e) {
			if (e.getCause() != null && e.getCause().getClass().equals(TimeoutException.class)) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, METHOD_REQUEST_SYNC,
						THROWING_REST_SERVICE_EXCEPTION + "- CONNECTION_TIMED_OUT - \n " + ExceptionUtils.getStackTrace(e));
				throw new IdRepoRetryException(new RestServiceException(CONNECTION_TIMED_OUT, e));
			} else {
				mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, REQUEST_SYNC_RUNTIME_EXCEPTION,
						THROWING_REST_SERVICE_EXCEPTION + UNKNOWN_ERROR_LOG + ExceptionUtils.getStackTrace(e));
				throw new IdRepoRetryException(new RestServiceException(UNKNOWN_ERROR, e));
			}
		}
	}

	/**
	 * Request to send/receive HTTP requests and return the response asynchronously.
	 *
	 * @param request the request
	 * @return the supplier
	 * @throws RestServiceException 
	 */
	@Async
	public CompletableFuture<Object> requestAsync(@Valid RestRequestDTO request) {
		mosipLogger.debug(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, METHOD_REQUEST_ASYNC,
				PREFIX_REQUEST + request.getUri());
		try {
			Object obj =  requestSync(request);
			return CompletableFuture.completedFuture(obj);
		} catch (RestServiceException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, METHOD_REQUEST_ASYNC,
					ExceptionUtils.getStackTrace(e));
			return CompletableFuture.failedFuture(e);
		}
	}

	/**
	 * Method to send/receive HTTP requests and return the response as Mono.
	 *
	 * @param request    the request
	 * @param sslContext the ssl context
	 * @return the mono
	 */
	private Mono<?> request(RestRequestDTO request) {
		Mono<?> monoResponse;
		RequestBodySpec requestBodySpec;
		ResponseSpec exchange;
		
		if (request.getParams() != null && request.getPathVariables() == null) {
			request.setUri(UriComponentsBuilder
					.fromUriString(request.getUri())
					.queryParams(request.getParams())
					.toUriString());
		} else if (request.getParams() == null && request.getPathVariables() != null) {
			request.setUri(UriComponentsBuilder
					.fromUriString(request.getUri())
					.buildAndExpand(request.getPathVariables())
					.toUriString());
		} else if (request.getParams() != null && request.getPathVariables() != null) {
			request.setUri(UriComponentsBuilder
					.fromUriString(request.getUri())
					.queryParams(request.getParams())
					.buildAndExpand(request.getPathVariables())
					.toUriString());
		}
		
		requestBodySpec = webClient.method(request.getHttpMethod()).uri(request.getUri());

		if (request.getHeaders() != null) {
			requestBodySpec = requestBodySpec
					.headers(headers -> headers.addAll(request.getHeaders()));
		}

		if (request.getRequestBody() != null) {
			exchange = requestBodySpec.syncBody(request.getRequestBody()).retrieve();
		} else {
			exchange = requestBodySpec.retrieve();
		}

		monoResponse = exchange.bodyToMono(request.getResponseType());

		return monoResponse;
	}

	/**
	 * Check error response.
	 *
	 * @param response     the response
	 * @param responseType the response type
	 * @throws RestServiceException the rest service exception
	 */
	private void checkErrorResponse(Object response, Class<?> responseType) throws RestServiceException {
		try {
			if (Objects.nonNull(response)) {
				ObjectNode responseNode = mapper.readValue(mapper.writeValueAsBytes(response), ObjectNode.class);
				if (responseNode.has(ERRORS) && !responseNode.get(ERRORS).isNull() && responseNode.get(ERRORS).isArray()
						&& responseNode.get(ERRORS).size() > 0) {
					mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, CHECK_ERROR_RESPONSE,
							THROWING_REST_SERVICE_EXCEPTION + UNKNOWN_ERROR_LOG
									+ responseNode.get(ERRORS).toString());
					throw new RestServiceException(CLIENT_ERROR, responseNode.toString(),
							mapper.readValue(responseNode.toString().getBytes(), responseType));
				}
			} else {
				mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, CHECK_ERROR_RESPONSE,
						THROWING_REST_SERVICE_EXCEPTION + UNKNOWN_ERROR_LOG + "Response is null");
				throw new RestServiceException(CLIENT_ERROR);
			}
		} catch (IOException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, CHECK_ERROR_RESPONSE,
					THROWING_REST_SERVICE_EXCEPTION + UNKNOWN_ERROR_LOG + e.getMessage());
			throw new RestServiceException(UNKNOWN_ERROR, e);
		}
	}

	/**
	 * Handle 4XX/5XX status error. Retry is triggered using {@code IdRepoRetryException}.
	 * Retry is done for 401 and 5xx status codes.
	 *
	 * @param e            the response
	 * @param responseType the response type
	 * @return the mono<? extends throwable>
	 * @throws RestServiceException 
	 */
	private RestServiceException handleStatusError(WebClientResponseException e, Class<?> responseType)
			throws RestServiceException {
		try {
			mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER,
					"request failed with status code :" + e.getRawStatusCode(), "\n\n" + e.getResponseBodyAsString());
			if (e.getStatusCode().is4xxClientError()) {
				if (e.getRawStatusCode() == HttpStatus.UNAUTHORIZED.value()) {
					List<ServiceError> errorList = ExceptionUtils.getServiceErrorList(e.getResponseBodyAsString());
					throw new AuthenticationException(errorList.get(0).getErrorCode(), errorList.get(0).getMessage(),
							e.getRawStatusCode());
				} else if (e.getRawStatusCode() == HttpStatus.FORBIDDEN.value()) {
					List<ServiceError> errorList = ExceptionUtils.getServiceErrorList(e.getResponseBodyAsString());
					throw new IdRepoRetryException(new AuthenticationException(errorList.get(0).getErrorCode(),
							errorList.get(0).getMessage(), e.getRawStatusCode()));
				} else {
					mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, METHOD_HANDLE_STATUS_ERROR,
							"Status error - returning RestServiceException - CLIENT_ERROR ");
					throw new RestServiceException(CLIENT_ERROR, e.getResponseBodyAsString(),
							mapper.readValue(e.getResponseBodyAsString().getBytes(), responseType));
				}
			} else {
				mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, METHOD_HANDLE_STATUS_ERROR,
						"Status error - returning RestServiceException - SERVER_ERROR");
				throw new IdRepoRetryException(new RestServiceException(SERVER_ERROR, e.getResponseBodyAsString(),
						mapper.readValue(e.getResponseBodyAsString().getBytes(), responseType)));
			}
		} catch (IOException ex) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, METHOD_HANDLE_STATUS_ERROR,
					ex.getMessage());
			return new RestServiceException(UNKNOWN_ERROR, ex);
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> T postApi(String uri, MediaType mediaType, Object requestType, Class<?> responseClass)
			throws RestServiceException {
		try {
			mosipLogger.debug(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, METHOD_POST_API,
					uri);
			T response = (T) restTemplate.postForObject(uri, setRequestHeader(requestType, mediaType),
					responseClass);
			return response;

		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, METHOD_POST_API,
					e.getMessage() + ExceptionUtils.getStackTrace(e));

			throw new RestServiceException(API_RESOURCE_ACCESS_EXCEPTION, e);
		}
	}
	
	/**
	 * this method sets token to header of the request
	 *
	 * @param requestType
	 * @param mediaType
	 * @return HttpEntity<Object>
	 */
	@SuppressWarnings("unchecked")
	private HttpEntity<Object> setRequestHeader(Object requestType, MediaType mediaType) {
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
		//headers.add("Cookie", "Authorization=eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJNVFJ4YmVSUWJWOHFCTlYtY2pjVzNJSmpVdmppbldNdVdPbTN6VGdYVjZvIn0.eyJleHAiOjE2ODU2NTU2MjIsImlhdCI6MTY4NTYxOTYyMiwianRpIjoiM2JlMDBiMDItYTE1OC00ZmVmLThjZDQtZTlhMzUyZjgwOWY2IiwiaXNzIjoiaHR0cHM6Ly9pYW0udGYxLmlkZW5jb2RlLmxpbmsvYXV0aC9yZWFsbXMvbW9zaXAiLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiNjFiMTY3NDItNmIxYi00MjE3LWIzNzAtOGRjNWY4NzMxNjU2IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoibW9zaXAtcmVzaWRlbnQtY2xpZW50IiwiYWNyIjoiMSIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJDUkVERU5USUFMX1JFUVVFU1QiLCJSRVNJREVOVCIsIm9mZmxpbmVfYWNjZXNzIiwiUEFSVE5FUl9BRE1JTiIsInVtYV9hdXRob3JpemF0aW9uIiwiZGVmYXVsdC1yb2xlcy1tb3NpcCJdfSwicmVzb3VyY2VfYWNjZXNzIjp7Im1vc2lwLXJlc2lkZW50LWNsaWVudCI6eyJyb2xlcyI6WyJ1bWFfcHJvdGVjdGlvbiJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJpbmRpdmlkdWFsX2lkIGlkYV90b2tlbiBlbWFpbCBwcm9maWxlIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJjbGllbnRIb3N0IjoiMTAuNDIuNS4xNjciLCJjbGllbnRJZCI6Im1vc2lwLXJlc2lkZW50LWNsaWVudCIsInByZWZlcnJlZF91c2VybmFtZSI6InNlcnZpY2UtYWNjb3VudC1tb3NpcC1yZXNpZGVudC1jbGllbnQiLCJjbGllbnRBZGRyZXNzIjoiMTAuNDIuNS4xNjcifQ.Udf6LsOKppUu4UCYYrKbtay2acnhyFS3gj0ItzIa7_wZ7QvFRAk7UM4hAdOk3MKOXhw2uPFnCuSKPH0_8tuettJgTqN034JjL83SN2TzHgtMk7CWDTaLcJbPy9nbUxwmhMsnaYwjDD5KyD1bHgEIdud9Ndpox7Z9uA6O7A7FxbKONjN5A0h19SQsEvgqPKUdaJJxPuBTzLKyJhf9SOOlPfWt3nDnxttPRmO-x8Mc_WIvwH8PN8ujdQf99JlbxBHwFVSgWHMuMBShdni8ZO6sY7ZArzc8L8tqfBJXPxNeJXQcfxWRa2qg-SQMIwKgcEGOrCXLMMCz_sbEq4qcY4XRQQ");
		headers.add("Authorization", "futureProof");
		if (mediaType != null) {
			headers.add("Content-Type", mediaType.toString());
		}
		if (requestType != null) {
			try {
				HttpEntity<Object> httpEntity = (HttpEntity<Object>) requestType;
				HttpHeaders httpHeader = httpEntity.getHeaders();
				for (String key : httpHeader.keySet()) {
					if (!(headers.containsKey("Content-Type") && Objects.equals(key, "Content-Type"))){	
							List<String> headerKeys = httpHeader.get(key);
							if(headerKeys != null && !headerKeys.isEmpty()){
								headers.add(key,headerKeys.get(0));
							}
					}
				}
				return new HttpEntity<>(httpEntity.getBody(), headers);
			} catch (ClassCastException e) {
				return new HttpEntity<>(requestType, headers);
			}
		} else
			return new HttpEntity<>(headers);
	}
	
	public Object getApi(String uri, List<String> pathsegments, String queryParamName, String queryParamValue,
			Class<?> responseType) throws RestServiceException {

		Object obj = null;
		UriComponentsBuilder builder = null;
		UriComponents uriComponents = null;
		if (uri != null) {
			builder = UriComponentsBuilder.fromUriString(uri);
			if (!((pathsegments == null) || (pathsegments.isEmpty()))) {
				for (String segment : pathsegments) {
					if (!((segment == null) || (("").equals(segment)))) {
						builder.pathSegment(segment);
					}
				}
			}

			if (StringUtils.isNotEmpty(queryParamName)) {

				String[] queryParamNameArr = queryParamName.split(",");
				String[] queryParamValueArr = queryParamValue.split(",");
				for (int i = 0; i < queryParamNameArr.length; i++) {
					builder.queryParam(queryParamNameArr[i], queryParamValueArr[i]);
				}

			}
			try {

				uriComponents = builder.build(false).encode();
				obj = getApi(uriComponents.toUri(), responseType, null);

			} catch (Exception e) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, METHOD_GET_API,
						e.getMessage() + ExceptionUtils.getStackTrace(e));
				
				throw new RestServiceException(API_RESOURCE_ACCESS_EXCEPTION, e);

			}
		}

		return obj;
	}
	
	public <T> T getApi(URI uri, Class<?> responseType,
			MultiValueMap<String, String> headerMap) throws RestServiceException {
		try {
			return (T) restTemplate.exchange(uri, HttpMethod.GET, headerMap == null ? setRequestHeader(null, null) : new HttpEntity<T>(headerMap), responseType)
					.getBody();
		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, METHOD_GET_API,
					e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw new RestServiceException(API_RESOURCE_ACCESS_EXCEPTION, e);
		}

	}
}