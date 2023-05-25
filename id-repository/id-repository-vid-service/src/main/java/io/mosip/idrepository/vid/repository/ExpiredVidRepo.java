package io.mosip.idrepository.vid.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import io.mosip.idrepository.vid.entity.ExpiredVid;

@Repository
public interface ExpiredVidRepo extends JpaRepository<ExpiredVid, String> {

	@Modifying
	@Query(value="insert into idmap.vid_expirylist(vid, currentdate, issend) SELECT vid, now(), false FROM idmap.vid WHERE vidtyp_code= 'TEMPORARY' AND NOW() = NOW() and vid not in (SELECT vid FROM idmap.vid_expirylist)", nativeQuery=true)
	public void insertExpiredVids();
	
}
