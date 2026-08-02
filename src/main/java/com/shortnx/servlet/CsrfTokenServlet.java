package com.shortnx.servlet;

import com.shortnx.util.CsrfUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Since these are plain static HTML pages (no JSP templating), the CSRF
 * token can't be embedded server-side at render time. Instead the page
 * fetches one over GET on load and attaches it to the next POST.
 */
@WebServlet("/api/csrf-token")
public class CsrfTokenServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String token = CsrfUtil.getOrCreateToken(req.getSession(true));
        resp.setContentType("application/json");
        resp.getWriter().write(new JSONObject().put("csrfToken", token).toString());
    }
}
