package com.sinapsi.webservice.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.sinapsi.model.ActionInterface;
import com.sinapsi.webservice.db.DatabaseManager;

/**
 * Servlet implementation class AvailableActionServlet
 */
@WebServlet("/available_actions")
public class AvailableActionServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		DatabaseManager db = new DatabaseManager();
		response.setContentType("application/json");
		int idDevice = Integer.parseInt(request.getParameter("device"));
		
		try {
			List<ActionInterface> actions = db.getAvailableAction(idDevice);
			Gson gson = new Gson();
			out.print(gson.toJson(actions)); 
			out.flush(); 
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		StringBuffer jb = new StringBuffer();
		String line = null;
		Gson gson = new Gson();
		try {
			BufferedReader reader = request.getReader();
		    while ((line = reader.readLine()) != null)
		      jb.append(line);
		    
		} catch (Exception e) { 
			e.printStackTrace(); 
		}

		String jsonstring = jb.toString();
		//DEBUG
		System.out.println(jsonstring);
		ArrayList<ActionInterface> list = gson.fromJson(jsonstring, new TypeToken<ArrayList<ActionInterface>>(){}.getType());
		System.out.println(list.size());
	}

}
