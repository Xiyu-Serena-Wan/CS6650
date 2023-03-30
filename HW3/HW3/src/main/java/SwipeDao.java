import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwipeDao {
    private final String jdbcURL;
    private final String jdbcUsername;
    private final String jdbcPassword;
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

    public List<String> getSwipesFromMatches(String userID)
            throws SQLException {
        List<String> users = new ArrayList<>();
        String selectUsers =
                "SELECT Swipee FROM Swipe WHERE Swiper = ? and Swipe = 'right' LIMIT 100;";
        PreparedStatement selectStmt = null;
        ResultSet results = null;
        try {
            connect();
            selectStmt = jdbcConnection.prepareStatement(selectUsers);
            selectStmt.setString(1, userID);
            results = selectStmt.executeQuery();
            while(results.next()) {
                String userName = results.getString("Swipee");
                users.add(userName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if(selectStmt != null) {
                selectStmt.close();
            }
            if(results != null) {
                results.close();
            }
        }
        return users;
    }

    public Map<String, Integer> getStatsFromMatches(String userID)
            throws SQLException {
        Map<String, Integer> likes = new HashMap<>();
        String selectUsers =
                "SELECT SWIPERIGHT, SWIPELIFT, L.Swiper FROM (SELECT COUNT(Swipee) AS SWIPERIGHT, Swiper FROM Swipe " +
                        "WHERE Swiper = ? and Swipe = 'right') AS R INNER JOIN (SELECT COUNT(Swipee) AS SWIPELIFT, Swiper FROM Swipe " +
                        "WHERE Swiper = ? and Swipe = 'left') AS L ON L.Swiper = R.Swiper;";
        PreparedStatement selectStmt = null;
        ResultSet results = null;
        try {
            connect();
            selectStmt = jdbcConnection.prepareStatement(selectUsers);
            selectStmt.setString(1, userID);
            selectStmt.setString(2, userID);
            results = selectStmt.executeQuery();
            if(results.next()) {
                Integer right = Integer.valueOf(results.getString("SWIPERIGHT"));
                Integer left = Integer.valueOf(results.getString("SWIPELIFT"));
                likes.put("right", right);
                likes.put("left", left);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if(selectStmt != null) {
                selectStmt.close();
            }
            if(results != null) {
                results.close();
            }
        }
        return likes;
    }
}