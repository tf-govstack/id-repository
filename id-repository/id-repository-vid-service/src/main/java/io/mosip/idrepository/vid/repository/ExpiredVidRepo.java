package io.mosip.idrepository.vid.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import io.mosip.idrepository.vid.entity.ExpiredVid;

@Repository
public interface ExpiredVidRepo extends JpaRepository<ExpiredVid, Integer> {

	@Transactional
	@Modifying
	@Query(value="begin transaction; insert into idmap.vid_expirylist(vid, cr_date, is_smssent, is_emailsent) SELECT vid, current_date, false, false FROM idmap.vid WHERE vidtyp_code= 'TEMPORARY' and CAST(expiry_dtimes AS DATE)=current_date+1 and vid not in (SELECT vid FROM idmap.vid_expirylist where cr_date=current_date) ;commit", nativeQuery=true)
	public void insertExpiredVids();
	
	@Transactional
	@Modifying
	@Query(value="delete from idmap.vid_expirylist where cr_date<current_date", nativeQuery=true)
	public void deleteSentVids();
	
	@Transactional
	@Query(value="select * from idmap.vid_expirylist where is_smssent=false or is_emailsent=false", nativeQuery=true)
	public List<ExpiredVid> findNotSentVids();
	
}
