package com.shortnx.servlet;

import com.shortnx.db.DBConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;

@WebServlet("/api/analytics")
public class AnalyticsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");
        long userId = (long) req.getSession().getAttribute("userId");

        long linkId;
        try {
            linkId = Long.parseLong(req.getParameter("linkId"));
        } catch (NumberFormatException | NullPointerException e) {
            resp.setStatus(400);
            return;
        }

        // Ownership check first — join through links so a user can only
        // ever see analytics for a link_id that belongs to them.
        String sql = "SELECT c.clicked_at, c.user_agent, c.referrer " +
                "FROM clicks c JOIN links l ON c.link_id = l.id " +
                "WHERE l.id = ? AND l.user_id = ? ORDER BY c.clicked_at DESC LIMIT 500";

        JSONArray results = new JSONArray();
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, linkId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject row = new JSONObject();
                    row.put("clickedAt", rs.getTimestamp("clicked_at").toString());
                    row.put("userAgent", rs.getString("user_agent"));
                    row.put("referrer", rs.getString("referrer"));
                    results.put(row);
                }
            }
            resp.getWriter().write(results.toString());
        } catch (SQLException e) {
            resp.setStatus(500);
            getServletContext().log("Analytics fetch error", e);
        }
    }
}
