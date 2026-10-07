package com.example.productmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.example.productmanager.entity.Product;
import com.example.productmanager.multilanguage.GoogleTranslateMessageSource;
import tools.jackson.databind.ObjectMapper;

class GoogleTranslateServiceTests {

	@Test
	void translateCachesTheGoogleTranslationAndUnescapesItsText() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		server.expect(requestTo(org.hamcrest.Matchers.containsString("/translate_a/single")))
				.andExpect(method(HttpMethod.GET))
				.andExpect(queryParam("client", "gtx"))
				.andExpect(queryParam("sl", "vi"))
				.andExpect(queryParam("tl", "en"))
				.andExpect(requestTo(org.hamcrest.Matchers.containsString("q=Xin%20ch%C3%A0o")))
				.andRespond(withSuccess("[[[\"Hello &amp; welcome\",\"Xin chào\",null,null,1]],null,\"vi\"]",
						MediaType.APPLICATION_JSON));
		GoogleTranslateService translateService = new GoogleTranslateService(
				restClientBuilder.build(), new ObjectMapper());

		assertEquals("Hello & welcome", translateService.translate("Xin chào", "en"));
		assertEquals("Hello & welcome", translateService.translate("Xin chào", "en"));
		server.verify();
	}

	@Test
	void vietnameseMessageSourceUsesVietnameseAsItsSourceLocale() {
		GoogleTranslateMessageSource messageSource = new GoogleTranslateMessageSource(new GoogleTranslateService());

		assertEquals("SIêu thị ABC",
				messageSource.getMessage("app.system", null, Locale.forLanguageTag("vi-VN")));
	}

	@Test
	void englishMessageSourcePrefersBundleAndTranslatesMissingMessages() {
		AtomicInteger translationCalls = new AtomicInteger();
		GoogleTranslateService translateService = new GoogleTranslateService() {
			@Override
			public String translate(String text, String targetLanguage) {
				translationCalls.incrementAndGet();
				assertEquals("Chọn ảnh từ máy", text);
				assertEquals("en", targetLanguage);
				return "Translated fallback";
			}
		};
		GoogleTranslateMessageSource messageSource = new GoogleTranslateMessageSource(translateService);

		assertEquals("SIêu thị ABC", messageSource.getMessage("app.system", null, Locale.ENGLISH));
		assertEquals(0, translationCalls.get());
		assertEquals("Translated fallback",
				messageSource.getMessage("profile.avatarFile", null, Locale.ENGLISH));
		assertEquals(1, translationCalls.get());
	}

	@Test
	void productDisplayUsesStoredEnglishAndTranslatesMissingNameOrUnit() {
		AtomicInteger translationCalls = new AtomicInteger();
		GoogleTranslateService translateService = new GoogleTranslateService() {
			@Override
			public String translate(String text, String targetLanguage) {
				translationCalls.incrementAndGet();
				return "Translated " + text;
			}
		};
		ProductDisplayService productDisplayService = new ProductDisplayService(translateService);
		Product product = Product.builder()
				.nameVi("Táo")
				.nameEn("Apple")
				.unitVi("Quả")
				.build();

		LocaleContextHolder.setLocale(Locale.ENGLISH);
		try {
			assertEquals("Apple", productDisplayService.displayName(product));
			assertEquals("Translated Quả", productDisplayService.displayUnit(product));
			assertEquals(1, translationCalls.get());
		} finally {
			LocaleContextHolder.resetLocaleContext();
		}
	}
}
