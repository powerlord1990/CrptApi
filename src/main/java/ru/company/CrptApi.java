import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private final AtomicInteger requestCounter;
    private final int requestLimit;
    private final long timeUnitMillis;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.requestCounter = new AtomicInteger(0);
        this.requestLimit = requestLimit;
        this.timeUnitMillis = timeUnit.toMillis(1);

        scheduler.scheduleAtFixedRate(() -> requestCounter.set(0), timeUnitMillis, timeUnitMillis, TimeUnit.MILLISECONDS);
    }

    public synchronized HttpResponse<String> createDocument(Document document, String signature) throws IOException, InterruptedException {
        while (requestCounter.get() >= requestLimit) {
            wait();
        }

        requestCounter.incrementAndGet();
        notifyAll();

        String jsonBody = objectMapper.writeValueAsString(document);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                .header("Content-Type", "application/json")
                .header("Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        synchronized (this) {
            requestCounter.decrementAndGet();
            notifyAll();
        }

        return response;
    }

    public static class Document {
        public Description description;
        public String doc_id;
        public String doc_status;
        public String doc_type;
        public boolean importRequest;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public String production_date;
        public String production_type;
        public Product[] products;
        public String reg_date;
        public String reg_number;

        public static class Description {
            public String participantInn;
        }

        public static class Product {
            public String certificate_document;
            public String certificate_document_date;
            public String certificate_document_number;
            public String owner_inn;
            public String producer_inn;
            public String production_date;
            public String tnved_code;
            public String uit_code;
            public String uitu_code;
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        CrptApi api = new CrptApi(TimeUnit.MINUTES, 5);
        // От приватный полей и вызова через setter отказался для того что б облегчить вызов
        Document document = new Document();
        document.description = new Document.Description();
        document.description.participantInn = "1234567890";
        document.doc_id = "string";
        document.doc_type = "LP_INTRODUCE_GOODS";
        document.doc_status = "string";
        document.importRequest = true;
        document.owner_inn = "string";
        document.participant_inn = "string";
        document.producer_inn = "string";
        document.production_date = "2020-01-23";
        document.production_type = "string";
        document.products = new Document.Product[1];
        document.products[0] = new Document.Product();
        document.products[0].certificate_document = "string";
        document.products[0].certificate_document_date = "2020-01-23";
        document.products[0].certificate_document_number = "string";
        document.products[0].owner_inn = "string";
        document.products[0].producer_inn = "string";
        document.products[0].production_date = "2020-01-23";
        document.products[0].tnved_code = "string";
        document.products[0].uit_code = "string";
        document.products[0].uitu_code = "string";
        document.reg_date = "2020-01-23";
        document.reg_number = "string";

        String signature = "example_signature";

        HttpResponse<String> response = api.createDocument(document, signature);
        // однако запрос возвращает 401 ошибку документацию к Api я не нашел, предпологаю что нужно
        // из внутренней сети обратиться, если я правильно понял
        System.out.println(response.body());
    }
}