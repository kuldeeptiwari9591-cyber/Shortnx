package com.shortnx.servlet;

import com.shortnx.db.DBConfig;
import com.shortnx.util.CsrfUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;

/**
 * Guarded by AuthFilter (registered for /api/links/*). Every query is
 * scoped to the logged-in user's own rows — this is the "IDOR" defense:
 * a user can never read or delete another user's link just by guessing
 * an id in the URL, because user_id = ? is always part of the WHERE clause.
 */
@WebServlet("/api/links")
public class LinksServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");
        long userId = (long) req.getSession().getAttribute("userId");

        String sql = "SELECT id, short_code, long_url, is_active, created_at, " +
                "(SELECT COUNT(*) FROM clicks WHERE clicks.link_id = links.id) AS click_count " +
                "FROM links WHERE user_id = ? ORDER BY created_at DESC LIMIT 200";

        JSONArray results = new JSONArray();
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject row = new JSONObject();
                    row.put("id", rs.getLong("id"));
                    row.put("shortCode", rs.getString("short_code"));
                    row.put("longUrl", rs.getString("long_url"));
                    row.put("isActive", rs.getBoolean("is_active"));
                    row.put("createdAt", rs.getTimestamp("created_at").toString());
                    row.put("clickCount", rs.getLong("click_count"));
                    results.put(row);
                }
            }
            resp.getWriter().write(results.toString());
        } catch (SQLException e) {
            resp.setStatus(500);
            getServletContext().log("Links fetch error", e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");
        long userId = (long) req.getSession().getAttribute("userId");

        String idParam = req.getParameter("id");
        long linkId;
        try {
            linkId = Long.parseLong(idParam);
        } catch (NumberFormatException | NullPointerException e) {
            resp.setStatus(400);
            return;
        }

        // user_id = ? guarantees you can only delete links you own.
        String sql = "DELETE FROM links WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, linkId);
            ps.setLong(2, userId);
            int deleted = ps.executeUpdate();
            resp.setStatus(deleted > 0 ? 200 : 404);
        } catch (SQLException e) {
            resp.setStatus(500);
            getServletContext().log("Link delete error", e);
        }
    }
}
