package com.shortnx.servlet;

import com.shortnx.db.DBConfig;
import com.shortnx.util.PasswordUtil;
import com.shortnx.util.RateLimiter;
import com.shortnx.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");

        if (!RateLimiter.allow("login:" + com.shortnx.util.ClientIpUtil.getClientIp(req))) {
            resp.setStatus(429);
            resp.getWriter().write(errorJson("Too many attempts. Try again shortly."));
            return;
        }

        String email = req.getParameter("email");
        String password = req.getParameter("password");

        if (!ValidationUtil.isValidEmail(email) || password == null || password.isBlank()) {
            resp.setStatus(400);
            resp.getWriter().write(errorJson("Invalid email or password."));
            return;
        }

        String sql = "SELECT id, password_hash FROM users WHERE email = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                // Deliberately identical error for "no such user" and "wrong
                // password" — distinguishing them lets attackers enumerate
                // registered emails.
                if (!rs.next() || !PasswordUtil.verify(password, rs.getString("password_hash"))) {
                    resp.setStatus(401);
                    resp.getWriter().write(errorJson("Invalid email or password."));
                    return;
                }

                // Prevent session fixation: invalidate any pre-login session
                // and issue a fresh one on successful auth.
                HttpSession old = req.getSession(false);
                if (old != null) old.invalidate();
                HttpSession session = req.getSession(true);
                session.setAttribute("userId", rs.getLong("id"));
                session.setMaxInactiveInterval(30 * 60); // 30 min idle timeout

                resp.getWriter().write(new JSONObject().put("success", true).toString());
            }
        } catch (SQLException e) {
            resp.setStatus(500);
            resp.getWriter().write(errorJson("Something went wrong. Please try again."));
            getServletContext().log("Login error", e);
        }
    }

    private String errorJson(String msg) {
        return new JSONObject().put("success", false).put("error", msg).toString();
    }
}
