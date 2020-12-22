package uk.co.encity.company;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@RestController
public class CompanyController {

    private WebClient webClient = null;

    private final String apiKey;
    private final String apiURL;

    public CompanyController(@Value("${ch.api.key}") String apiKey, @Value("${ch.api.url}") String apiURL) {
        System.out.println("Company Controller Constructor..."); // REPLACE WITH PROPER LOGGING

        this.apiKey = apiKey;
        this.apiURL = apiURL;

        this.webClient = WebClient.builder()
                .baseUrl(this.apiURL)
                .defaultHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .defaultHeader(HttpHeaders.ACCEPT, "text/json")
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Basic" + " " + Base64Utils.encodeToString((this.apiKey + ":").getBytes(StandardCharsets.UTF_8)))
                .build();

        return;
    }

    /**
     * This method shows how I got a synchronous call working, but RestTemplate is deprecated.  This is here
     * purely for my reference for now, but it should not be called, and will be deleted in due course
     */
    @GetMapping("/company/s/{companyNumber}")
    public String getCustomerByCompanyNumberSync(@PathVariable String companyNumber) {
        RestTemplate restTemplate = new RestTemplate();
        URI uri = null;
        String apiKey = this.apiKey;
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.TEXT_PLAIN)); // There is no TEXT_JSON, but this works
        headers.setBasicAuth(Base64Utils.encodeToString((apiKey + ":").getBytes(StandardCharsets.UTF_8)));
        HttpEntity<String> httpEntity = new HttpEntity<>("body", headers);

        try {
            uri = new URI(this.apiURL + "/company/" + companyNumber);
        } catch (Exception ignored) {
            ;
        }

        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, httpEntity, String.class);

        String result = response.getBody();
        return result;
    }

    /**
     * Method to GET details of a single company using the official (Companies House) company number to identify
     * the company of interest
     * @param companyNumber the official company number allocated by Companies House
     * @return A {@link reactor.core.publisher.Mono Mono} that publishes a {@link java.lang.String String} containing
     * the JSON object retrieved from companies house
     */
    @CrossOrigin
    @GetMapping("/company/{companyNumber}")
    public Mono<String> getCustomerByCompanyNumber(@PathVariable String companyNumber) {

        // Call the Companies House API to fetch the company and return the JSON response
        System.out.println("Calling Companies House API now");
        Mono<String> response = this.webClient.get().uri("/company/" + companyNumber).retrieve().bodyToMono(String.class);
        System.out.println("Returned from Companies House API");

        // Of course this should be removed, but for now I'm keeping it as it's useful for async testing.  I'll find a
        // better way in due course (honest!)
        try {
            System.out.println("Sleeping");
            TimeUnit.SECONDS.sleep(1);
            System.out.println("Waking up");
        } catch (InterruptedException e) {
            System.out.println("Sleep failed!");
        }
        return response;
    }
}
