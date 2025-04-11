package kostovite;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.TimeZone;

public class Chronometer implements PluginInterface {

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");
    private static final Map<String, ChronoSession> activeSessions = new ConcurrentHashMap<>();

    public Chronometer() {
        // Initialize timezone for time formatting
        TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public String getName() {
        return "Chronometer";
    }

    @Override
    public void execute() {
        System.out.println("Chronometer Plugin executed");

        // Demonstrate basic usage
        try {
            // Create a session
            Map<String, Object> createParams = new HashMap<>();
            createParams.put("operation", "create");
            createParams.put("name", "Demo Session");

            Map<String, Object> createResult = process(createParams);
            String sessionId = (String) createResult.get("sessionId");

            System.out.println("Created session: " + sessionId);

            // Start the chronometer
            Map<String, Object> startParams = new HashMap<>();
            startParams.put("operation", "start");
            startParams.put("sessionId", sessionId);

            Map<String, Object> startResult = process(startParams);
            System.out.println("Started chronometer: " + startResult.get("status"));

            // Simulate some time passing
            Thread.sleep(1500);

            // Log a lap time
            Map<String, Object> logParams = new HashMap<>();
            logParams.put("operation", "log");
            logParams.put("sessionId", sessionId);
            logParams.put("label", "Checkpoint 1");

            Map<String, Object> logResult = process(logParams);
            System.out.println("Logged lap time: " + logResult.get("elapsedFormatted"));

            // Simulate more time passing
            Thread.sleep(1500);

            // Get current status
            Map<String, Object> statusParams = new HashMap<>();
            statusParams.put("operation", "status");
            statusParams.put("sessionId", sessionId);

            Map<String, Object> statusResult = process(statusParams);
            System.out.println("Current elapsed time: " + statusResult.get("elapsedFormatted"));

            // Stop the chronometer
            Map<String, Object> stopParams = new HashMap<>();
            stopParams.put("operation", "stop");
            stopParams.put("sessionId", sessionId);

            Map<String, Object> stopResult = process(stopParams);
            System.out.println("Stopped chronometer: " + stopResult.get("elapsedFormatted"));

            // Reset the chronometer
            Map<String, Object> resetParams = new HashMap<>();
            resetParams.put("operation", "reset");
            resetParams.put("sessionId", sessionId);

            process(resetParams);
            System.out.println("Reset chronometer");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", getName()); // Corresponds to ToolMetadata.name
        metadata.put("version", "1.0.0");
        metadata.put("description", "Monitor the duration of events with a chronometer"); // Corresponds to ToolMetadata.description

        // Define available backend operations (for informational purposes or direct API calls)
        Map<String, Object> operations = new HashMap<>();

        // Create session operation
        Map<String, Object> createOperation = new HashMap<>();
        createOperation.put("description", "Create a new chronometer session");
        Map<String, Object> createInputs = new HashMap<>();
        createInputs.put("name", Map.of("type", "string", "description", "Name of the session (optional)"));
        createOperation.put("inputs", createInputs);
        operations.put("create", createOperation);

        // Start operation
        Map<String, Object> startOperation = new HashMap<>();
        startOperation.put("description", "Start the chronometer");
        Map<String, Object> startInputs = new HashMap<>();
        startInputs.put("sessionId", Map.of("type", "string", "description", "ID of the chronometer session", "required", true));
        startOperation.put("inputs", startInputs);
        operations.put("start", startOperation);

        // Stop operation
        Map<String, Object> stopOperation = new HashMap<>();
        stopOperation.put("description", "Stop the chronometer");
        Map<String, Object> stopInputs = new HashMap<>();
        stopInputs.put("sessionId", Map.of("type", "string", "description", "ID of the chronometer session", "required", true));
        stopOperation.put("inputs", stopInputs);
        operations.put("stop", stopOperation);

        // Reset operation
        Map<String, Object> resetOperation = new HashMap<>();
        resetOperation.put("description", "Reset the chronometer");
        Map<String, Object> resetInputs = new HashMap<>();
        resetInputs.put("sessionId", Map.of("type", "string", "description", "ID of the chronometer session", "required", true));
        resetOperation.put("inputs", resetInputs);
        operations.put("reset", resetOperation);

        // Log time operation
        Map<String, Object> logOperation = new HashMap<>();
        logOperation.put("description", "Log a lap time");
        Map<String, Object> logInputs = new HashMap<>();
        logInputs.put("sessionId", Map.of("type", "string", "description", "ID of the chronometer session", "required", true));
        logInputs.put("label", Map.of("type", "string", "description", "Label for the lap time (optional)"));
        logOperation.put("inputs", logInputs);
        operations.put("log", logOperation);

        // Get status operation
        Map<String, Object> statusOperation = new HashMap<>();
        statusOperation.put("description", "Get the current status of the chronometer");
        Map<String, Object> statusInputs = new HashMap<>();
        statusInputs.put("sessionId", Map.of("type", "string", "description", "ID of the chronometer session", "required", true));
        statusOperation.put("inputs", statusInputs);
        operations.put("status", statusOperation);

        // Get logs operation
        Map<String, Object> getLogsOperation = new HashMap<>();
        getLogsOperation.put("description", "Get all logged lap times");
        Map<String, Object> getLogsInputs = new HashMap<>();
        getLogsInputs.put("sessionId", Map.of("type", "string", "description", "ID of the chronometer session", "required", true));
        getLogsOperation.put("inputs", getLogsInputs);
        operations.put("getLogs", getLogsOperation);

        // List sessions operation
        Map<String, Object> listSessionsOperation = new HashMap<>();
        listSessionsOperation.put("description", "List all active chronometer sessions");
        operations.put("listSessions", listSessionsOperation);

        // Delete session operation
        Map<String, Object> deleteSessionOperation = new HashMap<>();
        deleteSessionOperation.put("description", "Delete a chronometer session");
        Map<String, Object> deleteSessionInputs = new HashMap<>();
        deleteSessionInputs.put("sessionId", Map.of("type", "string", "description", "ID of the chronometer session", "required", true));
        deleteSessionOperation.put("inputs", deleteSessionInputs);
        operations.put("deleteSession", deleteSessionOperation);
        metadata.put("operations", operations); // Keep this for backend/API reference

        // --- Define UI Configuration (matches structure in WorldClock) ---
        Map<String, Object> uiConfig = new HashMap<>();
        uiConfig.put("id", "Chronometer"); // Corresponds to ToolMetadata.id
        uiConfig.put("icon", "Timer"); // Corresponds to ToolMetadata.icon (Material Icon name)
        uiConfig.put("category", "Utilities"); // Corresponds to ToolMetadata.category

        // --- Define UI Inputs ---
        List<Map<String, Object>> uiInputs = new ArrayList<>();

        // Input Section 1: Session Setup
        Map<String, Object> inputSection1 = new HashMap<>();
        inputSection1.put("header", "Chronometer Session");
        List<Map<String, Object>> section1Fields = new ArrayList<>();

        // Session name field
        Map<String, Object> nameField = new HashMap<>();
        nameField.put("name", "sessionName");
        nameField.put("label", "Session Name:");
        nameField.put("type", "text");
        nameField.put("default", "Chronometer Session");
        nameField.put("required", false);
        section1Fields.add(nameField);

        // Operation Selection
        Map<String, Object> operationField = new HashMap<>();
        operationField.put("name", "uiOperation");
        operationField.put("label", "Action:");
        operationField.put("type", "select");
        List<Map<String, String>> operationOptions = new ArrayList<>();
        operationOptions.add(Map.of("value", "create", "label", "Create Session"));
        operationOptions.add(Map.of("value", "start", "label", "Start"));
        operationOptions.add(Map.of("value", "stop", "label", "Stop"));
        operationOptions.add(Map.of("value", "reset", "label", "Reset"));
        operationOptions.add(Map.of("value", "log", "label", "Log Lap"));
        operationOptions.add(Map.of("value", "status", "label", "Get Status"));
        operationOptions.add(Map.of("value", "getLogs", "label", "Get Lap Times"));
        operationField.put("options", operationOptions);
        operationField.put("default", "create");
        operationField.put("required", true);
        section1Fields.add(operationField);

        // Session ID field (conditional)
        Map<String, Object> sessionIdField = new HashMap<>();
        sessionIdField.put("name", "sessionId");
        sessionIdField.put("label", "Session ID:");
        sessionIdField.put("type", "text");
        sessionIdField.put("required", true);
        sessionIdField.put("condition", "uiOperation !== 'create' && uiOperation !== 'listSessions'");
        section1Fields.add(sessionIdField);

        inputSection1.put("fields", section1Fields);
        uiInputs.add(inputSection1);

        // Input Section 2: Lap Settings
        Map<String, Object> inputSection2 = new HashMap<>();
        inputSection2.put("header", "Lap Settings");
        inputSection2.put("condition", "uiOperation === 'log'");
        List<Map<String, Object>> section2Fields = new ArrayList<>();

        // Lap label field
        Map<String, Object> lapLabelField = new HashMap<>();
        lapLabelField.put("name", "label");
        lapLabelField.put("label", "Lap Label:");
        lapLabelField.put("type", "text");
        lapLabelField.put("default", "Lap");
        lapLabelField.put("required", false);
        section2Fields.add(lapLabelField);

        inputSection2.put("fields", section2Fields);
        uiInputs.add(inputSection2);

        uiConfig.put("inputs", uiInputs);

        // --- Define UI Outputs ---
        List<Map<String, Object>> uiOutputs = new ArrayList<>();

        // Output Section 1: Session Info
        Map<String, Object> outputSection1 = new HashMap<>();
        outputSection1.put("header", "Session Information");
        outputSection1.put("condition", "uiOperation === 'create' || uiOperation === 'status' || uiOperation === 'start' || uiOperation === 'stop'");
        List<Map<String, Object>> section1OutputFields = new ArrayList<>();

        // Session ID (for new sessions)
        Map<String, Object> sessionIdOutput = new HashMap<>();
        sessionIdOutput.put("title", "Session ID");
        sessionIdOutput.put("name", "sessionId");
        sessionIdOutput.put("type", "text");
        sessionIdOutput.put("buttons", List.of("copy"));
        sessionIdOutput.put("condition", "uiOperation === 'create'");
        section1OutputFields.add(sessionIdOutput);

        // Status
        Map<String, Object> statusOutput = new HashMap<>();
        statusOutput.put("title", "Status");
        statusOutput.put("name", "status");
        statusOutput.put("type", "text");
        section1OutputFields.add(statusOutput);

        // Elapsed Time
        Map<String, Object> elapsedOutput = new HashMap<>();
        elapsedOutput.put("title", "Elapsed Time");
        elapsedOutput.put("name", "elapsedFormatted");
        elapsedOutput.put("type", "text");
        section1OutputFields.add(elapsedOutput);

        // Start Time
        Map<String, Object> startTimeOutput = new HashMap<>();
        startTimeOutput.put("title", "Start Time");
        startTimeOutput.put("name", "startTime");
        startTimeOutput.put("type", "text");
        startTimeOutput.put("condition", "startTime");
        section1OutputFields.add(startTimeOutput);

        outputSection1.put("fields", section1OutputFields);
        uiOutputs.add(outputSection1);

        // Output Section 2: Lap Info
        Map<String, Object> outputSection2 = new HashMap<>();
        outputSection2.put("header", "Lap Information");
        outputSection2.put("condition", "uiOperation === 'log'");
        List<Map<String, Object>> section2OutputFields = new ArrayList<>();

        section2OutputFields.add(Map.of(
                "title", "Lap Number",
                "name", "lapNumber",
                "type", "text"
        ));

        section2OutputFields.add(Map.of(
                "title", "Lap Time",
                "name", "elapsedFormatted",
                "type", "text"
        ));

        section2OutputFields.add(Map.of(
                "title", "Split Time",
                "name", "splitFormatted",
                "type", "text"
        ));

        outputSection2.put("fields", section2OutputFields);
        uiOutputs.add(outputSection2);

        // Output Section 3: Lap Times Table
        Map<String, Object> outputSection3 = new HashMap<>();
        outputSection3.put("header", "Lap Times");
        outputSection3.put("condition", "uiOperation === 'getLogs' || uiOperation === 'status'");
        List<Map<String, Object>> section3OutputFields = new ArrayList<>();

        Map<String, Object> lapsTableOutput = new HashMap<>();
        lapsTableOutput.put("name", "laps");
        lapsTableOutput.put("type", "table");
        List<Map<String, Object>> lapColumns = new ArrayList<>();
        lapColumns.add(Map.of("header", "Lap", "field", "lapNumber"));
        lapColumns.add(Map.of("header", "Label", "field", "label"));
        lapColumns.add(Map.of("header", "Time", "field", "elapsedFormatted"));
        lapColumns.add(Map.of("header", "Split", "field", "splitFormatted"));
        lapsTableOutput.put("columns", lapColumns);
        section3OutputFields.add(lapsTableOutput);

        outputSection3.put("fields", section3OutputFields);
        uiOutputs.add(outputSection3);

        // Output Section 4: Sessions List
        Map<String, Object> outputSection4 = new HashMap<>();
        outputSection4.put("header", "Active Sessions");
        outputSection4.put("condition", "uiOperation === 'listSessions'");
        List<Map<String, Object>> section4OutputFields = new ArrayList<>();

        Map<String, Object> sessionsTableOutput = new HashMap<>();
        sessionsTableOutput.put("name", "sessions");
        sessionsTableOutput.put("type", "table");
        List<Map<String, Object>> sessionColumns = new ArrayList<>();
        sessionColumns.add(Map.of("header", "Session ID", "field", "sessionId"));
        sessionColumns.add(Map.of("header", "Name", "field", "name"));
        sessionColumns.add(Map.of("header", "Status", "field", "status"));
        sessionColumns.add(Map.of("header", "Elapsed", "field", "elapsedFormatted"));
        sessionColumns.add(Map.of("header", "Laps", "field", "lapCount"));
        sessionsTableOutput.put("columns", sessionColumns);
        section4OutputFields.add(sessionsTableOutput);

        outputSection4.put("fields", section4OutputFields);
        uiOutputs.add(outputSection4);

        uiConfig.put("outputs", uiOutputs);

        // Add the structured uiConfig to the main metadata map
        metadata.put("uiConfig", uiConfig);

        return metadata;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String operation = (String) input.getOrDefault("operation", "status");

            switch (operation.toLowerCase()) {
                case "create":
                    return createSession(input);
                case "start":
                    return startChronometer(input);
                case "stop":
                    return stopChronometer(input);
                case "reset":
                    return resetChronometer(input);
                case "log":
                    return logLapTime(input);
                case "status":
                    return getStatus(input);
                case "getlogs":
                    return getLogs(input);
                case "listsessions":
                    return listSessions();
                case "deletesession":
                    return deleteSession(input);
                default:
                    result.put("error", "Unsupported operation: " + operation);
                    return result;
            }

        } catch (Exception e) {
            result.put("error", "Error processing request: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Create a new chronometer session
     *
     * @param input Input parameters
     * @return Session information
     */
    private Map<String, Object> createSession(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String name = (String) input.getOrDefault("name", "Chronometer Session");
            String sessionId = UUID.randomUUID().toString();

            ChronoSession session = new ChronoSession(sessionId, name);
            activeSessions.put(sessionId, session);

            result.put("success", true);
            result.put("sessionId", sessionId);
            result.put("name", name);
            result.put("created", session.getCreatedTime());
            result.put("status", "created");

        } catch (Exception e) {
            result.put("error", "Error creating session: " + e.getMessage());
        }

        return result;
    }

    /**
     * Start the chronometer
     *
     * @param input Input parameters
     * @return Status information
     */
    private Map<String, Object> startChronometer(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String sessionId = (String) input.get("sessionId");

            if (sessionId == null) {
                result.put("error", "Session ID is required");
                return result;
            }

            ChronoSession session = activeSessions.get(sessionId);
            if (session == null) {
                result.put("error", "Session not found: " + sessionId);
                return result;
            }

            session.start();

            result.put("success", true);
            result.put("sessionId", sessionId);
            result.put("name", session.getName());
            result.put("startTime", session.getStartTime());
            result.put("status", "running");

        } catch (Exception e) {
            result.put("error", "Error starting chronometer: " + e.getMessage());
        }

        return result;
    }

    /**
     * Stop the chronometer
     *
     * @param input Input parameters
     * @return Status information
     */
    private Map<String, Object> stopChronometer(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String sessionId = (String) input.get("sessionId");

            if (sessionId == null) {
                result.put("error", "Session ID is required");
                return result;
            }

            ChronoSession session = activeSessions.get(sessionId);
            if (session == null) {
                result.put("error", "Session not found: " + sessionId);
                return result;
            }

            session.stop();

            long elapsedTime = session.getElapsedTime();
            String elapsedFormatted = formatTime(elapsedTime);

            result.put("success", true);
            result.put("sessionId", sessionId);
            result.put("name", session.getName());
            result.put("startTime", session.getStartTime());
            result.put("stopTime", session.getStopTime());
            result.put("elapsed", elapsedTime);
            result.put("elapsedFormatted", elapsedFormatted);
            result.put("status", "stopped");

        } catch (Exception e) {
            result.put("error", "Error stopping chronometer: " + e.getMessage());
        }

        return result;
    }

    /**
     * Reset the chronometer
     *
     * @param input Input parameters
     * @return Status information
     */
    private Map<String, Object> resetChronometer(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String sessionId = (String) input.get("sessionId");

            if (sessionId == null) {
                result.put("error", "Session ID is required");
                return result;
            }

            ChronoSession session = activeSessions.get(sessionId);
            if (session == null) {
                result.put("error", "Session not found: " + sessionId);
                return result;
            }

            session.reset();

            result.put("success", true);
            result.put("sessionId", sessionId);
            result.put("name", session.getName());
            result.put("status", "reset");

        } catch (Exception e) {
            result.put("error", "Error resetting chronometer: " + e.getMessage());
        }

        return result;
    }

    /**
     * Log a lap time
     *
     * @param input Input parameters
     * @return Lap information
     */
    private Map<String, Object> logLapTime(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String sessionId = (String) input.get("sessionId");
            String label = (String) input.getOrDefault("label", "Lap");

            if (sessionId == null) {
                result.put("error", "Session ID is required");
                return result;
            }

            ChronoSession session = activeSessions.get(sessionId);
            if (session == null) {
                result.put("error", "Session not found: " + sessionId);
                return result;
            }

            ChronoLap lap = session.logLap(label);

            result.put("success", true);
            result.put("sessionId", sessionId);
            result.put("lapId", lap.getLapId());
            result.put("lapNumber", lap.getLapNumber());
            result.put("label", lap.getLabel());
            result.put("timestamp", lap.getTimestamp());
            result.put("elapsed", lap.getElapsedTime());
            result.put("elapsedFormatted", formatTime(lap.getElapsedTime()));
            result.put("splitTime", lap.getSplitTime());
            result.put("splitFormatted", formatTime(lap.getSplitTime()));

        } catch (Exception e) {
            result.put("error", "Error logging lap time: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get current status of the chronometer
     *
     * @param input Input parameters
     * @return Status information
     */
    private Map<String, Object> getStatus(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String sessionId = (String) input.get("sessionId");

            if (sessionId == null) {
                result.put("error", "Session ID is required");
                return result;
            }

            ChronoSession session = activeSessions.get(sessionId);
            if (session == null) {
                result.put("error", "Session not found: " + sessionId);
                return result;
            }

            long elapsedTime = session.getElapsedTime();
            String elapsedFormatted = formatTime(elapsedTime);

            result.put("success", true);
            result.put("sessionId", sessionId);
            result.put("name", session.getName());
            result.put("status", session.getStatus());
            result.put("startTime", session.getStartTime());
            result.put("stopTime", session.getStopTime());
            result.put("elapsed", elapsedTime);
            result.put("elapsedFormatted", elapsedFormatted);
            result.put("lapCount", session.getLapCount());

        } catch (Exception e) {
            result.put("error", "Error getting status: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get all logged lap times
     *
     * @param input Input parameters
     * @return Lap information
     */
    private Map<String, Object> getLogs(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String sessionId = (String) input.get("sessionId");

            if (sessionId == null) {
                result.put("error", "Session ID is required");
                return result;
            }

            ChronoSession session = activeSessions.get(sessionId);
            if (session == null) {
                result.put("error", "Session not found: " + sessionId);
                return result;
            }

            List<Map<String, Object>> laps = new ArrayList<>();

            for (ChronoLap lap : session.getLaps()) {
                Map<String, Object> lapData = new HashMap<>();
                lapData.put("lapId", lap.getLapId());
                lapData.put("lapNumber", lap.getLapNumber());
                lapData.put("label", lap.getLabel());
                lapData.put("timestamp", lap.getTimestamp());
                lapData.put("elapsed", lap.getElapsedTime());
                lapData.put("elapsedFormatted", formatTime(lap.getElapsedTime()));
                lapData.put("splitTime", lap.getSplitTime());
                lapData.put("splitFormatted", formatTime(lap.getSplitTime()));

                laps.add(lapData);
            }

            result.put("success", true);
            result.put("sessionId", sessionId);
            result.put("name", session.getName());
            result.put("laps", laps);
            result.put("lapCount", laps.size());

        } catch (Exception e) {
            result.put("error", "Error getting logs: " + e.getMessage());
        }

        return result;
    }

    /**
     * List all active chronometer sessions
     *
     * @return List of active sessions
     */
    private Map<String, Object> listSessions() {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Map<String, Object>> sessions = new ArrayList<>();

            for (ChronoSession session : activeSessions.values()) {
                Map<String, Object> sessionData = new HashMap<>();
                sessionData.put("sessionId", session.getSessionId());
                sessionData.put("name", session.getName());
                sessionData.put("status", session.getStatus());
                sessionData.put("startTime", session.getStartTime());
                sessionData.put("stopTime", session.getStopTime());
                sessionData.put("elapsed", session.getElapsedTime());
                sessionData.put("elapsedFormatted", formatTime(session.getElapsedTime()));
                sessionData.put("lapCount", session.getLapCount());

                sessions.add(sessionData);
            }

            result.put("success", true);
            result.put("sessions", sessions);
            result.put("count", sessions.size());

        } catch (Exception e) {
            result.put("error", "Error listing sessions: " + e.getMessage());
        }

        return result;
    }

    /**
     * Delete a chronometer session
     *
     * @param input Input parameters
     * @return Status information
     */
    private Map<String, Object> deleteSession(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String sessionId = (String) input.get("sessionId");

            if (sessionId == null) {
                result.put("error", "Session ID is required");
                return result;
            }

            ChronoSession session = activeSessions.remove(sessionId);
            if (session == null) {
                result.put("error", "Session not found: " + sessionId);
                return result;
            }

            result.put("success", true);
            result.put("sessionId", sessionId);
            result.put("message", "Session deleted successfully");

        } catch (Exception e) {
            result.put("error", "Error deleting session: " + e.getMessage());
        }

        return result;
    }

    /**
     * Format time in milliseconds to a readable string (HH:MM:SS.mmm)
     *
     * @param timeInMillis Time in milliseconds
     * @return Formatted time string
     */
    private String formatTime(long timeInMillis) {
        if (timeInMillis < 0) {
            return "00:00:00.000";
        }

        return TIME_FORMAT.format(new Date(timeInMillis));
    }

    /**
     * Inner class representing a chronometer session
     */
    private static class ChronoSession {
        private final String sessionId;
        private final String name;
        private final long createdTime;

        private long startTime = 0;
        private long stopTime = 0;
        private long elapsedTime = 0;
        private boolean running = false;

        private final List<ChronoLap> laps = new ArrayList<>();
        private long lastLapTime = 0;

        public ChronoSession(String sessionId, String name) {
            this.sessionId = sessionId;
            this.name = name;
            this.createdTime = System.currentTimeMillis();
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getName() {
            return name;
        }

        public long getCreatedTime() {
            return createdTime;
        }

        public void start() {
            if (!running) {
                startTime = System.currentTimeMillis();
                lastLapTime = startTime;
                running = true;
            }
        }

        public void stop() {
            if (running) {
                stopTime = System.currentTimeMillis();
                elapsedTime += (stopTime - startTime);
                running = false;
            }
        }

        public void reset() {
            startTime = 0;
            stopTime = 0;
            elapsedTime = 0;
            running = false;
            laps.clear();
            lastLapTime = 0;
        }

        public ChronoLap logLap(String label) {
            long now = System.currentTimeMillis();
            long lapTime = now;
            long elapsed = getElapsedTime();

            // If not running, use the stored elapsed time
            if (!running) {
                lapTime = stopTime > 0 ? stopTime : now;
            }

            // Calculate split time (time since last lap)
            long splitTime = lapTime - lastLapTime;
            lastLapTime = lapTime;

            ChronoLap lap = new ChronoLap(laps.size() + 1, label, lapTime, elapsed, splitTime);
            laps.add(lap);

            return lap;
        }

        public long getElapsedTime() {
            if (running) {
                return elapsedTime + (System.currentTimeMillis() - startTime);
            } else {
                return elapsedTime;
            }
        }

        public String getStatus() {
            if (running) {
                return "running";
            } else if (stopTime > 0) {
                return "stopped";
            } else {
                return "reset";
            }
        }

        public long getStartTime() {
            return startTime;
        }

        public long getStopTime() {
            return stopTime;
        }

        public int getLapCount() {
            return laps.size();
        }

        public List<ChronoLap> getLaps() {
            return new ArrayList<>(laps);
        }
    }

    /**
     * Inner class representing a lap time
     */
    private static class ChronoLap {
        private final String lapId;
        private final int lapNumber;
        private final String label;
        private final long timestamp;
        private final long elapsedTime;
        private final long splitTime;

        public ChronoLap(int lapNumber, String label, long timestamp, long elapsedTime, long splitTime) {
            this.lapId = UUID.randomUUID().toString();
            this.lapNumber = lapNumber;
            this.label = label;
            this.timestamp = timestamp;
            this.elapsedTime = elapsedTime;
            this.splitTime = splitTime;
        }

        public String getLapId() {
            return lapId;
        }

        public int getLapNumber() {
            return lapNumber;
        }

        public String getLabel() {
            return label;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public long getElapsedTime() {
            return elapsedTime;
        }

        public long getSplitTime() {
            return splitTime;
        }
    }
}