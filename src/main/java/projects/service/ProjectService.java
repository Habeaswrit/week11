package projects.service;

import java.util.List;
import java.util.NoSuchElementException;

import projects.dao.ProjectDao;
import projects.entity.Project;
import projects.exception.DbException;

/**
 * Service layer for managing project-related operations in a three-tier application.
 * This class acts as an intermediary between the presentation layer and the data access layer,
 * delegating CRUD operations to the {@link ProjectDao} while handling business logic and
 * exception translation.
 */
public class ProjectService {
    private ProjectDao projectDao = new ProjectDao();

    /**
     * Creates a new project by delegating to the data access layer.
     *
     * @param project The project to create.
     * @return The created project with its generated project ID.
     * @throws DbException If an error occurs during the database operation.
     */
    public Project addProject(Project project) {
        return projectDao.insertProject(project);
    }

    /**
     * Retrieves a project by its ID, including associated details such as materials,
     * steps, and categories.
     *
     * @param projectId The ID of the project to retrieve.
     * @return The project with the specified ID.
     * @throws NoSuchElementException If no project exists with the specified ID.
     * @throws DbException If an error occurs during the database operation.
     */
    public Project fetchProjectById(Integer projectId) {
        return projectDao.fetchProjectById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Project with ID=" + projectId + " does not exist."));
    }

    /**
     * Retrieves all projects from the data access layer, excluding detailed information
     * such as materials, steps, and categories.
     *
     * @return A list of all projects.
     * @throws DbException If an error occurs during the database operation.
     */
    public List<Project> fetchAllProjects() {
        return projectDao.fetchAllProjects();
    }

    /**
     * Updates the details of an existing project by delegating to the data access layer.
     *
     * @param project The project containing updated details.
     * @throws DbException If no project exists with the specified ID or if a database
     *                     error occurs.
     */
    public void modifyProjectDetails(Project project) {
        if (!projectDao.modifyProjectDetails(project)) {
            throw new DbException("Project with ID=" + project.getProjectId() + " does not exist.");
        }
    }

    /**
     * Deletes a project by its ID by delegating to the data access layer.
     *
     * @param projectId The ID of the project to delete.
     * @throws DbException If no project exists with the specified ID or if a database
     *                     error occurs.
     */
    public void deleteProject(Integer projectId) {
        if (!projectDao.deleteProject(projectId)) {
            throw new DbException("Project with ID=" + projectId + " does not exist.");
        }
    }
}