package org.test.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import java.util.logging.Level;
import java.util.logging.Logger;

public class CrptApi {
    private static final Logger log = Logger.getLogger(CrptApi.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;
    private final Semaphore semaphore;
    private final int requestLimit;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        this.semaphore = new Semaphore(requestLimit, true);
        this.requestLimit = requestLimit;
        scheduler.scheduleAtFixedRate(this::resetSemaphore, 0, 1, timeUnit);
    }

    public void createDocument(Document document, String signature) throws InterruptedException, JsonProcessingException {
        semaphore.acquire();
        try {
            String json = objectMapper.writeValueAsString(document);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .header("Content-Type", "application/json")
                    .header("signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(response -> log.info("Response received: " + response))
                    .exceptionally(e -> {
                        log.log(Level.SEVERE, "Request failed", e);
                        return null;
                    });
        } catch (JsonProcessingException e) {
            log.log(Level.SEVERE, "Exception during createDocument", e);
            throw e;
        } finally {
            semaphore.release();
        }
    }

    private void resetSemaphore() {
        semaphore.drainPermits();
        semaphore.release(requestLimit);
    }

}

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
class Document {

    private Description description;
    private String docId;
    private String docStatus;
    private String docType;
    private boolean importRequest;
    private String ownerInn;
    private String participantInn;
    private String producerInn;
    private String productionDate;
    private String productionType;
    private List<Product> products;
    private String regDate;
    private String regNumber;
}

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
class Description {

    private String participantInn;
}

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
class Product {

    private String certificateDocument;
    private String certificateDocumentDate;
    private String certificateDocumentNumber;
    private String ownerInn;
    private String producerInn;
    private String productionDate;
    private String tnvedCode;
    private String uitCode;
    private String uituCode;
}