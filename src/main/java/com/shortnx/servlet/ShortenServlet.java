package com.shortnx.servlet;

import com.shortnx.db.DBConfig;
import com.shortnx.util.CodeGenerator;
import com.shortnx.util.CsrfUtil;
import com.shortnx.util.RateLimiter;
import com.shortnx.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;

@WebServlet("/api/shorten")
public class ShortenServlet extends HttpServlet {

    private static final int MAX_COLLISION_RETRIES = 5;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");

        if (!CsrfUtil.isValid(req)) {
            resp.setStatus(403);
            resp.getWriter().write(errorJson("Session expired. Please refresh and try again."));
            return;
        }

        if (!RateLimiter.allow("shorten:" + req.getRemoteAddr())) {
            resp.setStatus(429);
            resp.getWriter().write(errorJson("Too many requests. Slow down a little."));
            return;
        }

        String longUrl = req.getParameter("url");
        String alias = req.getParameter("alias"); // optional, may be null/blank

        if (!ValidationUtil.isValidLongUrl(longUrl)) {
            resp.setStatus(400);
            resp.getWriter().write(errorJson("Enter a valid http:// or https:// URL."));
            return;
        }
        if (alias != null && !alias.isBlank() && !ValidationUtil.isValidAlias(alias)) {
            resp.setStatus(400);
            resp.getWriter().write(
                    errorJson("Alias must be 3-20 characters: letters, numbers, - or _ only."));
            return;
        }

        Long userId = getUserId(req); // null if anonymous — anonymous shortening still allowed

        try (Connection conn = DBConfig.getConnection()) {
            String code;
            if (alias != null && !alias.isBlank()) {
                code = alias;
                if (codeExists(conn, code)) {
                    resp.setStatus(409);
                    resp.getWriter().write(errorJson("That alias is already taken."));
                    return;
                }
                insertLink(conn, code, longUrl, userId);
            } else {
                code = insertWithGeneratedCode(conn, longUrl, userId);
                if (code == null) {
                    resp.setStatus(500);
                    resp.getWriter().write(errorJson("Could not generate a unique code. Try again."));
                    return;
                }
            }

            String shortUrl = req.getScheme() + "://" + req.getServerName()
                    + (req.getServerPort() == 80 || req.getServerPort() == 443 ? "" : ":" + req.getServerPort())
                    + req.getContextPath() + "/" + code;

            JSONObject json = new JSONObject();
            json.put("success", true);
            json.put("shortUrl", shortUrl);
            json.put("code", code);
            resp.setStatus(201);
            resp.getWriter().write(json.toString());

        } catch (SQLException e) {
            resp.setStatus(500);
            resp.getWriter().write(errorJson("Something went wrong. Please try again."));
            getServletContext().log("Shorten error", e);
        }
    }

    private boolean codeExists(Connection conn, String code) throws SQLException {
        String sql = "SELECT 1 FROM links WHERE short_code = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void insertLink(Connection conn, String code, String longUrl, Long userId) throws SQLException {
        String sql = "INSERT INTO links (short_code, long_url, user_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setString(2, longUrl);
            if (userId != null) ps.setLong(3, userId); else ps.setNull(3, Types.BIGINT);
            ps.executeUpdate();
        }
    }

    /** Retries on collision instead of trusting a single random draw is unique. */
    private String insertWithGeneratedCode(Connection conn, String longUrl, Long userId) throws SQLException {
        for (int attempt = 0; attempt < MAX_COLLISION_RETRIES; attempt++) {
            String code = CodeGenerator.generate();
            try {
                insertLink(conn, code, longUrl, userId);
                return code;
            } catch (SQLException e) {
                if (!"23505".equals(e.getSQLState())) throw e; // rethrow anything that isn't a unique violation
                // else: collision, loop and retry with a new code
            }
        }
        return null;
    }

    private Long getUserId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        Object id = session.getAttribute("userId");
        return id instanceof Long ? (Long) id : null;
    }

    private String errorJson(String msg) {
        return new JSONObject().put("success", false).put("error", msg).toString();
    }
}
