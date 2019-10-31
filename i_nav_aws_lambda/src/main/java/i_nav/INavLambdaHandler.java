package i_nav;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class INavLambdaHandler implements RequestStreamHandler {
//public class INavLambdaHandler implements RequestHandler<Request, Object> {

	JSONParser parser = new JSONParser();

//	@Override
//	public Object handleRequest(Request request, Context context) {
//		return "hi " + getLocations(context) + " [[" + request.getPathParameters().keySet().size() + "]]";
//	}

	public static String getLocations(String id) {

		String str = "";
		String where = "";
		if (id != null) {
			where = " where location_id = " + id;
		}
		String query = "select * from locations " + where;

		try {

//			String username = AWSEnvironment.decryptKey("username");
//			String password = AWSEnvironment.decryptKey("password");
//			String endpoint = AWSEnvironment.decryptKey("endpoint");

			String username = System.getenv("username");
			String password = System.getenv("password");
			String endpoint = System.getenv("endpoint");

			String url = "jdbc:mysql://" + endpoint + ":3306/i_nav";

			try {
				Connection conn = DriverManager.getConnection(url, username, password);

				Statement stmt = conn.createStatement();
				ResultSet resultSet = stmt.executeQuery(query);
//				str += resultSet.toString();

				JSONArray jsonArray = new JSONArray();
				while (resultSet.next()) {
					Location location = new Location();
					location.setLocation_id(resultSet.getInt(1));
					location.setShort_name(resultSet.getString(2));
					location.setLong_name(resultSet.getString(3));
					jsonArray.add(location.getJSONString());
				}
				str += jsonArray.toJSONString();

			} catch (SQLException e) {
				str += e.getMessage() + " " + query;
			}

		} catch (Exception e) {
//			str += e.getMessage() + "  <br />";
//			for (StackTraceElement s : e.getStackTrace()) {
//				str += s.toString() + "  <br />";
//			}
		}

		return str;

	}

	@Override
	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
		String proxy = null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		JSONObject responseJson = new JSONObject();
		String responseCode = "200";
		JSONObject event = null;
		JSONObject pps = new JSONObject();
		
		
//		OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
//		String str = reader.readLine();
//		while (str != null) {
//			writer.write(str);
//			str = reader.readLine();
//		}
//		
//		try {
//			JSONObject event2 = (JSONObject) parser.parse(reader);
//			writer.write(event2.toJSONString());
//		} catch (ParseException e) {
//			writer.write(e.getMessage());
//		}
		
		
		
		try {
			event = (JSONObject) parser.parse(reader);
			if (event.get("pathParameters") != null) {
				pps = (JSONObject) event.get("pathParameters");
				if (pps.get("proxy") != null) {
					proxy = (String) pps.get("proxy");
				}

			}
		} catch (Exception pex) {
			responseJson.put("statusCode", "400");
			responseJson.put("exception", pex);
		}

		

		JSONObject responseBody = new JSONObject();
		responseBody.put("input", event.toJSONString());
		
		
		String entity = event.get("entity").toString();
		String queryResultJson = "{}";
		if (entity.equals("location")) {
			String locationId = ((JSONObject)event).get("id").toString();
			queryResultJson = getLocations(locationId);
			
		} else if (entity.equals("locations")) {
			queryResultJson = getLocations(null);
		}
		
		responseBody.put("query result", queryResultJson);
		
		

		JSONObject headerJson = new JSONObject();
		headerJson.put("x-custom-header", "my custom header value");
		headerJson.put("Access-Control-Allow-Origin", "*");

		responseJson.put("isBase64Encoded", false);
		responseJson.put("statusCode", responseCode);
		responseJson.put("headers", headerJson);
		responseJson.put("body", responseBody.toString());

		OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
		writer.write(responseJson.toJSONString());
		writer.close();

	}

}