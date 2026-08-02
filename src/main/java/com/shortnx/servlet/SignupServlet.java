package com.shortnx.servlet;

import com.shortnx.db.DBConfig;
import com.shortnx.util.PasswordUtil;
import com.shortnx.util.RateLimiter;
import com.shortnx.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/api/signup")
public class SignupServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");

        if (!RateLimiter.allow("signup:" + req.getRemoteAddr())) {
            resp.setStatus(429);
            resp.getWriter().write(errorJson("Too many attempts. Try again shortly."));
            return;
        }

        String email = req.getParameter("email");
        String password = req.getParameter("password");

        if (!ValidationUtil.isValidEmail(email)) {
            resp.setStatus(400);
            resp.getWriter().write(errorJson("Enter a valid email address."));
            return;
        }
        if (!ValidationUtil.isValidPassword(password)) {
            resp.setStatus(400);
            resp.getWriter().write(errorJson("Password must be at least 8 characters."));
            return;
        }

        String hash = PasswordUtil.hash(password);

        // PreparedStatement everywhere — never string-concat user input into SQL.
        String sql = "INSERT INTO users (email, password_hash) VALUES (?, ?) RETURNING id";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.toLowerCase());
            ps.setString(2, hash);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    req.getSession(true).setAttribute("userId", rs.getLong("id"));
                }
            }
            resp.setStatus(201);
            resp.getWriter().write(new JSONObject().put("success", true).toString());
        } catch (SQLException e) {
            // Unique violation on email
            if ("23505".equals(e.getSQLState())) {
                resp.setStatus(409);
                resp.getWriter().write(errorJson("An account with that email already exists."));
            } else {
                resp.setStatus(500);
                resp.getWriter().write(errorJson("Something went wrong. Please try again."));
                // Log full detail server-side only — never leak stack traces to the client.
                getServletContext().log("Signup error", e);
            }
        }
    }

    private String errorJson(String msg) {
        return new JSONObject().put("success", false).put("error", msg).toString();
    }
}
