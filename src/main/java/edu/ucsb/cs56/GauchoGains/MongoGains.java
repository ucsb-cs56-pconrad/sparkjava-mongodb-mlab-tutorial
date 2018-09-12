package edu.ucsb.cs56.GauchoGains;

/*
 * Some portions:
 * Copyright (c) 2017 ObjectLabs Corporation
 * Distributed under the MIT license - http://opensource.org/licenses/MIT
 *
 * Written with mongo-3.4.2.jar
 * Documentation: http://api.mongodb.org/java/
 * A Java class connecting to a MongoDB database given a MongoDB Connection URI.
 */

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.port;

import java.net.UnknownHostException;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;

import org.bson.Document;
import java.util.Arrays;
import com.mongodb.Block;

import com.mongodb.client.MongoCursor;
import static com.mongodb.client.model.Filters.*;
import com.mongodb.client.result.DeleteResult;
import static com.mongodb.client.model.Updates.*;
import com.mongodb.client.result.UpdateResult;
import java.util.ArrayList;
import java.util.List;

import edu.ucsb.cs56.GauchoGains.User;

import static spark.Spark.get;
import static spark.Spark.post;
import spark.ModelAndView;
import spark.template.mustache.MustacheTemplateEngine;

/**
 * Hello world!
 *
 */

public class MongoGains {

	/**
	  return a HashMap with values of all the environment variables
	  listed; print error message for each missing one, and exit if any
	  of them is not defined.
	  */
	public static HashMap<String,String> getNeededEnvVars(String [] neededEnvVars) {
		ProcessBuilder processBuilder = new ProcessBuilder();
		HashMap<String,String> envVars = new HashMap<String,String>();
		boolean error=false;		
		for (String k:neededEnvVars) {
			String v = processBuilder.environment().get(k);
			if ( v!= null) {
				envVars.put(k,v);
			} else {
				error = true;
				System.err.println("Error: Must define env variable " + k);
			}
		}
		if (error) { System.exit(1); }
		System.out.println("envVars=" + envVars);
		return envVars;	 
	}
	public static String mongoDBUri(HashMap<String,String> envVars) {

		System.out.println("envVars=" + envVars);

		// mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]]
		String uriString = "mongodb://" +
			envVars.get("MONGODB_USER") + ":" +
			envVars.get("MONGODB_PASS") + "@" +
			envVars.get("MONGODB_HOST") + ":" +
			envVars.get("MONGODB_PORT") + "/" +
			envVars.get("MONGODB_NAME");
		System.out.println("uriString=" + uriString);
		return uriString;
	}

	public static void main(String[] args) {
		String uriString = initMongoDB();
		port(getHerokuAssignedPort());

		Map map = new HashMap();
		get("/", (rq, rs) -> new ModelAndView(map, "signupform.mustache"), new MustacheTemplateEngine());
		post("/check", (rq,rs) -> signUp(rq, uriString));

	}

	public static String signUp(spark.Request rq, String uriString) {
		MongoClientURI uri = new MongoClientURI(uriString);
                MongoClient client = new MongoClient(uri);
                MongoDatabase db = client.getDatabase(uri.getDatabase());
                MongoCollection<Document> users = db.getCollection("users");

		String email = rq.queryParams("email").toLowerCase();
		String firstName = rq.queryParams("firstname");
		String lastName = rq.queryParams("lastname");
		String password = rq.queryParams("password");
		
		String checkValid = checkValidUser(email, firstName, lastName, password, uriString);
		if (checkValid.equals("valid")) {
			Document newUser = new Document("_id", email)
				.append("firstName", firstName)
				.append("lastName", lastName)
				.append("password", password);
			users.insertOne(newUser);
		} else {
			return checkValid;
		}	
		return "Sign Up Success";
	}
	static String checkValidUser(String email, String firstName, String lastName, String password, String uriString) {
		MongoClientURI uri = new MongoClientURI(uriString);
		MongoClient client = new MongoClient(uri);
		MongoDatabase db = client.getDatabase(uri.getDatabase());
		MongoCollection<Document> users = db.getCollection("users");

		if(email.length() == 0 || firstName.length() == 0 || lastName.length() == 0 || password.length() == 0)
			return "Please fill in all forms";
		if(!email.contains("@") || email.length() < 5 || email.charAt(email.length()-4) != '.' ||
				email.charAt(email.indexOf("@")+1) == '.')
			return "Invalid Email";
		if(password.length() < 6)
			return "Please enter a password with length greater than 6";
		Document findQuery = new Document("_id", email);
		if(users.count(findQuery) > 0)
			return "Email is already in use";

		return "valid";
	}

	static String initMongoDB() {
		HashMap<String, String> envVars = getNeededEnvVars(new String []{
			"MONGODB_USER",
				"MONGODB_PASS",
				"MONGODB_NAME",
				"MONGODB_HOST",
				"MONGODB_PORT"
		});
		return mongoDBUri(envVars);
	}

	static int getHerokuAssignedPort() {
		ProcessBuilder processBuilder = new ProcessBuilder();
		if (processBuilder.environment().get("PORT") != null) {
			return Integer.parseInt(processBuilder.environment().get("PORT"));
		}
		return 4567; //return default port if heroku-port isn't set (i.e. on localhost)
	}

	static String makeString(ArrayList<String> text) {
		String resultString = "";
		for (String s: text) {
			resultString += "<b> " + s + "</b><br/>";
		}
		return resultString;
	}

}
