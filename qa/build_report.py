from pathlib import Path
from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.shared import Cm, Pt, RGBColor
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

GOOD = Path(r"C:\Users\блоош\IdeaProjects\proect")
BAD = Path(r"C:\Users\блоош\IdeaProjects\proecterror")
OUT = BAD / "Отчет_группа_6_ресторан.docx"
GOOD_SCREEN = BAD / "qa" / "proect_gui.png"
BAD_SCREEN = BAD / "qa" / "proecterror_gui.png"

def font(run, size=14, bold=False, name="Times New Roman", color=None):
    run.font.name = name
    props = run._element.get_or_add_rPr()
    fonts = props.rFonts
    if fonts is None:
        fonts = OxmlElement("w:rFonts"); props.insert(0, fonts)
    for attr in ("ascii", "hAnsi", "eastAsia", "cs"):
        fonts.set(qn(f"w:{attr}"), name)
    run.font.size = Pt(size); run.bold = bold
    if color: run.font.color.rgb = RGBColor(*color)

def field(paragraph, code):
    run = paragraph.add_run()
    begin = OxmlElement("w:fldChar"); begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText"); instr.set(qn("xml:space"), "preserve"); instr.text = code
    separate = OxmlElement("w:fldChar"); separate.set(qn("w:fldCharType"), "separate")
    text = OxmlElement("w:t"); text.text = "1"
    end = OxmlElement("w:fldChar"); end.set(qn("w:fldCharType"), "end")
    run._r.extend([begin, instr, separate, text, end]); font(run, size=10)

def body(doc, text, indent=True):
    p = doc.add_paragraph(); p.paragraph_format.line_spacing = 1.5
    p.paragraph_format.space_after = Pt(4)
    if indent: p.paragraph_format.first_line_indent = Cm(1.25)
    font(p.add_run(text)); return p

def heading(doc, text):
    p = doc.add_paragraph(); p.paragraph_format.keep_with_next = True
    p.paragraph_format.space_before = Pt(12); p.paragraph_format.space_after = Pt(6)
    font(p.add_run(text), size=14, bold=True); return p

def shade(cell, fill):
    shd = OxmlElement("w:shd"); shd.set(qn("w:fill"), fill)
    cell._tc.get_or_add_tcPr().append(shd)

def table(doc, headers, rows, widths=None):
    t = doc.add_table(rows=1, cols=len(headers)); t.style = "Table Grid"
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    t.rows[0]._tr.get_or_add_trPr().append(OxmlElement("w:tblHeader"))
    for i, text in enumerate(headers):
        cell = t.rows[0].cells[i]; shade(cell, "1F4E78")
        cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
        p = cell.paragraphs[0]; p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        font(p.add_run(text), size=10, bold=True, color=(255,255,255))
    for row_no, values in enumerate(rows):
        cells = t.add_row().cells
        for i, text in enumerate(values):
            if row_no % 2: shade(cells[i], "EAF2F8")
            cells[i].vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            p = cells[i].paragraphs[0]; p.paragraph_format.space_after = Pt(0)
            font(p.add_run(str(text)), size=9)
    if widths:
        for row in t.rows:
            for i, width in enumerate(widths): row.cells[i].width = Cm(width)
    doc.add_paragraph().paragraph_format.space_after = Pt(0)
    return t

def code(doc, title, lines):
    p = doc.add_paragraph(); p.paragraph_format.keep_with_next = True
    font(p.add_run(title), size=11, bold=True)
    for line in lines:
        p = doc.add_paragraph(); p.paragraph_format.left_indent = Cm(.6)
        p.paragraph_format.space_after = Pt(0); p.paragraph_format.line_spacing = 1
        font(p.add_run(line.rstrip()), size=8, name="Consolas")

def caption(doc, text):
    p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(3); p.paragraph_format.space_after = Pt(8)
    font(p.add_run(text), size=11)

doc = Document(); sec = doc.sections[0]
sec.top_margin = Cm(2); sec.bottom_margin = Cm(2)
sec.left_margin = Cm(2.5); sec.right_margin = Cm(1.5)
normal = doc.styles["Normal"]; normal.font.name = "Times New Roman"; normal.font.size = Pt(14)
normal._element.rPr.rFonts.set(qn("w:ascii"), "Times New Roman")
normal._element.rPr.rFonts.set(qn("w:hAnsi"), "Times New Roman")
footer = sec.footer.paragraphs[0]; footer.alignment = WD_ALIGN_PARAGRAPH.CENTER; field(footer, "PAGE")

p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER; p.paragraph_format.space_before = Cm(2.5)
font(p.add_run("ОТЧЁТ\nПО ПРОЕКТНОЙ РАБОТЕ"), size=18, bold=True)
p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER; p.paragraph_format.space_before = Cm(1.2)
font(p.add_run("Вариант (группа) 6\n«Управление меню и заказами ресторана»"), size=16, bold=True)
p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER; p.paragraph_format.space_before = Cm(1.5)
font(p.add_run("Две версии программного продукта:\nproect — исправленная; proecterror — с учебными ошибками"))
p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.RIGHT; p.paragraph_format.space_before = Cm(5)
font(p.add_run("Выполнил(а): ____________________\nПроверил(а): ____________________"))
p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER; p.paragraph_format.space_before = Cm(3)
font(p.add_run("2026")); doc.add_page_break()

heading(doc, "1. Цель и постановка задачи")
body(doc, "Цель работы — разработать Java-приложение для управления меню и заказами ресторана, подготовить две функционально сопоставимые версии и показать на их сравнении ошибки рефакторинга и оптимизации.")
body(doc, "В соответствии с заданием варианта 6 реализованы два проекта: proect с исправленной архитектурой и proecterror с намеренно сохранёнными пятью ошибками рефакторинга и пятью ошибками оптимизации. Обе версии используют графический интерфейс JavaFX и постоянное хранение данных в SQLite.")
heading(doc, "2. Функциональные требования")
table(doc, ["Функция", "proect", "proecterror"], [
    ("Просмотр меню", "Реализовано", "Реализовано"), ("Добавление блюда", "С проверкой данных", "Реализовано"),
    ("Поиск по названию", "Реализовано", "Реализовано"), ("Фильтр по категории", "Реализовано", "Реализовано"),
    ("Выбор нескольких блюд", "Множественный выбор", "Множественный выбор"), ("Создание заказа", "Реализовано", "Реализовано"),
    ("Удаление/очистка позиций", "Реализовано", "Удаление позиции"), ("История заказов", "SQLite, сохраняется", "SQLite, сохраняется"),
    ("Изменение статуса", "Текущий заказ", "Выбранный заказ истории"), ("Расчёт выручки", "Реализовано", "С учебной ошибкой"),
], [5.4,5.1,5.1])

heading(doc, "3. Средства разработки")
body(doc, "Язык программирования — Java 17. Для пользовательского интерфейса применена JavaFX 17.0.12, для доступа к базе данных — JDBC-драйвер Xerial sqlite-jdbc 3.42.0.0. Сборка описана в Maven-файлах pom.xml; точкой входа служит класс Main.")
heading(doc, "4. Архитектура исправленного проекта proect")
table(doc, ["Класс", "Назначение"], [
    ("Dish", "Неизменяемая модель блюда; проверяет идентификатор, название, категорию и цену."),
    ("NutritionInfo", "Группирует вес и калорийность блюда."), ("Customer", "Группирует имя клиента и номер телефона."),
    ("Order", "Хранит позиции, клиента, статус, время и вычисленную стоимость."), ("OrderStatus", "Перечисляет допустимые статусы."),
    ("RestaurantDAO", "Создаёт схему SQLite и выполняет параметризованные SQL-запросы."),
    ("RestaurantController", "Содержит прикладные операции над блюдами и заказами."),
    ("RestaurantApp", "Формирует JavaFX-интерфейс; запускает обращения к SQLite в Task."), ("Main", "Запускает RestaurantApp."),
], [4.2,11.4])

heading(doc, "5. Хранение данных в SQLite")
body(doc, "В проекте proect используется файл restaurant.db, в proecterror — restaurant-error.db. При первом запуске создаются таблицы dishes, orders и order_items. Начальные блюда добавляются только в пустую базу, поэтому повторный запуск не удаляет историю заказов.")
table(doc, ["Таблица", "Основные данные"], [("dishes", "Блюда меню и характеристики."), ("orders", "Клиент, телефон, статус, время и итог."), ("order_items", "Связь заказа с выбранными блюдами.")], [4.2,11.4])
body(doc, "В исправленной версии SQL выполняется через PreparedStatement, добавление и удаление заказа защищены транзакциями, а загрузка заказов и позиций выполняется одним JOIN-запросом. Это устраняет SQL-конкатенацию и проблему N+1.")

heading(doc, "6. Отсутствие зависания интерфейса в proect")
body(doc, "Все операции, которые обращаются к SQLite, передаются в фоновый javafx.concurrent.Task. После завершения результат возвращается в setOnSucceeded, который безопасно обновляет интерфейс. Во время операции выводится сообщение «Работа с базой...». Длительный запрос поэтому не блокирует окно.")
good_app = (GOOD / "src" / "RestaurantApp.java").read_text(encoding="utf-8").splitlines()
start = next(i for i,line in enumerate(good_app) if "private <T> void runDb" in line)
code(doc, "Листинг 1 — запуск работы с БД в фоновом Task", good_app[start:start+2])

doc.add_page_break(); heading(doc, "7. Интерфейс исправленного проекта")
body(doc, "Окно proect содержит меню с поиском и фильтром, форму добавления блюда, данные клиента, текущий заказ, изменение статуса, историю и общую выручку. Несколько строк выбираются с помощью Ctrl или Shift и добавляются одной кнопкой.")
doc.add_picture(str(GOOD_SCREEN), width=Cm(16.2)); caption(doc, "Рисунок 1 — интерфейс исправленного проекта proect после повторного запуска")

doc.add_page_break(); heading(doc, "8. Интерфейс проекта с учебными ошибками")
body(doc, "В proecterror доступны добавление блюда, поиск, фильтр категорий, множественный выбор, оформление и история. Для смены статуса требуется выделить заказ в правом списке, выбрать статус и нажать «Изменить статус выбранного заказа». База при запуске не очищается.")
doc.add_picture(str(BAD_SCREEN), width=Cm(16.2)); caption(doc, "Рисунок 2 — интерфейс proecterror с фильтром и сменой статуса")

heading(doc, "9. Ошибки рефакторинга в proecterror")
table(doc, ["№", "Ошибка", "Исправление в proect"], [
    (1,"Публичные поля моделей нарушают инкапсуляцию.","private final и методы доступа."),
    (2,"Конструктор Dish имеет длинный список параметров.","Параметры питания вынесены в NutritionInfo."),
    (3,"Order содержит группу customerName/customerPhone.","Данные объединены в Customer."),
    (4,"Статус NEW задан магической строкой.","Использован enum OrderStatus."),
    (5,"Feature Envy: контроллер перебирает позиции Order.","Расчёт инкапсулирован в Order."),
], [1,7.1,8])
heading(doc, "10. Ошибки оптимизации в proecterror")
table(doc, ["№", "Ошибка", "Исправление в proect"], [
    (1,"Стоимость полностью пересчитывается при каждом изменении.","Итог вычисляет модель неизменяемого заказа."),
    (2,"N+1: отдельный запрос позиций каждого заказа.","Единый JOIN-запрос."),
    (3,"SQL формируется конкатенацией строк.","PreparedStatement и параметры."),
    (4,"Фильтр каждый раз линейно обходит данные после DAO.","Меню кэшируется; повторного запроса к БД нет."),
    (5,"toLowerCase() многократно вызывается для тех же значений.","Запрос нормализуется один раз."),
], [1,7.1,8])

heading(doc, "11. Сравнение проектов")
body(doc, "Проекты имеют одну предметную область и близкий интерфейс, но различаются внутренним устройством. proect демонстрирует инкапсуляцию, объекты-значения, enum, транзакции, PreparedStatement, JOIN и фоновый Task. proecterror остаётся работоспособным специально для демонстрации последствий открытых полей, длинных параметров, магических строк, Feature Envy и неэффективного доступа к данным.")
heading(doc, "12. Проверка работоспособности")
table(doc, ["Проверка", "Результат"], [
    ("Компиляция proect", "Успешно, код javac 0."), ("Компиляция proecterror", "Успешно, код javac 0."),
    ("Запуск proect", "Окно открыто; история и выручка загружены из restaurant.db."),
    ("Запуск proecterror", "Окно открыто; история сохранена в restaurant-error.db."),
    ("Повторный запуск", "Начальные данные не дублируются, таблицы не очищаются."),
    ("Фильтр и поиск", "Присутствуют в обеих версиях."),
    ("Смена статуса в proecterror", "Выбирается заказ истории, статус сохраняется через DAO."),
], [8.2,7.9])
body(doc, "При JDK 24 SQLite может выводить предупреждение native access. Оно не мешает запуску. Для его скрытия указывается VM option --enable-native-access=ALL-UNNAMED.")
heading(doc, "13. Порядок запуска")
body(doc, "В IntelliJ IDEA нужно открыть папку проекта, дождаться загрузки Maven-зависимостей, выбрать src/Main.java и выполнить Run 'Main.main()'. Сначала можно показать proect, затем proecterror. SQLite-файлы создаются рядом с проектами автоматически.")
heading(doc, "Заключение")
body(doc, "Задание варианта 6 выполнено: созданы два JavaFX-проекта ресторана, подключено постоянное хранение SQLite, реализованы меню, поиск, фильтрация, работа с несколькими блюдами, заказы, статусы, история и выручка. В proect устранены пять ошибок рефакторинга и пять ошибок оптимизации, а обращения к базе вынесены из JavaFX-потока. В proecterror намеренные ошибки сохранены и отмечены комментариями, но пользовательский интерфейс пригоден для сравнительной демонстрации.")

doc.save(OUT); print(OUT)
