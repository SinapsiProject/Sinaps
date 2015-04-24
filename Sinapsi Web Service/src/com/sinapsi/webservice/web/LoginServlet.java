package com.sinapsi.webservice.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.sinapsi.model.impl.User;
import com.sinapsi.webservice.db.DatabaseManager;

/**
 * Servlet implementation class LoginServlet
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("applications/json");
		PrintWriter out = response.getWriter();
		DatabaseManager db = new DatabaseManager();
		
		try {
			String email = request.getParameter("email");
			String pwd = request.getParameter("password"); 
			if(db.checkUser(email, pwd)) {
				Gson gson = new Gson();
				User u = (User)db.getUserByEmail(email);
				out.print(gson.toJson(u)); 
			} else {
				Gson gson = new Gson();
				User user = (User) db.newUser(email, pwd);
				user.errorOccured(true);
				user.setErrorDescription("Login error");
				
				out.print(gson.toJson(user));
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
