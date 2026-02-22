package uk.gov.dwp.uc.pairtest.cinema.tickets.repository;

import uk.gov.dwp.uc.pairtest.cinema.tickets.domain.TicketPrice;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class TicketPriceRepository {

    private static final String DB_URL  = "jdbc:h2:mem:ticketdb;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASS = "";

    public TicketPriceRepository() {
        initDatabase();
    }

   
    // Initialisation
   

    private void initDatabase() {
        try (Connection conn = getConnection();
             Statement  stmt = conn.createStatement()) {

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS TICKET_PRICES (" +
                "  id           INT AUTO_INCREMENT PRIMARY KEY," +
                "  ticket_type  VARCHAR(10)  NOT NULL UNIQUE," +
                "  price        INT          NOT NULL," +
                "  requires_seat BOOLEAN     NOT NULL" +
                ")"
            );

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM TICKET_PRICES")) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    seedPrices(conn);
                }
            }

            System.out.println("[DB] TICKET_PRICES table ready.");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialise ticket price database: " + e.getMessage(), e);
        }
    }

    private void seedPrices(Connection conn) throws SQLException {
        String sql = "INSERT INTO TICKET_PRICES (ticket_type, price, requires_seat) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            insertPrice(ps, "ADULT",  25, true);
            insertPrice(ps, "CHILD",  15, true);
            insertPrice(ps, "INFANT",  0, false);
            System.out.println("[DB] Seeded default ticket prices.");
        }
    }

    private void insertPrice(PreparedStatement ps, String type, int price, boolean seat)
            throws SQLException {
        ps.setString(1, type);
        ps.setInt(2, price);
        ps.setBoolean(3, seat);
        ps.executeUpdate();
    }

  
    // Query methods

    /**
     * Returns the price in GBP for the given ticket type 
     *
     * @throws RuntimeException if the type is not found or a DB error occurs
     */
    public int getPriceByType(String ticketType) {
        String sql = "SELECT price FROM TICKET_PRICES WHERE ticket_type = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, ticketType.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("price");
                }
            }
            throw new RuntimeException("Unknown ticket type: " + ticketType);

        } catch (SQLException e) {
            throw new RuntimeException(
                "Failed to fetch price for ticket type '" + ticketType + "': " + e.getMessage(), e);
        }
    }

    /**
     * Returns all ticket price records ordered by id.
     */
    public List<TicketPrice> getAllPrices() {
        List<TicketPrice> prices = new ArrayList<>();
        String sql = "SELECT ticket_type, price, requires_seat FROM TICKET_PRICES ORDER BY id";

        try (Connection conn = getConnection();
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {

            while (rs.next()) {
                prices.add(new TicketPrice(
                    rs.getString("ticket_type"),
                    rs.getInt("price"),
                    rs.getBoolean("requires_seat")
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve ticket prices: " + e.getMessage(), e);
        }

        return prices;
    }

 
    // Helper
  
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }
}
