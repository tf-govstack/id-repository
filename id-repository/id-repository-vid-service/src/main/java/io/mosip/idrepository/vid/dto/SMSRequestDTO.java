package io.mosip.idrepository.vid.dto;

import lombok.Data;

@Data
public class SMSRequestDTO {

	private String message;
	
	private String number;
}
