package org.sj.irs.client;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sj.irs.client.status.IrsRequestStatusClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = IrsclientApplication.class)
@ActiveProfiles("TEST")
public class IrsRequestStatusClientTest {

	@Autowired
	private IrsRequestStatusClient client;

	@Test
	public void checkStatus() {
		/** Put Submitted Receipt ID */
		client.execute("1095C-16-111111");
	}

}
