package com.example.mall.thirdparty;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.InputStream;

@SpringBootTest
class MallThirdPartyApplicationTests {

	@Test
	public void testUpload() throws Exception {
		String endpoint = "oss-cn-nanjing.aliyuncs.com";
		String accessKeyId = "LTAI5tGMpGrzWEKPYQNM1V4F";
		String accessKeySecret = "26kk4o53CJbkgBIWBA1TdkpWPWYz52";
		String bucketName = "mall-qian";
		String objectName = "bug.jpg";
		String filePath= "C:\\Users\\31654\\Documents\\Memory\\Activity\\mall\\mall\\resources\\demo\\bug.jpg";

		OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

		try {
			InputStream inputStream = new FileInputStream(filePath);
			ossClient.putObject(bucketName, objectName, inputStream);
		} catch (OSSException oe) {
			System.out.println("Caught an OSSException, which means your request made it to OSS, "
					+ "but was rejected with an error response for some reason.");
			System.out.println("Error Message:" + oe.getErrorMessage());
			System.out.println("Error Code:" + oe.getErrorCode());
			System.out.println("Request ID:" + oe.getRequestId());
			System.out.println("Host ID:" + oe.getHostId());
		} catch (ClientException ce) {
			System.out.println("Caught an ClientException, which means the client encountered "
					+ "a serious internal problem while trying to communicate with OSS, "
					+ "such as not being able to access the network.");
			System.out.println("Error Message:" + ce.getMessage());
		} finally {
			if (ossClient != null) {
				ossClient.shutdown();
			}
		}
	}

}
