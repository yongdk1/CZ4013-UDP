package com.flightsudp.server;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class FlightServerController {
    private Map<String, FlightService> serviceMap;
    private Map<UUID, String> seenMap;

    public FlightServerController() {
        serviceMap = new HashMap<>();
        // Add FlightService implementations to the service map
        serviceMap.put("listFlights", new ListFlightsService());
        // TODO: XINRUI Add additional services as necessary
        // TODO: XINRUI make pubsub
        // TODO: XINRUI make dummy data + loader into memory

        seenMap = new HashMap<>();
    }

    public String processInput(String input) throws Exception {
        // Parse request JSON from input data
        JSONObject requestJson = new JSONObject(input);

        Boolean packetLossClientToServer = requestJson.getBoolean("packetLossClientToServer");
        if (packetLossClientToServer)
            throw new Exception("Packet Loss From Client To Server");

        String functionName = requestJson.getString("function");
        JSONObject params = requestJson.getJSONObject("data");
        String semantics = requestJson.getString("semantics");

        String str_uuid = requestJson.getString("uuid");
        UUID uuid = UUID.fromString(str_uuid);

        if (Objects.equals(semantics, "AT-MOST-ONCE")) {
            // Checks for duplicate with seenMap
            String cachedResponse = seenMap.get(uuid);
            if (cachedResponse != null) {
                Boolean packetLossServerToClient = requestJson.getBoolean("packetLossServerToClient");
                if (packetLossServerToClient)
                    throw new Exception("Packet Loss From Server To Client");
                return cachedResponse;
            }
        }

        // Invoke the appropriate service method based on the function name
        FlightService service = serviceMap.get(functionName);
        if (service == null) {
            return generateErrorResponse("Invalid function name: " + functionName);
        }
        JSONObject result = service.execute(params);

        // Construct response JSON with status and result data
        JSONObject responseJson = new JSONObject();
        responseJson.put("status", "success");
        responseJson.put("data", result);
        String responseString = responseJson.toString();

        seenMap.put(uuid, responseString);

        Boolean packetLossServerToClient = requestJson.getBoolean("packetLossServerToClient");
        if (packetLossServerToClient)
            throw new Exception("Packet Loss From Server To Client");

        return responseString;
    }

    private String generateErrorResponse(String message) {
        // Construct response JSON with error status and error message
        JSONObject responseJson = new JSONObject();
        responseJson.put("status", "error");
        JSONObject errorData = new JSONObject();
        errorData.put("message", message);
        responseJson.put("data", errorData);

        return responseJson.toString();
    }



    // TODO: DUANKAI handle duplicate req messages (at-least-once semantics) via histories - DONE
    // TODO: DUANKAI handle monitoring -> return "monitoring closed" as result when time is up
}
