package com.shortnx.servlet;

import com.shortnx.db.DBConfig;
import com.shortnx.util.RateLimiter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lets a user preview where a short link actually goes without visiting
 * it — the "expand" half of the product. Deliberately does NOT log a
 * click or touch the redirect path; this is a read-only lookup.
 */
@WebServlet("/api/expand")
public class ExpandServlet extends HttpServlet {

    // Accepts either a bare code ("aB3xQ9k") or a full short URL
    // ("https://shortnx.onrender.com/aB3xQ9k") and pulls the code out.
    private static final Pattern CODE_PATTERN = Pattern.compile("([A-Za-z0-9_-]{1,20})/?$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");

        if (!RateLimiter.allow("expand:" + com.shortnx.util.ClientIpUtil.getClientIp(req))) {
            resp.setStatus(429);
            resp.getWriter().write(errorJson("Too many requests. Slow down a little."));
            return;
        }

        String input = req.getParameter("code");
        if (input == null || input.isBlank()) {
            resp.setStatus(400);
            resp.getWriter().write(errorJson("Paste a ShortNx link or code."));
            return;
        }

        Matcher matcher = CODE_PATTERN.matcher(input.trim());
        if (!matcher.find()) {
            resp.setStatus(400);
            resp.getWriter().write(errorJson("That doesn't look like a valid ShortNx code."));
            return;
        }
        String code = matcher.group(1);

        String sql = "SELECT long_url, is_active, expiry, created_at FROM links WHERE short_code = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    resp.setStatus(404);
                    resp.getWriter().write(errorJson("No link found for that code."));
                    return;
                }

                boolean active = rs.getBoolean("is_active");
                Timestamp expiry = rs.getTimestamp("expiry");
                boolean expired = expiry != null && expiry.before(new Timestamp(System.currentTimeMillis()));

                JSONObject json = new JSONObject();
                json.put("success", true);
                json.put("code", code);
                json.put("longUrl", rs.getString("long_url"));
                json.put("active", active && !expired);
                json.put("createdAt", rs.getTimestamp("created_at").toString());
                resp.getWriter().write(json.toString());
            }
        } catch (SQLException e) {
            resp.setStatus(500);
            resp.getWriter().write(errorJson("Something went wrong. Please try again."));
            getServletContext().log("Expand error", e);
        }
    }

    private String errorJson(String msg) {
        return new JSONObject().put("success", false).put("error", msg).toString();
    }
}
