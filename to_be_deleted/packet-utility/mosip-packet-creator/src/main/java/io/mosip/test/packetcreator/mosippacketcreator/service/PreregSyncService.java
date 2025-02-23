package io.mosip.test.packetcreator.mosippacketcreator.service;


import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class PreregSyncService {
    Logger logger = LoggerFactory.getLogger(PreregSyncService.class);

    @Value("${mosip.test.baseurl}")
    private String baseUrl;

    @Value("${mosip.test.prereg.sycnapi}")
    private String syncapi;

    @Value("${mosip.test.regclient.centerid}")
	private String centerId;
	
	@Autowired
	private APIRequestUtil apiUtil;

	@Autowired
	private ZipUtils zipUtils;

	@Autowired
	private PacketMakerService packetMakerService;

    private LocalDateTime lastSyncTime;

    private String workDirectory;

    @PostConstruct
    public void init() {
		if (workDirectory != null) return;
		try{
			workDirectory = Files.createTempDirectory("prereg").toFile().getAbsolutePath();
			logger.info("CURRENT PRE_REG WORK DIRECTORY --> {}", workDirectory);
		} catch(Exception ex){
			logger.error("", ex);
		}
	}

	public String getWorkDirectory() {
		return workDirectory;
	}

    public JSONObject syncPrereg() throws Exception {
		if(lastSyncTime == null) {
			lastSyncTime = LocalDateTime.now();
			lastSyncTime = lastSyncTime.minus(6, ChronoUnit.DAYS);
		}
		LocalDateTime currentSyncTime = LocalDateTime.now();
		JSONObject syncRequest = new JSONObject();
		syncRequest.put("registrationCenterId", centerId);
		syncRequest.put("fromDate",apiUtil.getUTCDate(lastSyncTime));
		syncRequest.put("toDate",apiUtil.getUTCDate(currentSyncTime));

		JSONObject wrapper = new JSONObject();
		//wrapper.put("metadata", "");
		wrapper.put("version", "1.0");
		wrapper.put("id", "mosip.pre-registration.datasync.fetch.ids");
		wrapper.put("requesttime", apiUtil.getUTCDateTime(null));
		wrapper.put("request", syncRequest);

		logger.info("pre-reg sync request {}", wrapper);

		JSONObject preregResponse = apiUtil.post(syncapi, wrapper);
		logger.info("sync responded with {} pre-reg ids", preregResponse.get("countOfPreRegIds"));
		lastSyncTime = currentSyncTime;		
       return (JSONObject) preregResponse.get("preRegistrationIds");
    }

    public String downloadPreregPacket(String preregId) throws Exception{
		JSONObject preregResponse = apiUtil.get(syncapi+"/"+preregId, new JSONObject(), new JSONObject());
		logger.info("Downloaded data for prereg id {} ", preregResponse.getString("pre-registration-id"));
		Path temPath = Path.of(workDirectory, preregId+".zip");
		Files.write(temPath, Base64.getDecoder().decode(preregResponse.getString("zip-bytes")));
		logger.info("Wrote prereg id {} to {} ", preregResponse.getString("pre-registration-id"), temPath.toString());
        return temPath.toString();
	}
	
	/*public void syncAndDownload() throws Exception {
		JSONObject jb = syncPrereg();
		while(jb.keys().hasNext()) {
			String prid = (String)jb.get("pre-registration-id");
			try {
				String location = downloadPreregPacket(prid);
				logger.info("downloaded the prereg packet in {} ", location);

				File targetDirectory = Path.of(workDirectory, prid).toFile();
				if(!targetDirectory.exists()  && !targetDirectory.mkdir())
					throw new Exception("Failed to create target directory");

				zipUtils.unzip(location, targetDirectory.getAbsolutePath());

			} catch (Exception iox){
				logger.error("Failed for PRID : {}", prid, iox);
			}
		}
	}*/

}