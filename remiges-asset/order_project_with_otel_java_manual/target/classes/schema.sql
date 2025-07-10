-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(50)
);

-- Products table
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    price DECIMAL(10,2) NOT NULL CHECK (price > 0),
    stock_quantity INTEGER NOT NULL CHECK (stock_quantity >= 0),
    category VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    total_amount DECIMAL(10,2) NOT NULL CHECK (total_amount > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Insert sample data for demonstration
INSERT INTO users (name, email, role) VALUES 
('John Doe', 'john.doe@example.com', 'ADMIN'),
('Jane Smith', 'jane.smith@example.com', 'USER'),
('Bob Johnson', 'bob.johnson@example.com', 'MANAGER'),
('Alice Brown', 'alice.brown@example.com', 'USER'),
('Charlie Wilson', 'charlie.wilson@example.com', 'USER')
ON CONFLICT (email) DO NOTHING;

INSERT INTO products (name, description, price, stock_quantity, category) VALUES 
('Laptop Pro', 'High-performance laptop for professionals', 1299.99, 50, 'electronics'),
('Wireless Mouse', 'Ergonomic wireless mouse with long battery life', 29.99, 200, 'electronics'),
('Office Chair', 'Comfortable ergonomic office chair', 199.99, 25, 'furniture'),
('Programming Book', 'Complete guide to modern programming', 49.99, 100, 'books'),
('Coffee Mug', 'Insulated coffee mug for developers', 15.99, 150, 'accessories')
ON CONFLICT (name) DO NOTHING;

INSERT INTO orders (user_id, product_id, quantity, total_amount, status) VALUES 
(1, 1, 1, 1299.99, 'DELIVERED'),
(2, 2, 2, 59.98, 'SHIPPED'),
(3, 3, 1, 199.99, 'CONFIRMED'),
(4, 4, 3, 149.97, 'PENDING'),
(5, 5, 2, 31.98, 'DELIVERED')
ON CONFLICT DO NOTHING;
