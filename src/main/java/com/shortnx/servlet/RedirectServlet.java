package com.shortnx.servlet;

import com.shortnx.db.DBConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Set;

/**
 * Mapped to "/*" so short links can live at the domain root, e.g.
 * shortnx.app/aB3xQ9k — the way real shorteners behave.
 *
 * Because this catches everything, it first hands off known static/app
 * paths to Tomcat's default servlet, and only treats the remaining
 * single-segment paths as short codes to look up.
 */
@WebServlet("/*")
public class RedirectServlet extends HttpServlet {

    private static final Set<String> STATIC_PREFIXES = Set.of(
            "/css/", "/js/", "/api/", "/images/"
    );
    private static final Set<String> STATIC_FILES = Set.of(
            "/", "/index.html", "/shorten.html", "/login.html", "/signup.html",
            "/dashboard.html", "/404.html", "/robots.txt", "/sitemap.xml", "/favicon.ico"
    );

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (path.isEmpty()) path = "/";

        if (STATIC_FILES.contains(path) || STATIC_PREFIXES.stream().anyMatch(path::startsWith)) {
            RequestDispatcher dispatcher = req.getServletContext().getNamedDispatcher("default");
            dispatcher.forward(req, resp);
            return;
        }

        String code = path.startsWith("/") ? path.substring(1) : path;
        // Reject anything that isn't a plausible short code before touching the DB.
        if (code.isEmpty() || code.length() > 20 || !code.matches("[A-Za-z0-9_-]+")) {
            send404(req, resp);
            return;
        }

        String sql = "SELECT id, long_url, expiry, is_active FROM links WHERE short_code = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !rs.getBoolean("is_active")) {
                    send404(req, resp);
                    return;
                }
                Timestamp expiry = rs.getTimestamp("expiry");
                if (expiry != null && expiry.before(new Timestamp(System.currentTimeMillis()))) {
                    send404(req, resp);
                    return;
                }

                long linkId = rs.getLong("id");
                String longUrl = rs.getString("long_url");

                logClickAsync(linkId, req);

                // 302 (temporary) not 301 — keeps every future click hitting
                // this servlet so clicks stay measurable. A 301 would get
                // cached by the browser and analytics would go blind.
                resp.setStatus(HttpServletResponse.SC_FOUND);
                resp.setHeader("Location", longUrl);
            }
        } catch (SQLException e) {
            getServletContext().log("Redirect lookup error", e);
            resp.setStatus(500);
        }
    }

    /** Fire-and-forget click logging so it never slows down the redirect itself. */
    private void logClickAsync(long linkId, HttpServletRequest req) {
        String ip = req.getRemoteAddr();
        String ua = req.getHeader("User-Agent");
        String referrer = req.getHeader("Referer");
        Thread.startVirtualThread(() -> {
            String sql = "INSERT INTO clicks (link_id, ip_hash, user_agent, referrer) VALUES (?, ?, ?, ?)";
            try (Connection conn = DBConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, linkId);
                ps.setString(2, hashIp(ip));
                ps.setString(3, ua == null ? "" : ua.substring(0, Math.min(500, ua.length())));
                ps.setString(4, referrer == null ? "" : referrer.substring(0, Math.min(500, referrer.length())));
                ps.executeUpdate();
            } catch (SQLException e) {
                getServletContext().log("Click log error", e);
            }
        });
    }

    /** Store a hash instead of the raw IP — enough for abuse detection without keeping PII. */
    private String hashIp(String ip) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ip.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "unknown";
        }
    }

    private void send404(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        req.getRequestDispatcher("/404.html").forward(req, resp);
    }
}
