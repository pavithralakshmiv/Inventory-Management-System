# Inventory Management System

Classic desktop-style inventory software: Java 21, Spring Boot 3, Spring Data JPA, Spring Security, MySQL 8, HTML/CSS/JavaScript, Chart.js.

## Run it

1. Install JDK 21, Maven and MySQL 8. Open the folder in VS Code (Extension Pack for Java).
2. Set your MySQL login (defaults: `root` / `root`):
   - Windows PowerShell: `$env:DB_USERNAME="root"; $env:DB_PASSWORD="yourpassword"`
   - or edit `src/main/resources/application.properties`.
3. Start: `mvn spring-boot:run`
4. Open http://localhost:8080

The database `inventory_management_db` is created automatically; `schema.sql` creates the 16 tables and `data.sql` adds sample categories, suppliers, customers and products. The same script is in `database/inventory_management_db.sql` if you prefer to run it manually in MySQL Workbench.

## First administrator (no hardcoded login)

On the first start, if no admin exists, one is created:

- Email: `admin@inventory.local` (override with env var `ADMIN_EMAIL`)
- Password: the env var `ADMIN_PASSWORD` if set, otherwise a random password printed **once** in the console log.

Passwords are stored with BCrypt. Change it in Settings after the first login. Public registration always creates an EMPLOYEE; only an admin can create admins (Manage Users).

## Roles

| Area | Admin | Employee |
|---|---|---|
| Users, User Logs, Reports | yes | no |
| Delete anything, business settings, cancel purchases | yes | no |
| Categories, Products, Customers, Suppliers, Orders, Sales, Purchases, Stock | yes | add / update / view |
| Own profile and password | yes | yes |

Rules are enforced on the server (Spring Security); hidden buttons are only cosmetic.

## Stock rules

`Final Stock = Opening Stock + Purchased - Sold`. Every change goes through one method that locks the stock row, refuses negative stock and writes a row to `stock_movements`.

- Order created or Sale completed: stock decreases (blocked if insufficient).
- Purchase completed: stock increases and the product's purchase price is updated.
- Order set to Cancelled / Sale cancelled: quantities return to stock (REVERSAL movement). Cancelled orders are final.
- Purchase cancelled (admin): quantities are removed again, only if that stock is still available.
- Profit per sale = total after discount minus purchase cost of the items.
- Products with history cannot be deleted; set them to Inactive.

All multi-record operations run in one database transaction.

## Notes

- Discounts are percentages (0 to 100).
- Forgot password: an admin sets a new password in Manage Users (no email service is configured).
- Re-starting the app re-adds any deleted sample rows from `data.sql` (INSERT IGNORE). Delete `data.sql` content if you do not want sample data.
- Uploaded product images go to the `uploads/` folder.
- Chart.js loads from a CDN, so the dashboard and reports charts need internet.
- CSRF protection is disabled because the app uses same-origin session cookies with JSON APIs; enable it before exposing the app publicly.
