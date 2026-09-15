import json
import sqlite3
import statistics
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent
JSON_OUT = ROOT / "benchmark-results.json"
TEXT_OUT = ROOT / "benchmark-results.txt"


def measure(operation, repeats=7):
    values = []
    for _ in range(repeats):
        started = time.perf_counter()
        operation()
        values.append((time.perf_counter() - started) * 1000)
    return round(statistics.median(values), 3)


def database():
    connection = sqlite3.connect(":memory:")
    connection.executescript(
        """
        CREATE TABLE dishes (id INTEGER PRIMARY KEY, name TEXT, category TEXT, price REAL);
        CREATE TABLE orders (id INTEGER PRIMARY KEY, customer_name TEXT, status TEXT, created_at TEXT);
        CREATE TABLE order_items (order_id INTEGER, dish_id INTEGER);
        """
    )
    categories = ["Супы", "Напитки", "Десерты", "Горячее", "Салаты"]
    dishes = [(index, f"Блюдо {index}", categories[index % len(categories)], 100 + index % 500) for index in range(1, 5001)]
    orders = [(index, f"Клиент {index}", "NEW", f"2026-09-{index % 28 + 1:02d}T12:00:00") for index in range(1, 1001)]
    items = [(order_id, (order_id * 5 + shift) % 5000 + 1) for order_id in range(1, 1001) for shift in range(5)]
    connection.executemany("INSERT INTO dishes VALUES (?, ?, ?, ?)", dishes)
    connection.executemany("INSERT INTO orders VALUES (?, ?, ?, ?)", orders)
    connection.executemany("INSERT INTO order_items VALUES (?, ?)", items)
    connection.commit()
    return connection


def main():
    connection = database()

    def n_plus_one():
        orders = connection.execute("SELECT id, customer_name, status FROM orders LIMIT 300").fetchall()
        return [
            (order, connection.execute(
                "SELECT d.* FROM dishes d JOIN order_items oi ON oi.dish_id=d.id WHERE oi.order_id=?",
                (order[0],),
            ).fetchall())
            for order in orders
        ]

    def single_join():
        return connection.execute(
            "SELECT o.id,o.customer_name,o.status,d.id,d.name,d.category,d.price "
            "FROM (SELECT * FROM orders LIMIT 300) o "
            "JOIN order_items oi ON oi.order_id=o.id JOIN dishes d ON d.id=oi.dish_id"
        ).fetchall()

    def linear_filter():
        rows = connection.execute("SELECT * FROM dishes").fetchall()
        return [row for row in rows if row[2].lower() == "супы"]

    before_filter = measure(linear_filter)
    connection.execute("CREATE INDEX idx_benchmark_category ON dishes(category COLLATE NOCASE)")

    def indexed_filter():
        return connection.execute("SELECT * FROM dishes WHERE category = ? COLLATE NOCASE LIMIT 100", ("Супы",)).fetchall()

    def repeated_reads():
        for _ in range(100):
            connection.execute("SELECT id,name,category,price FROM dishes LIMIT 100").fetchall()

    cached_page = connection.execute("SELECT id,name,category,price FROM dishes LIMIT 100").fetchall()

    def cached_reads():
        for _ in range(100):
            list(cached_page)

    scenarios = [
        {"name": "Загрузка заказов", "before": measure(n_plus_one), "after": measure(single_join), "before_method": "N+1", "after_method": "JOIN"},
        {"name": "Фильтрация категории", "before": before_filter, "after": measure(indexed_filter), "before_method": "загрузка всех строк и O(n)", "after_method": "индексированный SQL с LIMIT"},
        {"name": "Повторное чтение меню", "before": measure(repeated_reads), "after": measure(cached_reads), "before_method": "100 запросов SQLite", "after_method": "кэш страницы"},
    ]
    for item in scenarios:
        item["speedup"] = round(item["before"] / max(item["after"], 0.001), 1)

    result = {
        "dataset": {"dishes": 5000, "orders": 1000, "order_items": 5000},
        "unit": "milliseconds, median of 7 runs",
        "scenarios": scenarios,
    }
    JSON_OUT.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    lines = [
        "Набор данных: 5000 блюд, 1000 заказов, 5000 позиций",
        "Единица: миллисекунды, медиана 7 запусков",
        "",
        "Сценарий | До | После | Ускорение",
    ]
    for item in scenarios:
        lines.append(f"{item['name']} | {item['before']:.3f} | {item['after']:.3f} | {item['speedup']:.1f}x")
    TEXT_OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(TEXT_OUT)
    print("\n".join(lines))


if __name__ == "__main__":
    main()
