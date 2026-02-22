import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.cinema.tickets.TicketServiceImpl;
import uk.gov.dwp.uc.pairtest.cinema.tickets.domain.TicketPrice;
import uk.gov.dwp.uc.pairtest.cinema.tickets.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.cinema.tickets.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.cinema.tickets.repository.TicketPriceRepository;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Server {

    public static void main(String[] args) {

        try {

            int[] captured = {0, 0}; // [totalAmount, totalSeats]

            TicketPaymentService paymentService = (accountId, amount) -> {
                captured[0] = amount;
                System.out.printf("  >> Payment  : account=%d  amount=£%d%n", accountId, amount);
            };

            SeatReservationService seatService = (accountId, seats) -> {
                captured[1] = seats;
                System.out.printf("  >> Seats    : account=%d  seats=%d%n", accountId, seats);
            };

            TicketPriceRepository priceRepository = new TicketPriceRepository();
            TicketServiceImpl     ticketService   = new TicketServiceImpl(paymentService, seatService, priceRepository);

            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

            // POST /api/tickets/purchase
            server.createContext("/api/tickets/purchase", exchange -> {
                try {
                    addCorsHeaders(exchange);

                    if ("OPTIONS".equals(exchange.getRequestMethod())) {
                        exchange.sendResponseHeaders(204, -1);
                        exchange.close();
                        return;
                    }

                    if (!"POST".equals(exchange.getRequestMethod())) {
                        respond(exchange, 405, "{\"error\":\"Method Not Allowed — use POST\"}");
                        return;
                    }

                    String body = new String(exchange.getRequestBody().readAllBytes());
                    System.out.println("\n[Request] POST /api/tickets/purchase");
                    System.out.println("  Body: " + body);

                    captured[0] = 0;
                    captured[1] = 0;

                    try {
                        Long accountId = parseAccountId(body);
                        List<TicketTypeRequest> tickets = parseTickets(body);

                        ticketService.purchaseTickets(accountId, tickets.toArray(new TicketTypeRequest[0]));

                        String json = String.format(
                            "{\"success\":true,\"message\":\"Tickets purchased successfully\",\"totalAmount\":%d,\"totalSeats\":%d}",
                            captured[0], captured[1]
                        );
                        System.out.println("  [200] " + json);
                        respond(exchange, 200, json);

                    } catch (InvalidPurchaseException e) {
                        String json = String.format("{\"success\":false,\"error\":\"%s\"}", escape(e.getMessage()));
                        System.out.println("  [400] " + json);
                        respond(exchange, 400, json);

                    } catch (Exception e) {
                        String json = String.format("{\"success\":false,\"error\":\"%s\"}", escape(e.getMessage()));
                        System.out.println("  [500] " + json);
                        respond(exchange, 500, json);
                    }

                } catch (IOException e) {
                    System.err.println("[ERROR] Failed to handle /api/tickets/purchase: " + e.getMessage());
                }
            });

            // GET /api/tickets/prices  — returns all ticket prices from the DB
            server.createContext("/api/tickets/prices", exchange -> {
                try {
                    addCorsHeaders(exchange);

                    if ("OPTIONS".equals(exchange.getRequestMethod())) {
                        exchange.sendResponseHeaders(204, -1);
                        exchange.close();
                        return;
                    }

                    if (!"GET".equals(exchange.getRequestMethod())) {
                        respond(exchange, 405, "{\"error\":\"Method Not Allowed — use GET\"}");
                        return;
                    }

                    System.out.println("\n[Request] GET /api/tickets/prices");

                    try {
                        List<TicketPrice> prices = priceRepository.getAllPrices();
                        StringBuilder sb = new StringBuilder("[");
                        for (int i = 0; i < prices.size(); i++) {
                            TicketPrice p = prices.get(i);
                            sb.append(String.format(
                                "{\"ticketType\":\"%s\",\"price\":%d,\"requiresSeat\":%b}",
                                p.getTicketType(), p.getPrice(), p.isRequiresSeat()
                            ));
                            if (i < prices.size() - 1) sb.append(",");
                        }
                        sb.append("]");

                        String json = "{\"success\":true,\"prices\":" + sb + "}";
                        System.out.println("  [200] " + json);
                        respond(exchange, 200, json);

                    } catch (Exception e) {
                        String json = String.format("{\"success\":false,\"error\":\"%s\"}", escape(e.getMessage()));
                        System.out.println("  [500] " + json);
                        respond(exchange, 500, json);
                    }

                } catch (IOException e) {
                    System.err.println("[ERROR] Failed to handle /api/tickets/prices: " + e.getMessage());
                }
            });

            // GET /health
            server.createContext("/health", exchange -> {
                try {
                    addCorsHeaders(exchange);
                    respond(exchange, 200, "{\"status\":\"UP\"}");
                } catch (IOException e) {
                    System.err.println("[ERROR] Failed to handle /health: " + e.getMessage());
                }
            });

            server.start();

            
            System.out.println("  Cinema Ticket Service  —  http://localhost:8080 ");
        
            System.out.println("  POST  http://localhost:8080/api/tickets/purchase");
            System.out.println("  GET   http://localhost:8080/api/tickets/prices  ");
            System.out.println("  GET   http://localhost:8080/health              ");
          
            System.out.println("  Press Ctrl+C to stop");
          

        } catch (IOException e) {
            System.err.println("[FATAL] Failed to start HTTP server on port 8080: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("[FATAL] Unexpected error during server startup: " + e.getMessage());
            System.exit(1);
        }
    }

    // JSON parsing 

    private static Long parseAccountId(String json) {
        Matcher m = Pattern.compile("\"accountId\"\\s*:\\s*(-?\\d+)").matcher(json);
        if (!m.find()) throw new RuntimeException("Missing or invalid 'accountId' field");
        return Long.parseLong(m.group(1));
    }

    private static List<TicketTypeRequest> parseTickets(String json) {
        List<TicketTypeRequest> list = new ArrayList<>();

        // Isolate the tickets array
        int start = json.indexOf('[');
        int end   = json.lastIndexOf(']');
        if (start == -1 || end == -1) {
            throw new RuntimeException("Missing 'tickets' array in request body");
        }
        String array = json.substring(start, end + 1);

        // Each ticket object: { "type": "ADULT", "quantity": 2 }  (fields in any order)
        Pattern objPattern = Pattern.compile("\\{([^}]*)\\}");
        Matcher objMatcher = objPattern.matcher(array);

        while (objMatcher.find()) {
            String obj = objMatcher.group(1);

            Matcher typeMatcher = Pattern.compile("\"type\"\\s*:\\s*\"([A-Z]+)\"").matcher(obj);
            Matcher qtyMatcher  = Pattern.compile("\"quantity\"\\s*:\\s*(\\d+)").matcher(obj);

            if (!typeMatcher.find()) throw new RuntimeException("Each ticket must have a 'type' field (ADULT, CHILD, INFANT)");
            if (!qtyMatcher.find())  throw new RuntimeException("Each ticket must have a 'quantity' field");

            String rawType = typeMatcher.group(1);
            int    qty     = Integer.parseInt(qtyMatcher.group(1));

            TicketTypeRequest.Type type;
            try {
                type = TicketTypeRequest.Type.valueOf(rawType);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Unknown ticket type: " + rawType + ". Valid values: ADULT, CHILD, INFANT");
            }

            list.add(new TicketTypeRequest(type, qty));
        }

        if (list.isEmpty()) throw new RuntimeException("'tickets' array must not be empty");
        return list;
    }

    // ── Helpers 
    private static void respond(HttpExchange exchange, int code, String json) throws IOException {
        byte[] body = json.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
