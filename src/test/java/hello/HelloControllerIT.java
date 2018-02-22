package hello;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.HttpClients;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HelloControllerIT {

	@LocalServerPort
	private int port;

	private RestTemplate template;

	@Before
	public void setUp() throws Exception {
		createTemplateFromKeyStore("keystore.p12");
	}

	@Test
	public void getHello() throws Exception {
		ResponseEntity<String> response = template.getForEntity("https://localhost:" + port + "/", String.class);
		assertThat(response.getBody(), equalTo("Greetings from Spring Boot!"));
	}

	private void createTemplateFromKeyStore(String keyStoreName) {
		try {
			InputStream keyStoreInputStream = getClass().getResourceAsStream(keyStoreName);
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(keyStoreInputStream, null);

			SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
					.loadKeyMaterial(keyStore, "password".toCharArray())
					.loadTrustMaterial(keyStore, new TrustAllStrategy()).build();

			HttpClient httpClient = HttpClients.custom().setSSLContext(sslContext)
					.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).build();

			HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
			requestFactory.setHttpClient(httpClient);

			template = new RestTemplate(requestFactory);
		} catch (IOException | GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}
}
