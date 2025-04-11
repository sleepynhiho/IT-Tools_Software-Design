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
        metadata.put("name", getName());
        metadata.put("version", "1.0.0");
        metadata.put("description", "Monitor the duration of events with a chronometer");

        // Define available operations
        Map<String, Object> operations = new HashMap<>();

        // Create session operation
        Map<String, Object> createOperation = new HashMap<>();
        createOperation.put("description", "Create a new chronometer session");

        Map<String, Object> createInputs = new HashMap<>();
        createInputs.put("name", "Name of the session (optional)");

        createOperation.put("inputs", createInputs);
        operations.put("create", createOperation);

        // Start operation
        Map<String, Object> startOperation = new HashMap<>();
        startOperation.put("description", "Start the chronometer");

        Map<String, Object> startInputs = new HashMap<>();
        startInputs.put("sessionId", "ID of the chronometer session");

        startOperation.put("inputs", startInputs);
        operations.put("start", startOperation);

        // Stop operation
        Map<String, Object> stopOperation = new HashMap<>();
        stopOperation.put("description", "Stop the chronometer");

        Map<String, Object> stopInputs = new HashMap<>();
        stopInputs.put("sessionId", "ID of the chronometer session");

        stopOperation.put("inputs", stopInputs);
        operations.put("stop", stopOperation);

        // Reset operation
        Map<String, Object> resetOperation = new HashMap<>();
        resetOperation.put("description", "Reset the chronometer");

        Map<String, Object> resetInputs = new HashMap<>();
        resetInputs.put("sessionId", "ID of the chronometer session");

        resetOperation.put("inputs", resetInputs);
        operations.put("reset", resetOperation);

        // Log time operation
        Map<String, Object> logOperation = new HashMap<>();
        logOperation.put("description", "Log a lap time");

        Map<String, Object> logInputs = new HashMap<>();
        logInputs.put("sessionId", "ID of the chronometer session");
        logInputs.put("label", "Label for the lap time (optional)");

        logOperation.put("inputs", logInputs);
        operations.put("log", logOperation);

        // Get status operation
        Map<String, Object> statusOperation = new HashMap<>();
        statusOperation.put("description", "Get the current status of the chronometer");

        Map<String, Object> statusInputs = new HashMap<>();
        statusInputs.put("sessionId", "ID of the chronometer session");

        statusOperation.put("inputs", statusInputs);
        operations.put("status", statusOperation);

        // Get logs operation
        Map<String, Object> getLogsOperation = new HashMap<>();
        getLogsOperation.put("description", "Get all logged lap times");

        Map<String, Object> getLogsInputs = new HashMap<>();
        getLogsInputs.put("sessionId", "ID of the chronometer session");

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
        deleteSessionInputs.put("sessionId", "ID of the chronometer session");

        deleteSessionOperation.put("inputs", deleteSessionInputs);
        operations.put("deleteSession", deleteSessionOperation);

        metadata.put("operations", operations);
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