package io.mosip.idrepository.vid.entity;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "vid_expirylist", schema = "idmap")
@Entity
public class ExpiredVid {
	
	/** The Id value */
	@Id
	private int id;

	/** The vid value */
	private String vid;
	
	@Column(name = "cr_date")
	private LocalDate createdDate;
	
	@Column(name = "is_smssent")
	private Boolean isSMSSent;
	
	@Column(name = "is_emailsent")
	private Boolean isEmailSent;
	
}
