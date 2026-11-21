INSERT IGNORE INTO roles (name) VALUES ('ADMIN'), ('EMPLOYEE');

INSERT IGNORE INTO categories (name, description, status) VALUES
('Mobiles', 'Smartphones and feature phones', 'Active'),
('Laptops', 'Windows and Linux laptops', 'Active'),
('Desktops', 'Desktop computers and all-in-ones', 'Active'),
('MacBooks', 'Apple MacBook range', 'Active'),
('iPhones', 'Apple iPhone range', 'Active'),
('Tablets', 'Tablets and iPads', 'Active'),
('Chargers', 'Mobile and laptop chargers', 'Active'),
('Mouse', 'Wired and wireless mouse', 'Active'),
('Keyboards', 'Keyboards and keyboard combos', 'Active'),
('Power Adapters', 'Power adapters and cables', 'Active'),
('Smart Watches', 'Smart watches and fitness bands', 'Active'),
('Accessories', 'Other accessories', 'Active');

INSERT IGNORE INTO suppliers (name, contact_person, mobile, email, address, status) VALUES
('Dell India', 'Rakesh Mehta', '9876543210', 'sales@dellindia.example', 'Bengaluru, Karnataka', 'Active'),
('HP India', 'Anita Rao', '9876543211', 'sales@hpindia.example', 'Chennai, Tamil Nadu', 'Active'),
('Apple India', 'Vikram Shah', '9876543212', 'sales@appleindia.example', 'Mumbai, Maharashtra', 'Active'),
('Samsung India', 'Neha Kapoor', '9876543213', 'sales@samsungindia.example', 'Noida, Uttar Pradesh', 'Active');

INSERT IGNORE INTO customers (name, mobile, email, address, status) VALUES
('Priya Sharma', '9876543201', 'priya@example.com', 'Bengaluru', 'Active'),
('Ananya Iyer', '9876543202', 'ananya@example.com', 'Mysuru', 'Active'),
('Megha Patil', '9876543203', 'megha@example.com', 'Delhi', 'Active'),
('Rahul Verma', '9876543204', 'rahul.verma@example.com', 'Hyderabad', 'Active');

INSERT IGNORE INTO products (product_code, name, category_id, supplier_id, purchase_price, selling_price, brand, product_date, description, status)
SELECT 'MOB-001', 'Redmi Note 13', c.id, s.id, 14500, 17999, 'Redmi', CURDATE(), 'Redmi Note 13 - sample product', 'Active' FROM categories c, suppliers s WHERE c.name = 'Mobiles' AND s.name = 'Samsung India';

INSERT IGNORE INTO products (product_code, name, category_id, supplier_id, purchase_price, selling_price, brand, product_date, description, status)
SELECT 'MOB-002', 'Samsung Galaxy S23', c.id, s.id, 52000, 62999, 'Samsung', CURDATE(), 'Samsung Galaxy S23 - sample product', 'Active' FROM categories c, suppliers s WHERE c.name = 'Mobiles' AND s.name = 'Samsung India';

INSERT IGNORE INTO products (product_code, name, category_id, supplier_id, purchase_price, selling_price, brand, product_date, description, status)
SELECT 'IPH-001', 'iPhone 15', c.id, s.id, 68000, 79900, 'Apple', CURDATE(), 'iPhone 15 - sample product', 'Active' FROM categories c, suppliers s WHERE c.name = 'iPhones' AND s.name = 'Apple India';

INSERT IGNORE INTO products (product_code, name, category_id, supplier_id, purchase_price, selling_price, brand, product_date, description, status)
SELECT 'MAC-001', 'MacBook Air M2', c.id, s.id, 89000, 104900, 'Apple', CURDATE(), 'MacBook Air M2 - sample product', 'Active' FROM categories c, suppliers s WHERE c.name = 'MacBooks' AND s.name = 'Apple India';

INSERT IGNORE INTO products (product_code, name, category_id, supplier_id, purchase_price, selling_price, brand, product_date, description, status)
SELECT 'LAP-001', 'Dell Inspiron 15', c.id, s.id, 42000, 52990, 'Dell', CURDATE(), 'Dell Inspiron 15 - sample product', 'Active' FROM categories c, suppliers s WHERE c.name = 'Laptops' AND s.name = 'Dell India';

INSERT IGNORE INTO products (product_code, name, category_id, supplier_id, purchase_price, selling_price, brand, product_date, description, status)
SELECT 'LAP-002', 'HP Pavilion 14', c.id, s.id, 48000, 59990, 'HP', CURDATE(), 'HP Pavilion 14 - sample product', 'Active' FROM categories c, suppliers s WHERE c.name = 'Laptops' AND s.name = 'HP India';

INSERT IGNORE INTO products (product_code, name, category_id, supplier_id, purchase_price, selling_price, brand, product_date, description, status)
SELECT 'DES-001', 'Dell OptiPlex Desktop', c.id, s.id, 31000, 38990, 'Dell', CURDATE(), 'Dell OptiPlex Desktop - sample product', 'Active' FROM categories c, suppliers s WHERE c.name = 'Desktops' AND s.name = 'Dell India';

INSERT IGNORE INTO products (product_code, name, category_id, supplier_id, purchase_price, selling_price, brand, product_date, description, status)
SELECT 'TAB-001', 'iPad 10th Gen', c.id, s.id, 31000, 39900, 'Apple', CURDATE(), 'iPad 10th Gen - sample product', 'Active' FROM categories c, suppliers s WHERE c.name = 'Tablets' AND s.name = 'Apple India';

INSERT IGNORE INTO products (product_code, name, category_id, supplier_id, purchase_price, selling_price, brand, product_date, description, status)
SELECT 'CHG-001', 'USB-C 20W Charger', c.id, s.id, 1200, 1900, 'Apple', CURDATE(), 'USB-C 20W Charger - sample product', 'Active' FROM categories c, suppliers s WHERE c.name = 'Chargers' AND s.name = 'Apple India';

INSERT IGNORE INTO products (product_code, name, category_id, supplier_id, purchase_price, selling_price, brand, product_date, description, status)
SELECT 'MOU-001', 'Wireless Mouse M221', c.id, s.id, 400, 799, 'Logitech', CURDATE(), 'Wireless Mouse M221 - sample product', 'Active' FROM categories c, suppliers s WHERE c.name = 'Mouse' AND s.name = 'HP India';

INSERT IGNORE INTO products (product_code, name, category_id, supplier_id, purchase_price, selling_price, brand, product_date, description, status)
SELECT 'KEY-001', 'Mechanical Keyboard K2', c.id, s.id, 2800, 4299, 'Keychron', CURDATE(), 'Mechanical Keyboard K2 - sample product', 'Active' FROM categories c, suppliers s WHERE c.name = 'Keyboards' AND s.name = 'HP India';

INSERT IGNORE INTO products (product_code, name, category_id, supplier_id, purchase_price, selling_price, brand, product_date, description, status)
SELECT 'PWR-001', '65W Laptop Adapter', c.id, s.id, 1500, 2499, 'Dell', CURDATE(), '65W Laptop Adapter - sample product', 'Active' FROM categories c, suppliers s WHERE c.name = 'Power Adapters' AND s.name = 'Dell India';

INSERT IGNORE INTO products (product_code, name, category_id, supplier_id, purchase_price, selling_price, brand, product_date, description, status)
SELECT 'WAT-001', 'Galaxy Watch 6', c.id, s.id, 18000, 24999, 'Samsung', CURDATE(), 'Galaxy Watch 6 - sample product', 'Active' FROM categories c, suppliers s WHERE c.name = 'Smart Watches' AND s.name = 'Samsung India';

INSERT IGNORE INTO products (product_code, name, category_id, supplier_id, purchase_price, selling_price, brand, product_date, description, status)
SELECT 'ACC-001', 'Laptop Backpack 15.6', c.id, s.id, 700, 1499, 'HP', CURDATE(), 'Laptop Backpack 15.6 - sample product', 'Active' FROM categories c, suppliers s WHERE c.name = 'Accessories' AND s.name = 'HP India';

INSERT IGNORE INTO stock (product_id, quantity)
SELECT p.id, CASE p.product_code WHEN 'MOB-001' THEN 40 WHEN 'MOB-002' THEN 12 WHEN 'IPH-001' THEN 25 WHEN 'MAC-001' THEN 8 WHEN 'LAP-001' THEN 15 WHEN 'LAP-002' THEN 20 WHEN 'DES-001' THEN 6 WHEN 'TAB-001' THEN 10 WHEN 'CHG-001' THEN 60 WHEN 'MOU-001' THEN 3 WHEN 'KEY-001' THEN 0 WHEN 'PWR-001' THEN 18 WHEN 'WAT-001' THEN 9 WHEN 'ACC-001' THEN 30 ELSE 0 END FROM products p WHERE p.product_code IN ('MOB-001', 'MOB-002', 'IPH-001', 'MAC-001', 'LAP-001', 'LAP-002', 'DES-001', 'TAB-001', 'CHG-001', 'MOU-001', 'KEY-001', 'PWR-001', 'WAT-001', 'ACC-001');

INSERT INTO stock_movements (product_id, movement_type, quantity_change, balance_after, reference_type, note, created_by)
SELECT p.id, 'OPENING', s.quantity, s.quantity, 'OPENING', 'Opening stock', 'system' FROM products p JOIN stock s ON s.product_id = p.id WHERE s.quantity > 0 AND NOT EXISTS (SELECT 1 FROM stock_movements m WHERE m.product_id = p.id);

INSERT IGNORE INTO settings (setting_key, setting_value) VALUES
('business_name', 'Inventory Management System'),
('business_address', 'Bengaluru, Karnataka, India'),
('business_phone', '9876500000'),
('business_email', 'info@inventory.local'),
('currency_symbol', '₹'),
('low_stock_threshold', '5');
