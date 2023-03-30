//public class SwipeDao {
//}
import java.sql.*;

public class SwipeDao {
    private String jdbcURL;
    private String jdbcUsername;
    private String jdbcPassword;
    private Connection jdbcConnection;

    public SwipeDao(String jdbcURL, String jdbcUsername, String jdbcPassword) {
        this.jdbcURL = jdbcURL;
        this.jdbcUsername = jdbcUsername;
        this.jdbcPassword = jdbcPassword;
    }

    protected void connect() throws SQLException {
        if (jdbcConnection == null || jdbcConnection.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException(e);
            }
            jdbcConnection = DriverManager.getConnection(
                    jdbcURL, jdbcUsername, jdbcPassword);
        }
    }

    protected void disconnect() throws SQLException {
        if (jdbcConnection != null && !jdbcConnection.isClosed()) {
            jdbcConnection.close();
        }
    }

    public Swipe create(Swipe newSwipe) throws SQLException {
        String insertQueryStatement = "INSERT INTO Swipe (Swipe, Swiper, Swipee, Comment) VALUES (?,?,?,?)";
//        Connection conn = null;
        PreparedStatement insertStmt = null;
        ResultSet resultKey = null;
        try {
            connect();
//            insertStmt = conn.prepareStatement(insertQueryStatement);
            insertStmt = jdbcConnection.prepareStatement(insertQueryStatement,
                    Statement.RETURN_GENERATED_KEYS);
            insertStmt.setString(1, newSwipe.getSwipe());
            insertStmt.setString(2, newSwipe.getSwiper());
            insertStmt.setString(3, newSwipe.getSwipee());
            insertStmt.setString(4, newSwipe.getComment());

            // execute insert SQL statement
            insertStmt.executeUpdate();

            // Retrieve the auto-generated key and set it, so it can be used by the caller.
            resultKey = insertStmt.getGeneratedKeys();
            int swipeId = -1;
            if (resultKey.next()){
                swipeId = resultKey.getInt(1);
            }
            newSwipe.setSwipeId(swipeId);
            return newSwipe;
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (jdbcConnection != null) {
                jdbcConnection.close();
            }
            if (insertStmt != null) {
                insertStmt.close();
            }
            if(resultKey != null) {
                resultKey.close();
            }
        }
    }
}