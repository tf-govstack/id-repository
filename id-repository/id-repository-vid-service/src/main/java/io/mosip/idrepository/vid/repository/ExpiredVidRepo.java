package io.mosip.idrepository.vid.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.mosip.idrepository.vid.entity.ExpiredVid;

@Repository
public interface ExpiredVidRepo extends JpaRepository<ExpiredVid, String> {

}
