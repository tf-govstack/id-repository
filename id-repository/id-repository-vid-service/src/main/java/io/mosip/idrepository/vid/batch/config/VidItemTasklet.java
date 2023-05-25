package io.mosip.idrepository.vid.batch.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.vid.entity.ExpiredVid;
import io.mosip.idrepository.vid.entity.Vid;
import io.mosip.idrepository.vid.repository.ExpiredVidRepo;
import io.mosip.idrepository.vid.repository.VidRepo;
import io.mosip.idrepository.vid.service.impl.NotificationService;

@Component
public class VidItemTasklet implements Tasklet {

	@Autowired
	private VidRepo vidRepo;

	@Autowired
	private ExpiredVidRepo expiredVidRepo;
	
	@Autowired
	private NotificationService notifictionService;

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		storeExpiredVids();
		
		//notifictionService.sendNotification("5490873289160278");
		// List<NotificationsRecord>
		// loop thorugh the above list
		// send notifications servicd (By calling the notification service)
		return null;
	}

	public void storeExpiredVids() {
//		List<Vid> expiredIdmapVids = vidRepo.findExpiredVids(LocalDateTime.now());
//
//		List<ExpiredVid> expiredVids = new ArrayList<>();
//
//		expiredIdmapVids.forEach(vid -> {
//			expiredVids.add(new ExpiredVid(vid.getVid(), LocalDateTime.now(), false));
//		});
//
//		expiredVidRepo.saveAll(expiredVids);\
		
		expiredVidRepo.insertExpiredVids();
	}

}
