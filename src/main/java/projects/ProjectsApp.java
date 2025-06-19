package projects;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import projects.entity.Project;
import projects.exception.DbException;
import projects.service.ProjectService;

/**
 * Console-based application for managing projects. Provides a menu-driven interface
 * for users to perform CRUD operations on projects, interacting with the
 * {@link ProjectService} to handle business logic and data access.
 */
public class ProjectsApp {
    private Scanner scanner = new Scanner(System.in);
    private ProjectService projectService = new ProjectService();
    private Project curProject;

    // @formatter:off
    /**
     * List of available menu operations displayed to the user.
     */
    private List<String> operations = List.of(
            "1) Add a project",
            "2) List projects",
            "3) Select a project",
            "4) Update project details",
            "5) Delete a project"
    );
    // @formatter:on

    /**
     * Entry point for the application. Initializes and starts the menu-driven interface.
     *
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        new ProjectsApp().processUserSelections();
    }

    /**
     * Processes user menu selections in a loop until the user chooses to exit.
     * Displays the menu, retrieves user input, and executes the corresponding operation.
     *
     * @throws DbException If a database error occurs during an operation.
     */
    private void processUserSelections() {
        boolean done = false;
        while (!done) {
            try {
                int selection = getUserSelection();

                switch (selection) {
                    case -1:
                        done = exitMenu();
                        break;
                    case 1:
                        createProject();
                        break;
                    case 2:
                        listProjects();
                        break;
                    case 3:
                        selectProject();
                        break;
                    case 4:
                        updateProjectDetails();
                        break;
                    case 5:
                        deleteProject();
                        break;
                    default:
                        System.out.println("\n" + selection + " is not a valid selection. Try again.");
                        break;
                }
            } catch (Exception e) {
                System.out.println("\nError: " + e.getMessage() + ". Try again.");
            }
        }
    }

    /**
     * Deletes a project by its ID after prompting the user for input. Clears the
     * current project if it matches the deleted project's ID.
     *
     * @throws DbException If the project does not exist or a database error occurs.
     */
    private void deleteProject() {
        listProjects();

        Integer projectId = getIntInput("Enter the ID of the project to delete");

        projectService.deleteProject(projectId);
        System.out.println("Project " + projectId + " was deleted successfully.");

        if (Objects.nonNull(curProject) && curProject.getProjectId().equals(projectId)) {
            curProject = null;
        }
    }

    /**
     * Updates the details of the currently selected project. Prompts the user for
     * new values and retains existing values if no input is provided.
     *
     * @throws DbException If no project is selected, the project does not exist,
     *                     or a database error occurs.
     */
    private void updateProjectDetails() {
        if (Objects.isNull(curProject)) {
            System.out.println("\nPlease select a project.");
            return;
        }

        String projectName = getStringInput("Enter the project name [" + curProject.getProjectName() + "]");
        BigDecimal estimatedHours = getDecimalInput("Enter the estimated hours [" + curProject.getEstimatedHours() + "]");
        BigDecimal actualHours = getDecimalInput("Enter the actual hours [" + curProject.getActualHours() + "]");
        Integer difficulty = getIntInput("Enter the project difficulty scale (1-5) [" + curProject.getDifficulty() + "]");
        String notes = getStringInput("Enter the project notes [" + curProject.getNotes() + "]");

        Project project = new Project();
        project.setProjectId(curProject.getProjectId());
        project.setProjectName(Objects.isNull(projectName) ? curProject.getProjectName() : projectName);
        project.setEstimatedHours(Objects.isNull(estimatedHours) ? curProject.getEstimatedHours() : estimatedHours);
        project.setActualHours(Objects.isNull(actualHours) ? curProject.getActualHours() : actualHours);
        project.setDifficulty(Objects.isNull(difficulty) ? curProject.getDifficulty() : difficulty);
        project.setNotes(Objects.isNull(notes) ? curProject.getNotes() : notes);

        projectService.modifyProjectDetails(project);
        curProject = projectService.fetchProjectById(curProject.getProjectId());
    }

    /**
     * Selects a project by its ID, setting it as the current project for subsequent operations.
     *
     * @throws DbException If the project ID is invalid or a database error occurs.
     */
    private void selectProject() {
        listProjects();
        Integer projectId = getIntInput("Enter a project ID to select a project");

        curProject = null;
        curProject = projectService.fetchProjectById(projectId);
    }

    /**
     * Lists all projects, displaying their IDs and names.
     *
     * @throws DbException If a database error occurs.
     */
    private void listProjects() {
        List<Project> projects = projectService.fetchAllProjects();

        System.out.println("\nProjects:");
        projects.forEach(project -> System.out.println("   " + project.getProjectId() + ":