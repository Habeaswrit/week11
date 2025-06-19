/* Drops tables in an order that respects foreign key constraints, ensuring dependent tables are dropped before referenced tables. */
DROP TABLE IF EXISTS project_category;
DROP TABLE IF EXISTS material;
DROP TABLE IF EXISTS step;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS project;

/* Creates tables in the reverse order of dropping to satisfy foreign key dependencies. Each table defines an auto-incrementing primary key and uses ON DELETE CASCADE for foreign keys to remove dependent rows automatically. */

-- Creates the project table to store core project details.
CREATE TABLE project (
    project_id INT NOT NULL AUTO_INCREMENT,
    project_name VARCHAR(128) NOT NULL,
    estimated_hours DECIMAL(7,2),
    actual_hours DECIMAL(7,2),
    difficulty INT,
    notes TEXT,
    PRIMARY KEY (project_id)
);

-- Creates the category table to store project category names.
CREATE TABLE category (
    category_id INT NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(128) NOT NULL,
    PRIMARY KEY (category_id)
);

-- Creates the step table to store ordered steps for a project. The ON DELETE CASCADE ensures steps are removed when the associated project is deleted.
CREATE TABLE step (
    step_id INT NOT NULL AUTO_INCREMENT,
    project_id INT NOT NULL,
    step_text TEXT NOT NULL,
    step_order INT NOT NULL,
    PRIMARY KEY (step_id),
    FOREIGN KEY (project_id) REFERENCES project (project_id) ON DELETE CASCADE
);

-- Creates the material table to store materials required for a project. The ON DELETE CASCADE ensures materials are removed when the associated project is deleted.
CREATE TABLE material (
    material_id INT AUTO_INCREMENT NOT NULL,
    project_id INT NOT NULL,
    material_name VARCHAR(128) NOT NULL,
    num_required INT,
    cost DECIMAL(7,2),
    PRIMARY KEY (material_id),
    FOREIGN KEY (project_id) REFERENCES project (project_id) ON DELETE CASCADE
);

-- Creates the project_category table to manage the many-to-many relationship between projects and categories. The ON DELETE CASCADE ensures mappings are removed when either the project or category is deleted. The UNIQUE KEY prevents duplicate project-category pairs.
CREATE TABLE project_category (
    project_id INT NOT NULL,
    category_id INT NOT NULL,
    FOREIGN KEY (category_id) REFERENCES category (category_id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES project (project_id) ON DELETE CASCADE,
    UNIQUE KEY (project_id, category_id)
);