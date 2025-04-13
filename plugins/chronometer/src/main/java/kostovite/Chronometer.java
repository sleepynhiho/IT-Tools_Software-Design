package kostovite;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// Assuming PluginInterface is standard
public class Chronometer implements PluginInterface {

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");
    private static final TimeZone UTC_ZONE = TimeZone.getTimeZone("UTC");
    private static final Map<String, ChronoSession> activeSessions = new ConcurrentHashMap<>();

    static {
        TIME_FORMAT.setTimeZone(UTC_ZONE);
    }

    /**
     * Internal name, should match the class for routing.
     * The user-facing name comes from the metadata.
     */
    @Override
    public String getName() {
        return "Chronometer";
    }

    /**
     * Standalone execution for testing.
     */
    @Override
    public void execute() {
        System.out.println("Chronometer Plugin executed (standalone test)");
        // Example using new IDs
        try {
            Map<String, Object> createParams = new HashMap<>();
            createParams.put("uiOperation", "create");
            createParams.put("sessionName", "Test Session");
            Map<String, Object> createResult = process(createParams);
            System.out.println("Create Result: " + createResult);

            if (createResult.get("success") == Boolean.TRUE) {
                String sessionId = (String) createResult.get("sessionId");

                Map<String, Object> startParams = Map.of("uiOperation", "start", "sessionId", sessionId);
                System.out.println("Start Result: " + process(startParams));

                Thread.sleep(1100);

                Map<String, Object> logParams = Map.of("uiOperation", "log", "sessionId", sessionId, "lapLabel", "Checkpoint 1");
                System.out.println("Log Result: " + process(logParams));

                Thread.sleep(1200);

                Map<String, Object> statusParams = Map.of("uiOperation", "status", "sessionId", sessionId);
                System.out.println("Status Result: " + process(statusParams));

                Map<String, Object> stopParams = Map.of("uiOperation", "stop", "sessionId", sessionId);
                System.out.println("Stop Result: " + process(stopParams));

                Map<String, Object> logsParams = Map.of("uiOperation", "getLogs", "sessionId", sessionId);
                System.out.println("Get Logs Result: " + process(logsParams));

                Map<String, Object> listParams = Map.of("uiOperation", "listSessions");
                System.out.println("List Sessions Result: " + process(listParams));

                Map<String, Object> deleteParams = Map.of("uiOperation", "deleteSession", "sessionId", sessionId);
                System.out.println("Delete Result: " + process(deleteParams));

            }
        } catch (Exception e) {
            System.err.println("Standalone test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generates metadata in the NEW format (sections, id, etc.).
     */
    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        // --- Top Level Attributes (New Format) ---
        metadata.put("id", "Chronometer"); // ID matches class name for routing
        metadata.put("name", "Chronometer"); // User-facing name
        metadata.put("description", "A multi-session stopwatch with lap timing.");
        metadata.put("icon", "Timer"); // Material Icon name
        metadata.put("category", "Utilities");
        metadata.put("customUI", false);
        // Chronometer isn't suitable for dynamic updates on every input change
        metadata.put("triggerUpdateOnChange", false);

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section 1: Action and Session Details ---
        Map<String, Object> actionSection = new HashMap<>();
        actionSection.put("id", "action");
        actionSection.put("label", "Chronometer Control");

        List<Map<String, Object>> actionInputs = new ArrayList<>();

        // Operation Selection Dropdown
        actionInputs.add(Map.ofEntries(
                Map.entry("id", "uiOperation"), // Use ID
                Map.entry("label", "Select Action:"),
                Map.entry("type", "select"),
                Map.entry("options", List.of( // Options for the select
                        Map.of("value", "create", "label", "Create New Session"),
                        Map.of("value", "listSessions", "label", "List Active Sessions"),
                        Map.of("value", "start", "label", "Start Timer"),
                        Map.of("value", "stop", "label", "Stop Timer"),
                        Map.of("value", "log", "label", "Log Lap Time"),
                        Map.of("value", "status", "label", "Get Current Status"),
                        Map.of("value", "getLogs", "label", "Get All Lap Times"),
                        Map.of("value", "reset", "label", "Reset Timer"),
                        Map.of("value", "deleteSession", "label", "Delete Session")
                )),
                Map.entry("default", "create"),
                Map.entry("required", true),
                Map.entry("helperText", "Choose the chronometer action.")
        ));

        // Session Name Input (Conditional)
        actionInputs.add(Map.ofEntries(
                Map.entry("id", "sessionName"), // Use ID
                Map.entry("label", "New Session Name:"),
                Map.entry("type", "text"),
                Map.entry("default", "My Session"),
                Map.entry("required", true), // Required only for 'create'
                Map.entry("condition", "uiOperation === 'create'"), // Condition based on ID
                Map.entry("placeholder", "Enter name for the new session")
        ));

        // Session ID Input (Conditional)
        actionInputs.add(Map.ofEntries(
                Map.entry("id", "sessionId"), // Use ID
                Map.entry("label", "Session ID:"),
                Map.entry("type", "text"),
                // Required is dynamically checked in process based on operation
                Map.entry("required", false), // Set false here, handle logic in backend
                Map.entry("condition", "uiOperation !== 'create' && uiOperation !== 'listSessions'"), // Condition based on ID
                Map.entry("placeholder", "Enter existing Session ID"),
                Map.entry("helperText", "Required for actions on existing sessions.")
        ));

        // Lap Label Input (Conditional)
        actionInputs.add(Map.ofEntries(
                Map.entry("id", "lapLabel"), // Use ID
                Map.entry("label", "Lap Label (Optional):"),
                Map.entry("type", "text"),
                Map.entry("required", false),
                Map.entry("condition", "uiOperation === 'log'"), // Condition based on ID
                Map.entry("placeholder", "e.g., Checkpoint 1")
        ));

        actionSection.put("inputs", actionInputs);
        sections.add(actionSection);


        // --- Section 2: Results Display ---
        Map<String, Object> resultsSection = new HashMap<>();
        resultsSection.put("id", "results");
        resultsSection.put("label", "Results");
        // Conditionally show this whole section only on success? Or let individual fields handle it?
        // Let individual fields handle it via their conditions for more flexibility.

        List<Map<String, Object>> resultOutputs = new ArrayList<>();

        // Session ID Output (on create)
        resultOutputs.add(Map.ofEntries(
                Map.entry("id", "sessionId"), // Matches key in response map
                Map.entry("label", "New Session ID"),
                Map.entry("type", "text"),
                Map.entry("buttons", List.of("copy")),
                Map.entry("condition", "typeof sessionId !== 'undefined' && uiOperation === 'create'") // Show only if defined AND was create op
        ));

        // Status Output
        resultOutputs.add(Map.ofEntries(
                Map.entry("id", "status"), // Matches key in response map
                Map.entry("label", "Status"),
                Map.entry("type", "text"),
                Map.entry("condition", "typeof status !== 'undefined'") // Show if status exists
        ));

        // Message Output (for reset/delete)
        resultOutputs.add(Map.ofEntries(
                Map.entry("id", "message"), // Matches key in response map
                Map.entry("label", "Info"),
                Map.entry("type", "text"),
                Map.entry("condition", "typeof message !== 'undefined'") // Show if message exists
        ));

        // Elapsed Time Output
        resultOutputs.add(Map.ofEntries(
                Map.entry("id", "elapsedFormatted"), // Matches key in response map
                Map.entry("label", "Elapsed Time"),
                Map.entry("type", "text"),
                Map.entry("monospace", true), // Good for time
                Map.entry("condition", "typeof elapsedFormatted !== 'undefined'") // Show if elapsed exists
        ));

        // Lap Logged Details (Conditional on log operation success)
        resultOutputs.add(Map.ofEntries(
                Map.entry("id", "lapNumber"), // Matches key in response map
                Map.entry("label", "Lap Number Logged"),
                Map.entry("type", "text"),
                Map.entry("condition", "typeof lapNumber !== 'undefined'") // Show if lapNumber exists (indicates log op)
        ));
        resultOutputs.add(Map.ofEntries(
                Map.entry("id", "lapLoggedLabel"), // Use distinct ID if 'label' used elsewhere
                Map.entry("label", "Lap Label"),
                Map.entry("type", "text"),
                Map.entry("condition", "typeof lapNumber !== 'undefined' && typeof lapLoggedLabel !== 'undefined' && lapLoggedLabel !== ''") // Show if log op & label exists
        ));
        resultOutputs.add(Map.ofEntries(
                Map.entry("id", "lapElapsedFormatted"), // Distinct ID
                Map.entry("label", "Lap Total Time"),
                Map.entry("type", "text"),
                Map.entry("monospace", true),
                Map.entry("condition", "typeof lapNumber !== 'undefined'") // Show if log op
        ));
        resultOutputs.add(Map.ofEntries(
                Map.entry("id", "splitFormatted"), // Matches key in response map
                Map.entry("label", "Lap Split Time"),
                Map.entry("type", "text"),
                Map.entry("monospace", true),
                Map.entry("condition", "typeof lapNumber !== 'undefined'") // Show if log op
        ));

        // Laps Table Output (Conditional on getLogs operation success)
        Map<String, Object> lapsTable = new HashMap<>();
        lapsTable.put("id", "laps"); // Matches key in response map (containing the array)
        lapsTable.put("label", "Lap Times");
        lapsTable.put("type", "table");
        lapsTable.put("condition", "typeof laps !== 'undefined'"); // Show if laps array exists
        lapsTable.put("columns", List.of( // Define table columns
                Map.of("header", "Lap #", "field", "lapNumber"),
                Map.of("header", "Label", "field", "label"),
                Map.of("header", "Timestamp", "field", "elapsedFormatted"),
                Map.of("header", "Split", "field", "splitFormatted")
        ));
        resultOutputs.add(lapsTable);

        // Sessions Table Output (Conditional on listSessions operation success)
        Map<String, Object> sessionsTable = new HashMap<>();
        sessionsTable.put("id", "sessions"); // Matches key in response map
        sessionsTable.put("label", "Active Sessions");
        sessionsTable.put("type", "table");
        sessionsTable.put("condition", "typeof sessions !== 'undefined'"); // Show if sessions array exists
        sessionsTable.put("columns", List.of(
                Map.of("header", "Session ID", "field", "sessionId"),
                Map.of("header", "Name", "field", "name"),
                Map.of("header", "Status", "field", "status"),
                Map.of("header", "Elapsed", "field", "elapsedFormatted"),
                Map.of("header", "Laps", "field", "lapCount")
        ));
        resultOutputs.add(sessionsTable);


        resultsSection.put("outputs", resultOutputs);
        sections.add(resultsSection);


        // --- Section 3: Error Display ---
        Map<String, Object> errorSection = new HashMap<>();
        errorSection.put("id", "errorDisplay");
        errorSection.put("label", "Error");
        errorSection.put("condition", "success === false"); // Show only on failure

        List<Map<String, Object>> errorOutputs = new ArrayList<>();
        errorOutputs.add(Map.ofEntries(
                Map.entry("id", "errorMessage"), // Specific ID for the error message
                Map.entry("label", "Details"),
                Map.entry("type", "text"),
                Map.entry("style", "error") // Hint for styling
        ));
        errorSection.put("outputs", errorOutputs);
        sections.add(errorSection);


        // Add sections list to the main metadata map
        metadata.put("sections", sections);

        // Remove old structures if they existed

        return metadata;
    }


    /**
     * Processes the input parameters (using IDs from the new format)
     * to perform chronometer actions.
     */
    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        // Read the operation using the ID defined in UI metadata
        String uiOperation = (String) input.get("uiOperation");
        String errorOutputId = "errorMessage"; // Matches the error output field ID

        if (uiOperation == null || uiOperation.isBlank()) {
            return Map.of("success", false, errorOutputId, "No operation specified.");
        }

        // Include the operation in the input map for potential use in output conditions
        // Note: Modifying the input map directly might be undesirable in some contexts.
        // Consider passing it separately if needed.
        Map<String, Object> processingInput = new HashMap<>(input); // Create a mutable copy
        processingInput.put("uiOperation", uiOperation); // Ensure it's there


        try {
            Map<String, Object> result;
            // Route to the correct private method based on the UI operation
            switch (uiOperation.toLowerCase()) {
                case "create" -> result = createSession(processingInput);
                case "start" -> result = startChronometer(processingInput);
                case "stop" -> result = stopChronometer(processingInput);
                case "reset" -> result = resetChronometer(processingInput);
                case "log" -> result = logLapTime(processingInput);
                case "status" -> result = getStatus(processingInput);
                case "getlogs" -> result = getLogs(processingInput);
                case "listsessions" -> result = listSessions(); // No input map needed for list
                case "deletesession" -> result = deleteSession(processingInput);
                default -> {
                    return Map.of("success", false, errorOutputId, "Unsupported operation: " + uiOperation);
                }
            }

            // Add the original operation back into the result map
            // so output conditions like "typeof lapNumber !== 'undefined'" work correctly
            // AND we can still differentiate results if needed (e.g., condition="uiOperation==='create'")
            // Make a mutable copy before modifying
            if (result.get("success") == Boolean.TRUE) {
                Map<String, Object> finalResult = new HashMap<>(result);
                finalResult.put("uiOperation", uiOperation); // Add operation context to success response
                return finalResult;
            } else {
                // If the helper method returned an error, ensure it uses the correct error key
                if (result.containsKey("error") && !result.containsKey(errorOutputId)) {
                    Map<String, Object> finalResult = new HashMap<>(result);
                    finalResult.put(errorOutputId, result.get("error"));
                    finalResult.remove("error"); // Remove the old key
                    return finalResult;
                }
                return result; // Return error as is if already using correct key
            }


        } catch (IllegalArgumentException e) { // Catch specific expected errors
            return Map.of("success", false, errorOutputId, e.getMessage());
        } catch (Exception e) { // Catch unexpected errors
            System.err.println("Error processing chronometer request: " + e.getMessage());
            e.printStackTrace(); // Log stack trace for debugging
            return Map.of("success", false, errorOutputId, "An internal server error occurred: " + e.getMessage());
        }
    }

    // ========================================================================
    // Private Helper Methods for Operations
    // (Ensure returned map keys match NEW output field IDs)
    // ========================================================================

    private Map<String, Object> createSession(Map<String, Object> input) {
        // Extract parameter using the ID defined in metadata
        String name = (String) input.getOrDefault("sessionName", "Chronometer Session");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Session name cannot be empty.");
        }

        String sessionId = UUID.randomUUID().toString();
        ChronoSession session = new ChronoSession(sessionId, name);
        activeSessions.put(sessionId, session);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("sessionId", sessionId); // Matches output ID "sessionId"
        result.put("name", name);           // Matches sessions table field "name"
        result.put("status", "created");    // Matches output ID "status"

        return result;
    }

    private ChronoSession getSessionOrThrow(Map<String, Object> input, String action) {
        // Read session ID using the ID from metadata
        String sessionId = (String) input.get("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            // Check if the action actually requires a session ID
            // ListSessions does not, Create does not.
            List<String> noIdNeeded = List.of("create", "listsessions");
            if (!noIdNeeded.contains(action.toLowerCase())) {
                throw new IllegalArgumentException("Session ID is required for '" + action + "' operation.");
            }
            return null; // Return null if ID not needed for this action
        }
        // If ID was provided (or required and provided), try to get the session
        ChronoSession session = activeSessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        return session;
    }

    private Map<String, Object> startChronometer(Map<String, Object> input) {
        ChronoSession session = getSessionOrThrow(input, "start");
        assert session != null;
        session.start();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        // result.put("sessionId", session.getSessionId()); // Optional, not a dedicated output field
        result.put("status", "running");                 // Matches output ID "status"
        result.put("elapsedFormatted", formatTime(session.getElapsedTime())); // Update elapsed time

        return result;
    }

    private Map<String, Object> stopChronometer(Map<String, Object> input) {
        ChronoSession session = getSessionOrThrow(input, "stop");
        assert session != null;
        session.stop();

        long elapsedTime = session.getElapsedTime();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("status", "stopped");                 // Matches output ID "status"
        result.put("elapsedFormatted", formatTime(elapsedTime)); // Matches output ID "elapsedFormatted"

        return result;
    }

    private Map<String, Object> resetChronometer(Map<String, Object> input) {
        ChronoSession session = getSessionOrThrow(input, "reset");
        assert session != null;
        session.reset();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("status", "reset"); // Matches output ID "status"
        result.put("message", "Chronometer reset successfully."); // Matches output ID "message"

        return result;
    }

    private Map<String, Object> logLapTime(Map<String, Object> input) {
        ChronoSession session = getSessionOrThrow(input, "log");
        // Extract optional label using the ID defined in metadata
        String label = (String) input.getOrDefault("lapLabel", ""); // Use ID "lapLabel"

        assert session != null;
        ChronoLap lap = session.logLap(label);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        // Fields match output IDs for lap details
        result.put("lapNumber", lap.getLapNumber()); // Matches output ID "lapNumber"
        if (!lap.getLabel().isEmpty()) {
            result.put("lapLoggedLabel", lap.getLabel()); // Matches output ID "lapLoggedLabel"
        }
        result.put("lapElapsedFormatted", formatTime(lap.getElapsedTime())); // Matches output ID "lapElapsedFormatted"
        result.put("splitFormatted", formatTime(lap.getSplitTime())); // Matches output ID "splitFormatted"

        return result;
    }

    private Map<String, Object> getStatus(Map<String, Object> input) {
        ChronoSession session = getSessionOrThrow(input, "status");
        assert session != null;
        long elapsedTime = session.getElapsedTime();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("status", session.getStatus()); // Matches output ID "status"
        result.put("elapsedFormatted", formatTime(elapsedTime)); // Matches output ID "elapsedFormatted"
        // Include lap data if needed by a table shown alongside status
        result.put("laps", formatLapsForTable(session)); // Matches output ID "laps"
        result.put("lapCount", session.getLapCount()); // Matches sessions table field "lapCount"

        return result;
    }

    private Map<String, Object> getLogs(Map<String, Object> input) {
        ChronoSession session = getSessionOrThrow(input, "getLogs");

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        assert session != null;
        result.put("laps", formatLapsForTable(session)); // Matches output ID "laps"

        return result;
    }

    // Helper to format laps consistently for tables
    private List<Map<String, Object>> formatLapsForTable(ChronoSession session) {
        return session.getLaps().stream()
                .map(lap -> Map.<String, Object>of(
                        "lapNumber", lap.getLapNumber(),           // Matches table column field
                        "label", lap.getLabel(),                   // Matches table column field
                        "elapsedFormatted", formatTime(lap.getElapsedTime()), // Matches table column field
                        "splitFormatted", formatTime(lap.getSplitTime())      // Matches table column field
                ))
                .collect(Collectors.toList());
    }

    private Map<String, Object> listSessions() {
        List<Map<String, Object>> sessionsData = activeSessions.values().stream()
                .map(session -> Map.<String, Object>of(
                        "sessionId", session.getSessionId(),           // Matches table column field
                        "name", session.getName(),                     // Matches table column field
                        "status", session.getStatus(),                 // Matches table column field
                        "elapsedFormatted", formatTime(session.getElapsedTime()), // Matches table column field
                        "lapCount", session.getLapCount()              // Matches table column field
                ))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("sessions", sessionsData); // Matches output ID "sessions"

        return result;
    }

    private Map<String, Object> deleteSession(Map<String, Object> input) {
        ChronoSession removedSession = getSessionOrThrow(input, "deleteSession");
        assert removedSession != null;
        activeSessions.remove(removedSession.getSessionId());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Session '" + removedSession.getName() + "' deleted."); // Matches output ID "message"

        return result;
    }

    /**
     * Format time in milliseconds to HH:MM:SS.mmm using UTC.
     * This method is synchronized for thread safety of SimpleDateFormat.
     */
    private String formatTime(long timeInMillis) {
        if (timeInMillis < 0) return "00:00:00.000";
        synchronized (TIME_FORMAT) {
            return TIME_FORMAT.format(new Date(timeInMillis));
        }
    }

    // --- Inner Classes (ChronoSession, ChronoLap) ---
    // (Keep these as they are, no changes needed here)
    private static class ChronoSession {
        private final String sessionId;
        private final String name;
        private final long createdTime;
        private long startTime = 0;
        private long stopTime = 0;
        private long accumulatedElapsedTime = 0;
        private boolean running = false;
        private final List<ChronoLap> laps = Collections.synchronizedList(new ArrayList<>()); // Use synchronized list
        private long lastEventTime;

        public ChronoSession(String sessionId, String name) {
            this.sessionId = sessionId;
            this.name = name;
            this.createdTime = System.currentTimeMillis();
            this.lastEventTime = this.createdTime;
        }
        public String getSessionId() { return sessionId; }
        public String getName() { return name; }
        public long getCreatedTime() { return createdTime; }

        // Make methods synchronized to protect state transitions
        public synchronized void start() {
            if (!running) {
                startTime = System.currentTimeMillis();
                stopTime = 0;
                running = true;
                lastEventTime = startTime;
            }
        }
        public synchronized void stop() {
            if (running) {
                stopTime = System.currentTimeMillis();
                accumulatedElapsedTime += (stopTime - startTime);
                running = false;
                startTime = 0;
            }
        }
        public synchronized void reset() {
            startTime = 0;
            stopTime = 0;
            accumulatedElapsedTime = 0;
            running = false;
            laps.clear();
            lastEventTime = 0;
        }
        public synchronized ChronoLap logLap(String label) {
            if (!running && stopTime == 0 && startTime == 0) {
                throw new IllegalStateException("Chronometer must be running or stopped (after running) to log a lap.");
            }
            long now = running ? System.currentTimeMillis() : stopTime;
            long currentTotalElapsed = getElapsedTime();
            long splitTime = now - lastEventTime;
            lastEventTime = now;
            ChronoLap lap = new ChronoLap(laps.size() + 1, label, now, currentTotalElapsed, splitTime);
            laps.add(lap);
            return lap;
        }
        public synchronized long getElapsedTime() {
            if (running) {
                return accumulatedElapsedTime + (System.currentTimeMillis() - startTime);
            } else {
                return accumulatedElapsedTime;
            }
        }
        public String getStatus() { // Status check doesn't modify state, less critical to synchronize fully
            if (running) return "running";
            if (accumulatedElapsedTime > 0 || stopTime > 0) return "stopped";
            if (startTime == 0 && stopTime == 0 && accumulatedElapsedTime == 0) return "reset";
            return "created";
        }
        public long getStartTime() { return startTime; }
        public long getStopTime() { return stopTime; }
        public int getLapCount() { return laps.size(); }
        public List<ChronoLap> getLaps() { return new ArrayList<>(laps); } // Return copy
    }

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
            this.label = (label == null || label.isBlank()) ? "" : label; // Store empty string if blank
            this.timestamp = timestamp;
            this.elapsedTime = elapsedTime;
            this.splitTime = splitTime;
        }
        public String getLapId() { return lapId; }
        public int getLapNumber() { return lapNumber; }
        public String getLabel() { return label; }
        public long getTimestamp() { return timestamp; }
        public long getElapsedTime() { return elapsedTime; }
        public long getSplitTime() { return splitTime; }
    }
}