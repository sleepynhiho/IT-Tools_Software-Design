package kostovite;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Chronometer implements PluginInterface {

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");
    private static final TimeZone UTC_ZONE = TimeZone.getTimeZone("UTC");
    private static final Map<String, ChronoSession> activeSessions = new ConcurrentHashMap<>();

    static {
        TIME_FORMAT.setTimeZone(UTC_ZONE);
    }

    @Override
    public String getName() {
        return "Chronometer";
    }

    @Override
    public void execute() {
        System.out.println("Chronometer Plugin executed (standalone test)");
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

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        // Top Level Attributes
        metadata.put("id", "Chronometer");
        metadata.put("name", "Chronometer");
        metadata.put("description", "A multi-session stopwatch with lap timing.");
        metadata.put("icon", "Timer");
        metadata.put("category", "Utilities");
        metadata.put("customUI", false);
        metadata.put("triggerUpdateOnChange", false);

        // Sections
        List<Map<String, Object>> sections = new ArrayList<>();

        // Section 1: Action and Session Details
        Map<String, Object> actionSection = new HashMap<>();
        actionSection.put("id", "action");
        actionSection.put("label", "Chronometer Control");

        List<Map<String, Object>> actionInputs = new ArrayList<>();

        // Operation Selection Dropdown
        actionInputs.add(Map.ofEntries(
                Map.entry("id", "uiOperation"),
                Map.entry("label", "Select Action:"),
                Map.entry("type", "select"),
                Map.entry("options", List.of(
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
                Map.entry("id", "sessionName"),
                Map.entry("label", "New Session Name:"),
                Map.entry("type", "text"),
                Map.entry("default", "My Session"),
                Map.entry("required", true),
                Map.entry("condition", "uiOperation === 'create'"),
                Map.entry("placeholder", "Enter name for the new session")
        ));

        // Session ID Input (Conditional)
        actionInputs.add(Map.ofEntries(
                Map.entry("id", "sessionId"),
                Map.entry("label", "Session ID:"),
                Map.entry("type", "text"),
                Map.entry("required", false),
                Map.entry("condition", "uiOperation !== 'create' && uiOperation !== 'listSessions'"),
                Map.entry("placeholder", "Enter existing Session ID"),
                Map.entry("helperText", "Required for actions on existing sessions.")
        ));

        // Lap Label Input (Conditional)
        actionInputs.add(Map.ofEntries(
                Map.entry("id", "lapLabel"),
                Map.entry("label", "Lap Label (Optional):"),
                Map.entry("type", "text"),
                Map.entry("required", false),
                Map.entry("condition", "uiOperation === 'log'"),
                Map.entry("placeholder", "e.g., Checkpoint 1")
        ));

        actionSection.put("inputs", actionInputs);
        sections.add(actionSection);

        // Section 2: Results Display
        Map<String, Object> resultsSection = new HashMap<>();
        resultsSection.put("id", "results");
        resultsSection.put("label", "Results");

        List<Map<String, Object>> resultOutputs = new ArrayList<>();

        // Session ID Output (on create)
        resultOutputs.add(Map.ofEntries(
                Map.entry("id", "sessionId"),
                Map.entry("label", "New Session ID"),
                Map.entry("type", "text"),
                Map.entry("buttons", List.of("copy"))
        ));

        // Status Output
        resultOutputs.add(Map.ofEntries(
                Map.entry("id", "status"),
                Map.entry("label", "Status"),
                Map.entry("type", "text")
        ));

        // Message Output (for reset/delete)
        resultOutputs.add(Map.ofEntries(
                Map.entry("id", "message"),
                Map.entry("label", "Info"),
                Map.entry("type", "text")
        ));

        // Elapsed Time Output
        resultOutputs.add(Map.ofEntries(
                Map.entry("id", "elapsedFormatted"),
                Map.entry("label", "Elapsed Time"),
                Map.entry("type", "text"),
                Map.entry("monospace", true)
        ));

        // Lap Logged Details
        resultOutputs.add(Map.ofEntries(
                Map.entry("id", "lapNumber"),
                Map.entry("label", "Lap Number Logged"),
                Map.entry("type", "text")
        ));
        resultOutputs.add(Map.ofEntries(
                Map.entry("id", "lapLoggedLabel"),
                Map.entry("label", "Lap Label"),
                Map.entry("type", "text")
        ));
        resultOutputs.add(Map.ofEntries(
                Map.entry("id", "lapElapsedFormatted"),
                Map.entry("label", "Lap Total Time"),
                Map.entry("type", "text"),
                Map.entry("monospace", true)
        ));
        resultOutputs.add(Map.ofEntries(
                Map.entry("id", "splitFormatted"),
                Map.entry("label", "Lap Split Time"),
                Map.entry("type", "text"),
                Map.entry("monospace", true)
        ));

        // Laps Table Output
        Map<String, Object> lapsTable = new HashMap<>();
        lapsTable.put("id", "laps");
        lapsTable.put("label", "Lap Times");
        lapsTable.put("type", "table");
        lapsTable.put("columns", List.of(
                Map.of("header", "Lap #", "field", "lapNumber"),
                Map.of("header", "Label", "field", "label"),
                Map.of("header", "Timestamp", "field", "elapsedFormatted"),
                Map.of("header", "Split", "field", "splitFormatted")
        ));
        resultOutputs.add(lapsTable);

        // Sessions Table Output
        Map<String, Object> sessionsTable = new HashMap<>();
        sessionsTable.put("id", "sessions");
        sessionsTable.put("label", "Active Sessions");
        sessionsTable.put("type", "table");
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

        metadata.put("sections", sections);
        return metadata;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        // Read the operation
        String uiOperation = (String) input.get("uiOperation");
        Map<String, Object> result = new HashMap<>();
        result.put("success", true); // Assume success

        if (uiOperation == null || uiOperation.isBlank()) {
            result.put("success", false);
            result.put("error", "No operation specified.");
            return result;
        }

        try {
            // Route to the correct private method based on the UI operation
            switch (uiOperation.toLowerCase()) {
                case "create" -> {
                    String name = (String) input.getOrDefault("sessionName", "Chronometer Session");
                    if (name == null || name.isBlank()) {
                        name = "Chronometer Session";
                    }

                    String sessionId = UUID.randomUUID().toString();
                    ChronoSession session = new ChronoSession(sessionId, name);
                    activeSessions.put(sessionId, session);

                    result.put("sessionId", sessionId);
                    result.put("name", name);
                    result.put("status", "created");
                }
                case "start" -> {
                    ChronoSession session = getSessionSafely(input);
                    if (session != null) {
                        session.start();
                        result.put("status", "running");
                        result.put("elapsedFormatted", formatTime(session.getElapsedTime()));
                    } else {
                        result.put("status", "error");
                        result.put("message", "Session not found");
                    }
                }
                case "stop" -> {
                    ChronoSession session = getSessionSafely(input);
                    if (session != null) {
                        session.stop();
                        result.put("status", "stopped");
                        result.put("elapsedFormatted", formatTime(session.getElapsedTime()));
                    } else {
                        result.put("status", "error");
                        result.put("message", "Session not found");
                    }
                }
                case "reset" -> {
                    ChronoSession session = getSessionSafely(input);
                    if (session != null) {
                        session.reset();
                        result.put("status", "reset");
                        result.put("message", "Chronometer reset successfully.");
                    } else {
                        result.put("status", "error");
                        result.put("message", "Session not found");
                    }
                }
                case "log" -> {
                    ChronoSession session = getSessionSafely(input);
                    if (session != null) {
                        String label = (String) input.getOrDefault("lapLabel", "");
                        try {
                            ChronoLap lap = session.logLap(label);
                            result.put("lapNumber", lap.getLapNumber());
                            if (!lap.getLabel().isEmpty()) {
                                result.put("lapLoggedLabel", lap.getLabel());
                            }
                            result.put("lapElapsedFormatted", formatTime(lap.getElapsedTime()));
                            result.put("splitFormatted", formatTime(lap.getSplitTime()));
                        } catch (IllegalStateException e) {
                            result.put("status", "error");
                            result.put("message", e.getMessage());
                        }
                    } else {
                        result.put("status", "error");
                        result.put("message", "Session not found");
                    }
                }
                case "status" -> {
                    ChronoSession session = getSessionSafely(input);
                    if (session != null) {
                        result.put("status", session.getStatus());
                        result.put("elapsedFormatted", formatTime(session.getElapsedTime()));
                        result.put("laps", formatLapsForTable(session));
                        result.put("lapCount", session.getLapCount());
                    } else {
                        result.put("status", "error");
                        result.put("message", "Session not found");
                    }
                }
                case "getlogs" -> {
                    ChronoSession session = getSessionSafely(input);
                    if (session != null) {
                        result.put("laps", formatLapsForTable(session));
                    } else {
                        result.put("status", "error");
                        result.put("message", "Session not found");
                    }
                }
                case "listsessions" -> {
                    List<Map<String, Object>> sessionsData = activeSessions.values().stream()
                            .map(session -> Map.<String, Object>of(
                                    "sessionId", session.getSessionId(),
                                    "name", session.getName(),
                                    "status", session.getStatus(),
                                    "elapsedFormatted", formatTime(session.getElapsedTime()),
                                    "lapCount", session.getLapCount()
                            ))
                            .collect(Collectors.toList());
                    result.put("sessions", sessionsData);
                }
                case "deletesession" -> {
                    ChronoSession session = getSessionSafely(input);
                    if (session != null) {
                        activeSessions.remove(session.getSessionId());
                        result.put("message", "Session '" + session.getName() + "' deleted.");
                    } else {
                        result.put("status", "error");
                        result.put("message", "Session not found");
                    }
                }
                default -> {
                    result.put("success", false);
                    result.put("error", "Unsupported operation: " + uiOperation);
                }
            }

            // Add the operation context
            result.put("uiOperation", uiOperation);
            return result;

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    private ChronoSession getSessionSafely(Map<String, Object> input) {
        String sessionId = (String) input.get("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        return activeSessions.get(sessionId);
    }

    private List<Map<String, Object>> formatLapsForTable(ChronoSession session) {
        return session.getLaps().stream()
                .map(lap -> Map.<String, Object>of(
                        "lapNumber", lap.getLapNumber(),
                        "label", lap.getLabel(),
                        "elapsedFormatted", formatTime(lap.getElapsedTime()),
                        "splitFormatted", formatTime(lap.getSplitTime())
                ))
                .collect(Collectors.toList());
    }

    private String formatTime(long timeInMillis) {
        if (timeInMillis < 0) return "00:00:00.000";
        synchronized (TIME_FORMAT) {
            return TIME_FORMAT.format(new Date(timeInMillis));
        }
    }

    // --- Inner Classes (ChronoSession, ChronoLap) ---
    private static class ChronoSession {
        private final String sessionId;
        private final String name;
        private final long createdTime;
        private long startTime = 0;
        private long stopTime = 0;
        private long accumulatedElapsedTime = 0;
        private boolean running = false;
        private final List<ChronoLap> laps = Collections.synchronizedList(new ArrayList<>());
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
        public String getStatus() {
            if (running) return "running";
            if (accumulatedElapsedTime > 0 || stopTime > 0) return "stopped";
            if (startTime == 0 && stopTime == 0 && accumulatedElapsedTime == 0) return "reset";
            return "created";
        }
        public long getStartTime() { return startTime; }
        public long getStopTime() { return stopTime; }
        public int getLapCount() { return laps.size(); }
        public List<ChronoLap> getLaps() { return new ArrayList<>(laps); }
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
            this.label = (label == null || label.isBlank()) ? "" : label;
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