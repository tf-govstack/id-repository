package io.mosip.idrepository.vid.util;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

public class JsonUtil {

	private static ObjectMapper objectMapper;

	static {
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.registerModule(new AfterburnerModule());
		objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
	}

	@SuppressWarnings("unchecked")
	public static JSONObject getJSONObject(JSONObject jsonObject, Object key) {
		LinkedHashMap<Object, Object> identity = null;
		if (jsonObject.get(key) instanceof LinkedHashMap) {
			identity = (LinkedHashMap<Object, Object>) jsonObject.get(key);
		}
		return identity != null ? new JSONObject(identity) : null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static JSONArray getJSONArray(JSONObject jsonObject, Object key) {
		ArrayList value = (ArrayList) jsonObject.get(key);
		if (value == null)
			return null;
		JSONArray jsonArray = new JSONArray();
		jsonArray.addAll(value);

		return jsonArray;

	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getJSONValue(JSONObject jsonObject, String key) {
		T value = (T) jsonObject.get(key);
		return value;
	}

	public static String writeValueAsString(Object obj) throws IOException {
		return objectMapper.writeValueAsString(obj);
	}

	@SuppressWarnings("unchecked")
	public static <T> T readValue(String jsonString, Class<?> clazz) throws IOException {
		return (T) objectMapper.readValue(jsonString, clazz);
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] mapJsonNodeToJavaObject(Class<? extends Object> genericType, JSONArray demographicJsonNode)
			throws ReflectiveOperationException {
		String language;
		String value;
		T[] javaObject = (T[]) Array.newInstance(genericType, demographicJsonNode.size());
		try {
			for (int i = 0; i < demographicJsonNode.size(); i++) {

				T jsonNodeElement = (T) genericType.newInstance();

				JSONObject objects = JsonUtil.getJSONObjectFromArray(demographicJsonNode, i);
				if (objects != null) {
					language = (String) objects.get("language");
					value = (String) objects.get("value");

					Field languageField = jsonNodeElement.getClass().getDeclaredField("language");
					languageField.setAccessible(true);
					languageField.set(jsonNodeElement, language);

					Field valueField = jsonNodeElement.getClass().getDeclaredField("value");
					valueField.setAccessible(true);
					valueField.set(jsonNodeElement, value);

					javaObject[i] = jsonNodeElement;
				}
			}
		} catch (InstantiationException | IllegalAccessException e) {

			throw e;

		} catch (NoSuchFieldException | SecurityException e) {

			throw e;

		}
		return javaObject;
	}

	@SuppressWarnings("rawtypes")
	public static JSONObject getJSONObjectFromArray(JSONArray jsonObject, int key) {
		LinkedHashMap identity = (LinkedHashMap) jsonObject.get(key);
		return identity != null ? new JSONObject(identity) : null;
	}

}
