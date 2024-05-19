import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    private SyntaxChecker checker;
    private final Map<String, Double> taskTypeSizes;
    private final Map<String, String> stations;
    private final Map<String, String> jobTypes;
    private final ArrayList<Task> tasks = new ArrayList<>();
    private final ArrayList<jobTypeID> jobs = new ArrayList<>();
    private final ArrayList<Station> Stations = new ArrayList<>();

    // Constructor
    public Parser(String workflowFile) {
        File file = new File(workflowFile);
        checker = new SyntaxChecker();
        taskTypeSizes = new HashMap<>();
        stations = new HashMap<>();
        jobTypes = new HashMap<>();
        read(file);
    }

    //to generate object ArrayLists:
    public ArrayList<Task> getTasks() {
        return tasks;
    }

    public ArrayList<jobTypeID> getJobs() {
        return jobs;
    }
    public ArrayList<Station> getStations(){
        return Stations;
    }

    // Read the file
    public void read(File file) {
        try {
            readWorkflowFile(file);
        } catch (FileNotFoundException e) {
            System.err.println("Workflow file not found.");
        }
    }

    private void readWorkflowFile(File file) throws FileNotFoundException {
        try (Scanner fileScanner = new Scanner(file)) {
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine().trim();
                if (line.startsWith("(TASKTYPES")) {
                    parseTaskTypes(line, fileScanner);
                } else if (line.startsWith("(STATIONS")) {
                    parseStations(line, fileScanner);
                } else if (line.startsWith("(JOBTYPES")) {
                    parseJobTypes(line, fileScanner);
                }
            }
        }
    }

    private void parseTaskTypes(String line, Scanner fileScanner) {
        StringBuilder taskTypesBuilder = new StringBuilder(line);

        // Read until the closing parenthesis is found
        while (!line.contains(")")) {
            if (fileScanner.hasNextLine()) {
                line = fileScanner.nextLine().trim();
                taskTypesBuilder.append(" ").append(line);
            } else {
                checker.add("Error: No closing parenthesis found for TASKTYPES.");
                return;
            }
        }

        String completeLine = taskTypesBuilder.toString();
        System.out.println("Complete TASKTYPES line: " + completeLine);

        Pattern pattern = Pattern.compile("\\(TASKTYPES\\s+([^)]+)\\)");
        Matcher matcher = pattern.matcher(completeLine);

        if (matcher.find()) {
            String content = matcher.group(1).trim();
            String[] parts = content.split("\\s+");
            Set<String> taskTypeSet = new HashSet<>();

            for (int i = 0; i < parts.length; i++) {
                String taskType = parts[i];

                if (taskType.isEmpty()) {
                    continue;
                }

                // Check if the next part is a number (task size)
                if (i + 1 < parts.length) {
                    String nextPart = parts[i + 1].trim();
                    if (isNumeric(nextPart)) {
                        double taskSize = Math.abs(Double.parseDouble(nextPart));
                        if (isValidTaskType(taskType)) {
                            if (taskTypeSizes.containsKey(taskType)) {
                                checker.add("Duplicate task type: " + taskType);
                            } else {
                                taskTypeSizes.put(taskType, taskSize); // Store valid task type and size globally
                                taskTypeSet.add(taskType);
                            }
                        } else {
                            checker.add("Invalid task type: " + taskType);
                        }
                        i++;
                    } else {
                        if (isValidTaskType(taskType)) {
                            if (taskTypeSizes.containsKey(taskType)) {
                                checker.add("Duplicate task type: " + taskType);
                            } else {
                                taskTypeSizes.put(taskType, 1.0); // Default size: 1.0 for valid task type
                                taskTypeSet.add(taskType);
                            }
                        } else {
                            checker.add("Invalid task type: " + taskType);
                        }
                    }
                } else {
                    if (isValidTaskType(taskType)) {
                        if (taskTypeSizes.containsKey(taskType)) {
                            checker.add("Duplicate task type: " + taskType);
                        } else {
                            taskTypeSizes.put(taskType, 1.0); // Default size for the last valid task type without a size
                            taskTypeSet.add(taskType);
                        }
                    } else {
                        checker.add("Invalid task type: " + taskType);
                    }
                }
            }
        } else {
            checker.add("Error: 'TASKTYPES' pattern not found in the line: " + completeLine);
        }
        for (Map.Entry<String, Double> entry : taskTypeSizes.entrySet()) {
            Task task = new Task(entry.getKey(), entry.getValue());
            tasks.add(task);
        }
    }

    //what I need: Arraylist<Task> stationTasks, ArrayList<TaskTypeReeders, then generate objects
    private void parseStations(String line, Scanner fileScanner) {
        StringBuilder stationsBuilder = new StringBuilder(line);

        // Read until the closing parenthesis is found
        while (!line.contains("))")) {
            if (fileScanner.hasNextLine()) {
                line = fileScanner.nextLine().trim();
                stationsBuilder.append(" ").append(line);
            } else {
                checker.add("Error: No closing parenthesis found for STATIONS.");
                return;
            }
        }

        String completeLine = stationsBuilder.toString();
        System.out.println("Complete STATIONS line: " + completeLine);

        Pattern pattern = Pattern.compile("\\(STATIONS\\s+([^)]+)\\)");
        Matcher matcher = pattern.matcher(completeLine);

        if (matcher.find()) {
            String content = matcher.group(1).trim();
            Pattern stationPattern = Pattern.compile("\\(([^)]+)\\)");
            Matcher stationMatcher = stationPattern.matcher(content);
            System.out.println("Here"+content);

            while (stationMatcher.find()) {
                String stationContent = stationMatcher.group(1).trim();
                System.out.println(stationContent);
                String[] stationParts = stationContent.split("\\s+");

                if (stationParts.length >= 2) {
                    String stationName = stationParts[0].trim();
                    String stationLocation = stationParts[1].trim();
                    StringBuilder additionalInfo = new StringBuilder();
                    for (int i = 2; i < stationParts.length; i++) {
                        additionalInfo.append(stationParts[i]).append(" ");
                    }
                    stations.put(stationName, stationLocation + " " + additionalInfo.toString().trim());
                } else {
                    checker.add("Invalid station format: " + stationContent);
                }
            }
        } else {
            checker.add("Error: 'STATIONS' pattern not found in the line: " + completeLine);
        }
    }


    private void parseJobTypes(String line, Scanner fileScanner) {
        while (!line.contains("))")) {
            if (fileScanner.hasNextLine()) {
                line += " " + fileScanner.nextLine().trim();
            } else {
                checker.add("Error: No closing parenthesis found for JOBTYPES.");
                return;
            }
        }

        System.out.println("Complete JOBTYPES line: " + line);

        Pattern pattern = Pattern.compile("\\(JOBTYPES\\s+(.+)\\)");
        Matcher matcher = pattern.matcher(line);

        while (matcher.find()) {
            String jobBlock = matcher.group(1).trim();
            Pattern jobPattern = Pattern.compile("\\(([^)]+)\\)");
            Matcher jobMatcher = jobPattern.matcher(jobBlock);

            while (jobMatcher.find()) {
                String jobContent = jobMatcher.group(1).trim();
                String[] jobParts = jobContent.split("\\s+");

                if (jobParts.length > 0) {
                    String jobId = jobParts[0];
                    ArrayList<Task> jobTasks = new ArrayList<>();

                    for (int i = 1; i < jobParts.length; i++) {
                        String task = jobParts[i];
                        int next = i + 1;

                        // if task matches and next is numeric: .setSize()
                        if (isValidTaskType(task) && next < jobParts.length && isNumeric(jobParts[next])) {
                            double size = Double.parseDouble(jobParts[next]);
                            jobTasks.add(new Task(task, size));
                            i++; // skip next part as it's used
                        } else if (isValidTaskType(task)) {
                            if (taskTypeSizes.containsKey(task)) {
                                jobTasks.add(new Task(task, taskTypeSizes.get(task)));
                            } else {
                                checker.add("Unknown task type: " + task);
                            }
                        } else {
                            checker.add("Invalid task type in job " + jobId + ": " + task);
                        }
                    }

                    boolean isDuplicate = false;
                    for (jobTypeID job : jobs) {
                        if (job.getJobTypeID().equals(jobId)) {
                            checker.add("Duplicate Job: " + jobId);
                            isDuplicate = true;
                            break;
                        }
                    }
                    if (!isDuplicate) {
                        jobTypeID newJob = new jobTypeID(jobId, jobTasks);
                        jobs.add(newJob);
                    }

                } else {
                    checker.add("Invalid job format: " + jobContent);
                }

            }

        }
    }

    private boolean isValidTaskType(String taskType) {
        return taskType.matches("^T\\d+$");
    }

    private boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    public void printErrors() {
        checker.printErrors();
    }

}
