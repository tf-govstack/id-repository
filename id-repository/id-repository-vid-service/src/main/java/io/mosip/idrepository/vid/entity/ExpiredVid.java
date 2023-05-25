package io.mosip.idrepository.vid.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "vid_expirylist", schema = "idmap")
@Entity
public class ExpiredVid {
	
	public ExpiredVid(String vid, LocalDateTime currentDate, boolean isSend) {
		this.vid = vid;
		this.currentDate = currentDate;
		this.isSend = isSend;
	}
	
	/** The Id value */
	@Id
	private String id;

	/** The vid value */
	private String vid;
	
	@Column(name = "currentdate")
	private LocalDateTime currentDate;
	
	@Column(name = "issend")
	private boolean isSend;
	
}
