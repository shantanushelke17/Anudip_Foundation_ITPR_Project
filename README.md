# Product Inventory System (Java + JDBC + MySQL, CLI)

A clean, beginner-friendly **command-line** CRUD project using **Java + JDBC + MySQL**.  
It demonstrates connecting to MySQL, handling user input with a menu, and performing **Create / Read / Update / Delete** operations on a `product` table.

---

## ✨ Features
- Add / list / find-by-id / update / delete products
- List **low-stock** items (<= threshold)
- **Adjust stock** with positive or negative quantity deltas
- Uses **PreparedStatement** everywhere to prevent SQL injection
- Clear, robust input handling (no crashes on wrong input)
- Plain **single-file Java** app (easy to read and submit)

---

## 🧱 Database Schema

**Database:** `inventorydb`  
**Table:** `product`

```sql
CREATE TABLE product (
  id INT PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  quantity INT NOT NULL CHECK (quantity >= 0),
  price DECIMAL(10,2) NOT NULL CHECK (price >= 0)
);
