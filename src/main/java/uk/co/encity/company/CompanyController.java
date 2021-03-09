package uk.co.encity.company;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

/**
 * A web controller that provides access to Companies House services.
 */
@RestController
public class CompanyController {

    private WebClient webClient = null;

    private Logger logger = Loggers.getLogger(getClass());
    private final String apiKey;
    private final String apiURL;

    /**
     * This constructor creates an instance of the controller that will use a given API key (allocated
     * by Companies House) to access services at a given URL.
     *
     * @param apiKey the API Key allocated by Companies House (used as the username in basic authentication)
     * @param apiURL the URL of the downstream server that implements the Companies House API
     */
    public CompanyController(@Value("${ch.api.key}") String apiKey, @Value("${ch.api.url}") String apiURL) {
        logger.debug("Constructing " + this.getClass().getName());

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

        logger.debug("Construction of " + this.getClass().getName() + " is complete");
        return;
    }

    /**
     * This method shows how I got a synchronous call working, but RestTemplate is deprecated.  This is here
     * purely for my reference for now, but it should not be called, and will be deleted in due course
     */
    @Deprecated
    @GetMapping("/company/s/{companyNumber}")
    public String getCustomerByCompanyNumberSync(@PathVariable String companyNumber) {
        RestTemplate restTemplate = new RestTemplate();
        URI uri = null;
        String apiKey = this.apiKey;
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.TEXT_PLAIN)); // There is no TEXT_JSON, but this works
        headers.setBasicAuth(Base64Utils.encodeToString((apiKey + ":").getBytes(StandardCharsets.UTF_8)));
        HttpEntity<String> httpEntity = new HttpEntity<>("body", headers);

        ResponseEntity<String> response = null;

        try {
            uri = new URI(this.apiURL + "/company/" + companyNumber);
            response = restTemplate.exchange(uri, HttpMethod.GET, httpEntity, String.class);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }

        //response = restTemplate.exchange(uri, HttpMethod.GET, httpEntity, String.class);

        String result = response.getBody();
        return result;
    }

    /**
     * An exception handler that allows bespoke handling of different status codes returned from
     * the downstream server, so that we can be specific in the way we respond upstream.
     * Without this handler the default behaviour of WebFlux would be to convert all 4xx and 5xx
     * statuses into a generic 500 error, which is not so nice for the client.
     *
     * @param ex the exception generated by the web client
     * @return a ResponseEntity containing an appropriate status and the body received from downstream
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<String> handleWebClientResponseException(WebClientResponseException ex) {
        logger.debug("Received " + ex.getRawStatusCode() + " from downstream server");

        // For now, just pass back to the client the status and response body received
        return ResponseEntity.status(ex.getRawStatusCode()).body(ex.getResponseBodyAsString());

        // TODO:    There is an apparent inconsistency in that if a GET works, we return a Mono<String>, but if it fails,
        //          we return a ResponseEntity<String> to the client.  It doesn't seem to matter, but it smells a bit
    }

    /**
     * Method to GET details of a single company using the official (Companies House) company number to identify
     * the company of interest
     * @param companyNumber the official company number allocated by Companies House
     * @return A {@link reactor.core.publisher.Mono Mono} that publishes a {@link java.lang.String String} containing
     * the JSON object retrieved from companies house
     */
    @CrossOrigin
    @Deprecated
    @GetMapping("/company/{companyNumber}")
    public Mono<String> getCustomerByCompanyNumber(@PathVariable String companyNumber) {

        // Call the Companies House API to fetch the company and return the JSON response
        logger.debug("Retrieving company details for company number " + companyNumber);
        Mono<String> response = this.webClient
                .get()
                .uri("/company/" + companyNumber)
                .retrieve()
                .bodyToMono(String.class);

        return response;
    }


    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<String> handleHttpMessageNotWritableException(HttpMessageNotWritableException ex) {
        logger.debug("Received " + ex.getMessage() + " from downstream server");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("");
    }

    /**
     * Get details of a given company
     * @param companyNumber the Company Number assigned by Companies House
     * @param uriBuilder a {@link UriComponentsBuilder} that is configured for this service
     * @return a well-formed RESTful / HATEOAS style response containing company details, or an error status
     */
    @CrossOrigin
    @GetMapping("/companies/{companyNumber}")
    public Mono<ResponseEntity<EntityModel<CompanyResponse>>> getCompanyDetails(@PathVariable String companyNumber,
                                                                       UriComponentsBuilder uriBuilder) {
        // Configure and initiate the WebClient so that when it executes it produces the right kind of Mono

        Responder responder = new Responder(uriBuilder, companyNumber);

        Mono<ResponseEntity<EntityModel<CompanyResponse>>> result = this.webClient
            .get()
            .uri("/company/" + companyNumber)
            .exchangeToMono(responder::makeResponse);

        return result;
    }

    /**
     * Creates a well-structured RESTful / HATEOAS response that is de-coupled from
     * the Companies House response (although not massively).
     */
    static class Responder {
        private String companyNo;
        private UriComponentsBuilder uriBuilder;
        private Logger logger = Loggers.getLogger(getClass());

        Responder(UriComponentsBuilder uriBuilder, String n) {
            this.companyNo = n;
            this.uriBuilder = uriBuilder;
        }

        Mono<ResponseEntity<EntityModel<CompanyResponse>>> makeResponse(ClientResponse clientResponse) {
            logger.debug("Start of makeResponse");

            Mono<ResponseEntity<EntityModel<CompanyResponse>>> result;
            result = clientResponse.bodyToMono(String.class).flatMap(body -> {
                CompanyResponse response = null;

                // Deserialize...

                ObjectMapper mapper = new ObjectMapper();
                SimpleModule module = new SimpleModule();
                module.addDeserializer(CompanyResponse.class, new CompanyResponseDeserializer());
                mapper.registerModule(module);

                try {
                    response = mapper.readValue(body, CompanyResponse.class);
                    logger.debug("Company response de-serialised successfully");
                } catch (IOException e) {
                    logger.error("Error de-serialising company response: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                }

                // Handle errors from CH API here...


                // Add a self link here...
                EntityModel<CompanyResponse> model;
                try {
                    model = EntityModel.of(response);
                    try {
                        Method m = CompanyController.class.getMethod("getCompanyDetails", String.class, UriComponentsBuilder.class);
                        Link l = linkTo(m, this.companyNo).withSelfRel();

                        model.add(l);
                    } catch (NoSuchMethodException e) {
                        logger.error("Failure generating HAL relations - please investigate.  Company: " + this.companyNo);
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                    }
                } catch (Exception e) {
                    logger.error("Unexpected error generating EntityModel - please investigate: " + this.companyNo);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                }

                // Include the correct location
                UriComponents uriComponents = uriBuilder.path("/companies/" + this.companyNo).build();
                HttpHeaders headers = new HttpHeaders();
                headers.setLocation(uriComponents.toUri());

                return Mono.just(ResponseEntity.status(HttpStatus.OK).headers(headers).body(model));
            });

            logger.debug("end of makeResponse");
            return result;
        }
    }
}
