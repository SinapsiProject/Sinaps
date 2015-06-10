package com.sinapsi.webservice.web;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.sinapsi.model.impl.User;
import com.sinapsi.webservice.db.UserDBManager;

/**
 * Servlet implementation class WebLogin
 */
@WebServlet("/web_login")
public class WebLogin extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    
		String email = request.getParameter("email");
		String password = request.getParameter("password");
		UserDBManager userManager = (UserDBManager) getServletContext().getAttribute("users_db");
		
		try {
		    User user = (User) userManager.getUserByEmail(email);           
            if (user != null) {
                
                // the user is ok
                if (userManager.checkUser(email, password)) {          
                    Cookie cookie = new Cookie("user", email);
                   
                    cookie.setMaxAge(60*60);
                    response.addCookie(cookie);
                    //response.sendRedirect("index.jsp");
                    request.getRequestDispatcher("index.jsp").forward(request, response);
                } else {
                    request.getRequestDispatcher("login.html").forward(request, response);
                }
            // the user doesn't exist in the db
            } else {
                request.getRequestDispatcher("login.html").forward(request, response);
            }
		} catch(Exception e) {
		    e.printStackTrace();
		}
	}

}