package projects.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import projects.entity.Category;
import projects.entity.Material;
import projects.entity.Project;
import projects.entity.Step;
import projects.exception.DbException;
import provided.util.DaoBase;

/**
 * Data Access Object (DAO) for performing CRUD operations on the project-related tables
 * in the database using JDBC.
 */
public class ProjectDao extends DaoBase {

    private static final String CATEGORY_TABLE = "category";
    private static final String MATERIAL_TABLE = "material";
    private static final String PROJECT_TABLE = "project";
    private static final String PROJECT_CATEGORY_TABLE = "project_category";
    private static final String STEP_TABLE = "step";

    /**
     * Inserts a new project into the project table.
     *
     * @param project The project to insert.
     * @return The inserted project with its generated project ID.
     * @throws DbException If an error occurs during the database operation.
     */
    public Project insertProject(Project project) {
        String sql = "INSERT INTO " + PROJECT_TABLE
                + " (project_name, estimated_hours, actual_hours, difficulty, notes) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DbConnection.getConnection()) {
            startTransaction(conn);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setParameter(stmt, 1, project.getProjectName(), String.class);
                setParameter(stmt, 2, project.getEstimatedHours(), BigDecimal.class);
                setParameter(stmt, 3, project.getActualHours(), BigDecimal.class);
                setParameter(stmt, 4, project.getDifficulty(), Integer.class);
                setParameter(stmt, 5, project.getNotes(), String.class);

                stmt.executeUpdate();

                Integer projectId = getLastInsertId(conn, PROJECT_TABLE);
                commitTransaction(conn);

                project.setProjectId(projectId);
                return project;
            } catch (Exception e) {
                rollbackTransaction(conn);
                throw new DbException("Failed to insert project", e);
            }
        } catch (SQLException e) {
            throw new DbException("Database connection error", e);
        }
    }

    /**
     * Retrieves all projects from the project table, ordered by project name.
     *
     * @return A list of all projects.
     * @throws DbException If an error occurs during the database operation.
     */
    public List<Project> fetchAllProjects() {
        String sql = "SELECT * FROM " + PROJECT_TABLE + " ORDER BY project_name";

        try (Connection conn = DbConnection.getConnection()) {
            startTransaction(conn);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Project> projects = new LinkedList<>();

                    while (rs.next()) {
                        projects.add(extract(rs, Project.class));
                    }

                    return projects;
                }
            } catch (Exception e) {
                rollbackTransaction(conn);
                throw new DbException("Failed to fetch projects", e);
            }
        } catch (SQLException e) {
            throw new DbException("Database connection error", e);
        }
    }

    /**
     * Retrieves a project by its ID, including associated materials, steps, and categories.
     *
     * @param projectId The ID of the project to retrieve.
     * @return An Optional containing the project if found, or empty if no project exists
     *         with the specified ID.
     * @throws DbException If an error occurs during the database operation.
     */
    public Optional<Project> fetchProjectById(Integer projectId) {
        String sql = "SELECT * FROM " + PROJECT_TABLE + " WHERE project_id = ?";

        try (Connection conn = DbConnection.getConnection()) {
            startTransaction(conn);

            try {
                Project project = null;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    setParameter(stmt, 1, projectId, Integer.class);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            project = extract(rs, Project.class);
                        }
                    }
                }

                if (Objects.nonNull(project)) {
                    project.getMaterials().addAll(fetchMaterialsForProject(conn, projectId));
                    project.getSteps().addAll(fetchStepsForProject(conn, projectId));
                    project.getCategories().addAll(fetchCategoriesForProject(conn, projectId));
                }

                commitTransaction(conn);
                return Optional.ofNullable(project);
            } catch (Exception e) {
                rollbackTransaction(conn);
                throw new DbException("Failed to fetch project with ID " + projectId, e);
            }
        } catch (SQLException e) {
            throw new DbException("Database connection error", e);
        }
    }

    /**
     * Retrieves all categories associated with the specified project ID. Uses an inner join
     * with the project_category table to handle the many-to-many relationship between
     * projects and categories.
     *
     * @param conn      The database connection, provided by the caller to maintain the
     *                  transaction context.
     * @param projectId The ID of the project for which to retrieve categories.
     * @return A list of categories associated with the project.
     * @throws DbException If an error occurs during the database operation.
     */
    private List<Category> fetchCategoriesForProject(Connection conn, Integer projectId) {
        // @formatter:off
        String sql = "SELECT c.* FROM " + CATEGORY_TABLE + " c "
                + "JOIN " + PROJECT_CATEGORY_TABLE + " pc USING (category_id) "
                + "WHERE project_id = ?";
        // @formatter:on

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParameter(stmt, 1, projectId, Integer.class);

            try (ResultSet rs = stmt.executeQuery()) {
                List<Category> categories = new LinkedList<>();

                while (rs.next()) {
                    categories.add(extract(rs, Category.class));
                }

                return categories;
            }
        } catch (SQLException e) {
            throw new DbException("Failed to fetch categories for project ID " + projectId, e);
        }
    }

    /**
     * Retrieves all steps associated with the specified project ID, ordered by step order.
     *
     * @param conn      The database connection, provided by the caller to maintain the
     *                  transaction context.
     * @param projectId The ID of the project for which to retrieve steps.
     * @return A list of steps associated with the project.
     * @throws SQLException If a database error occurs.
     */
    private List<Step> fetchStepsForProject(Connection conn, Integer projectId) throws SQLException {
        String sql = "SELECT * FROM " + STEP_TABLE + " WHERE project_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParameter(stmt, 1, projectId, Integer.class);

            try (ResultSet rs = stmt.executeQuery()) {
                List<Step> steps = new LinkedList<>();

                while (rs.next()) {
                    steps.add(extract(rs, Step.class));
                }

                return steps;
            }
        }
    }

    /**
     * Retrieves all materials associated with the specified project ID.
     *
     * @param conn      The database connection, provided by the caller to maintain the
     *                  transaction context.
     * @param projectId The ID of the project for which to retrieve materials.
     * @return A list of materials associated with the project.
     * @throws SQLException If a database error occurs.
     */
    private List<Material> fetchMaterialsForProject(Connection conn, Integer projectId) throws SQLException {
        String sql = "SELECT * FROM " + MATERIAL_TABLE + " WHERE project_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParameter(stmt, 1, projectId, Integer.class);

            try (ResultSet rs = stmt.executeQuery()) {
                List<Material> materials = new LinkedList<>();

                while (rs.next()) {
                    materials.add(extract(rs, Material.class));
                }

                return materials;
            }
        }
    }

    /**
     * Updates the details of an existing project in the project table.
     *
     * @param project The project containing updated details.
     * @return true if the project was updated successfully, false if no project was found
     *         with the specified ID.
     * @throws DbException If an error occurs during the database operation.
     */
    public boolean modifyProjectDetails(Project project) {
        String sql = "UPDATE " + PROJECT_TABLE + " SET "
                + "project_name = ?, "
                + "estimated_hours = ?, "
                + "actual_hours = ?, "
                + "difficulty = ?, "
                + "notes = ? "
                + "WHERE project_id = ?";

        try (Connection conn = DbConnection.getConnection()) {
            startTransaction(conn);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setParameter(stmt, 1, project.getProjectName(), String.class);
                setParameter(stmt, 2, project.getEstimatedHours(), BigDecimal.class);
                setParameter(stmt, 3, project.getActualHours(), BigDecimal.class);
                setParameter(stmt, 4, project.getDifficulty(), Integer.class);
                setParameter(stmt, 5, project.getNotes(), String.class);
                setParameter(stmt, 6, project.getProjectId(), Integer.class);

                boolean modified = stmt.executeUpdate() == 1;
                commitTransaction(conn);

                return modified;
            } catch (Exception e) {
                rollbackTransaction(conn);
                throw new DbException("Failed to update project with ID " + project.getProjectId(), e);
            }
        } catch (SQLException e) {
            throw new DbException("Database connection error", e);
        }
    }

    /**
     * Deletes a project from the project table by its ID.
     *
     * @param projectId The ID of the project to delete.
     * @return true if the project was deleted successfully, false if no project was found
     *         with the specified ID.
     * @throws DbException If an error occurs during the database operation.
     */
    public boolean deleteProject(Integer projectId) {
        String sql = "DELETE FROM " + PROJECT_TABLE + " WHERE project_id = ?";

        try (Connection conn = DbConnection.getConnection()) {
            startTransaction(conn);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setParameter(stmt, 1, projectId, Integer.class);

                boolean deleted = stmt.executeUpdate() == 1;
                commitTransaction(conn);

                return deleted;
            } catch (Exception e) {
                rollbackTransaction(conn);
                throw new DbException("Failed to delete project with ID " + projectId, e);
            }
        } catch (SQLException e) {
            throw new DbException("Database connection error", e);
        }
    }
}