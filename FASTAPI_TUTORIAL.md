# FastAPI 快速入门：给零 Python 经验的 Flutter 工程师

> 目标读者：会 Dart、Flutter、REST API，但没有 Python 后端开发经验。
>
> 最终目标：看懂并能修改本仓库的三个项目，独立完成一个连接数据库、带测试和权限控制的 FastAPI 后台接口。

这不是一份按概念堆砌的 Python 或 FastAPI 百科。学习主线始终围绕仓库中的项目：

1. 任务管理 API：掌握 HTTP、数据校验、数据库 CRUD、依赖注入和测试。
2. RBAC 项目协作 API：掌握登录、JWT、密码哈希、角色权限和资源权限。
3. 订单库存 API：掌握事务、库存一致性、幂等、金额快照和服务层。

## 0. 先明确学习结果

完成教程后，你应该能够：

- 读懂项目中使用到的 Python 语法，不要求先成为 Python 专家。
- 使用 FastAPI 定义 GET、POST、PATCH、DELETE 接口。
- 区分路径参数、查询参数、请求体、请求头和依赖参数。
- 使用 SQLModel 定义表模型以及请求、响应模型。
- 理解 engine、session、commit、rollback 和事务边界。
- 使用 `Depends` 管理数据库会话、当前用户和权限。
- 使用 `APIRouter` 和 service 层组织后台项目。
- 使用 JWT、密码哈希和 RBAC 保护接口。
- 为接口编写能隔离数据库的自动化测试。
- 从 Flutter 使用 Dio 调用接口并处理常见错误。
- 知道从 SQLite 学习项目升级到 PostgreSQL 生产项目还缺什么。

### 三个项目的递进关系

| 阶段 | 运行端口 | 你要解决的问题 | 核心目录 |
| --- | --- | --- | --- |
| 1. Task CRUD | 8000 | 请求如何进入数据库再返回 JSON | `app/` |
| 2. RBAC Project | 8001 | 谁可以访问和修改哪些资源 | `projects/rbac_project_api/` |
| 3. Order Inventory | 8002 | 多张表和库存如何保持一致 | `projects/order_inventory_api/` |

建议先完整走通阶段 1，再进入后两个项目。直接从 JWT 或事务开始，容易把 Python、FastAPI 和业务规则混在一起。

### 这份教程怎么快速使用

不要一次从头读到尾：

1. 第一遍只完成第 1 到 4 章，把服务跑起来。
2. 第二遍边操作边读第 5 到 10 章，并完成 Task 动手任务。
3. 阶段一测试通过后，再分别学习第 11、12 章。
4. 第 13 章在 Flutter 联调时再读，第 14、15 章用于建立生产认知。

每次只解决当前阶段的问题。代码能运行、能解释、能修改、测试通过后，再向下一阶段推进。

## 1. Python 最小必备：从 Dart 翻译过来

你不需要先学完 Python。先掌握仓库里会反复出现的语法即可。

### 1.1 Dart 与 Python 类型对照

| Dart | Python | 说明 |
| --- | --- | --- |
| `String` | `str` | 字符串 |
| `int` | `int` | 整数 |
| `double` | `float` | 浮点数 |
| `bool` | `bool` | 布尔值 |
| `null` | `None` | 空值 |
| `List<Task>` | `list[Task]` | 列表 |
| `Map<String, dynamic>` | `dict[str, object]` | JSON 对象 |
| `String?` | `str \| None` | 可空字符串 |
| `Future<Task>` | `async def` 返回的协程 | 异步结果 |
| `throw Exception()` | `raise Exception()` | 抛出异常 |

Python 的 `True`、`False`、`None` 首字母大写。

### 1.2 缩进就是代码块

Dart 使用花括号：

```dart
if (done) {
  print('finished');
}
```

Python 使用冒号和缩进：

```python
if done:
    print("finished")
```

统一使用 4 个空格。缩进错误不是格式问题，而是语法或逻辑错误。

### 1.3 函数与类型提示

```python
def calculate_total(price_cents: int, quantity: int) -> int:
    return price_cents * quantity
```

对应 Dart：

```dart
int calculateTotal(int priceCents, int quantity) {
  return priceCents * quantity;
}
```

Python 的类型提示默认主要用于工具、框架和阅读代码，不像 Dart 编译器那样阻止所有错误。FastAPI 会读取这些类型提示，用它们完成请求转换、校验和 OpenAPI 文档生成。

### 1.4 类、继承和 `self`

```python
class TaskBase(SQLModel):
    title: str
    done: bool = False
```

`TaskBase(SQLModel)` 表示继承 `SQLModel`。实例方法的第一个参数通常写 `self`，作用接近 Dart 中隐式存在的 `this`。

```python
class Counter:
    def __init__(self, value: int = 0):
        self.value = value

    def increment(self) -> None:
        self.value += 1
```

### 1.5 import 与包

```python
from fastapi import FastAPI
from app.models import Task
```

第一行从第三方包导入，第二行从本项目的 `app/models.py` 导入。

`__init__.py` 表示目录可以作为 Python 包使用。你不需要在里面写代码，它也可以只是空文件。

### 1.6 装饰器：`@app.get` 是什么

```python
@app.get("/tasks/{task_id}")
def get_task(task_id: int):
    return {"task_id": task_id}
```

`@app.get(...)` 是装饰器。可以把它理解为把下面的函数注册到 FastAPI 路由表：

- HTTP 方法：GET
- 路径：`/tasks/{task_id}`
- 处理函数：`get_task`

它不是注释，也不是 Flutter Widget annotation。函数定义完成时，装饰器就会执行注册。

### 1.7 `Annotated` 与 `Depends`

项目中会看到：

```python
SessionDep = Annotated[Session, Depends(get_session)]
```

可以拆成两部分理解：

- `Session`：编辑器和 Python 看到的类型。
- `Depends(get_session)`：FastAPI 看到的依赖规则。

它接近 Flutter 中通过 Riverpod 或 Provider 声明依赖，只是依赖的生命周期通常以一次 HTTP 请求为单位。

### 1.8 `with` 与 `yield`

```python
def get_session():
    with Session(engine) as session:
        yield session
```

请求生命周期是：

1. 进入 `with`，创建数据库 session。
2. `yield session` 把 session 提供给路由函数。
3. 路由处理完成。
4. 回到依赖函数，退出 `with` 并关闭 session。

现在不需要系统学习 generator。先记住：在 FastAPI 的 `yield` 依赖中，`yield` 前是准备资源，`yield` 后是清理资源。

### 1.9 `async def` 与 Dart `Future`

Dart：

```dart
Future<User> loadUser() async {
  return await api.getUser();
}
```

Python：

```python
async def load_user() -> User:
    return await api.get_user()
```

本仓库的 SQLModel 使用同步 session，所以数据库路由主要写普通 `def`。FastAPI 可以混合处理 `def` 和 `async def`。

实用规则：

- 调用的库要求 `await`，使用 `async def`。
- 使用当前同步 SQLModel session，路由使用普通 `def`。
- 不要为了看起来高级，把所有函数都改成 `async def`。
- 不要在 `async def` 中直接执行耗时的同步网络或磁盘操作，它会阻塞事件循环。

### 1.10 读懂这几个 Python 表达式

```python
task: Task | None
```

表示 task 是 `Task` 或 `None`，对应 Dart 的 `Task?`。

```python
items = [item.product_id for item in payload.items]
```

这是列表推导式，对应：

```dart
final items = payload.items.map((item) => item.productId).toList();
```

```python
data = payload.model_dump(exclude_unset=True)
```

把模型转成字典，并只保留客户端实际提交的字段。PATCH 更新会用到它。

## 2. 环境准备：先让项目稳定运行

本仓库要求 Python 3.10 以上。这台机器已经安装 Python 3.12，默认 `python3` 仍是 3.9，所以明确使用 `python3.12` 创建虚拟环境。

在仓库根目录执行：

```bash
python3.12 --version
python3.12 -m venv .venv
source .venv/bin/activate
python -m pip install -U pip
python -m pip install -e ".[dev]"
```

激活虚拟环境后，命令行通常会出现 `(.venv)`。每次打开新的终端，都需要重新执行：

```bash
source .venv/bin/activate
```

验证环境：

```bash
python --version
python -c "import fastapi, sqlmodel; print('environment ok')"
```

### 2.1 与 Flutter 工具链的类比

| Flutter | Python |
| --- | --- |
| `pubspec.yaml` | `pyproject.toml` |
| `flutter pub get` | `python -m pip install ...` |
| 项目 SDK/依赖环境 | `.venv` 虚拟环境 |
| `flutter test` | `pytest` |
| `flutter run` | `fastapi dev` 或 `uvicorn` |

`pyproject.toml` 声明运行依赖和开发依赖；`.venv` 保存当前项目自己的 Python 与依赖，不应提交到 Git。

## 3. 先建立 HTTP 心智模型

Flutter 调用后端时，一次请求至少包含：

- method：GET、POST、PATCH、DELETE。
- path：例如 `/tasks/1`。
- query：例如 `?offset=0&limit=20`。
- headers：例如 `Authorization`、`Content-Type`。
- body：通常是 JSON。
- response：状态码、headers 和 JSON。

FastAPI 的核心工作是把这些 HTTP 数据转换成有类型的 Python 参数，再把返回值转换成 JSON。

### 3.1 参数来源如何判断

```python
@router.patch("/{task_id}", response_model=TaskRead)
def update_task(
    task_id: int,
    task_update: TaskUpdate,
    session: SessionDep,
) -> Task:
    ...
```

FastAPI 会判断：

| 参数 | 来源 | 原因 |
| --- | --- | --- |
| `task_id` | 路径参数 | 路径中存在 `{task_id}` |
| `task_update` | JSON 请求体 | 它是 SQLModel/Pydantic 模型 |
| `session` | 依赖注入 | `SessionDep` 包含 `Depends` |

再看分页：

```python
def list_tasks(offset: int = 0, limit: int = 20):
    ...
```

`offset` 和 `limit` 不在路径中，又是简单类型，所以会成为查询参数。

### 3.2 常见状态码

| 状态码 | 含义 | 项目中的例子 |
| --- | --- | --- |
| 200 | 查询或更新成功 | 查询任务 |
| 201 | 资源创建成功 | 创建任务、订单 |
| 204 | 成功但没有响应体 | 删除任务 |
| 400 | 请求不符合业务要求 | 可用于一般业务错误 |
| 401 | 没有有效身份 | JWT 无效或过期 |
| 403 | 身份有效但权限不足 | member 创建项目 |
| 404 | 资源不存在或不可见 | 查询不存在的项目 |
| 409 | 当前状态发生冲突 | 库存不足、幂等键冲突 |
| 422 | 输入未通过模型校验 | 缺字段、类型错误 |
| 500 | 未处理的服务器错误 | 代码异常 |

Flutter 端不要只判断请求是否抛异常，还要读取 HTTP 状态码和响应中的 `detail`。

## 4. 第一次运行 FastAPI

启动根目录任务项目：

```bash
source .venv/bin/activate
fastapi dev app/main.py
```

也可以直接启动 Uvicorn：

```bash
python -m uvicorn app.main:app --reload --port 8000
```

`app.main:app` 的含义是：

- 导入 `app/main.py` 模块。
- 找到模块里的 `app = FastAPI(...)` 对象。

打开：

- `http://127.0.0.1:8000/health`
- `http://127.0.0.1:8000/docs`
- `http://127.0.0.1:8000/redoc`

最小 FastAPI 应用是：

```python
from fastapi import FastAPI

app = FastAPI()

@app.get("/")
def root() -> dict[str, str]:
    return {"message": "Hello World"}
```

项目中的 `app/main.py` 在此基础上增加了数据库初始化和路由注册。

### 本章验收

- 能打开 `/docs`。
- 能在 Swagger UI 中执行 `GET /health`。
- 能解释 `app.main:app` 的两个 `app` 分别是什么。
- 能用 `Control+C` 停止服务器再重新启动。

## 5. 阶段一项目：任务 CRUD 的完整请求链

一次创建任务请求会经过：

```text
Flutter / Swagger / curl
        |
        v
POST /tasks + JSON
        |
        v
TaskCreate 校验
        |
        v
tasks.py 路由函数
        |
        v
SessionDep -> SQLite
        |
        v
TaskRead 响应过滤
        |
        v
201 + JSON
```

### 5.1 目录职责

```text
.
├── app
│   ├── main.py
│   ├── database.py
│   ├── models.py
│   └── routers
│       └── tasks.py
├── tests
│   └── test_tasks.py
├── projects
│   ├── rbac_project_api
│   └── order_inventory_api
├── pyproject.toml
└── FASTAPI_TUTORIAL.md
```

| 文件 | 只负责什么 |
| --- | --- |
| `app/main.py` | 创建应用、生命周期、注册路由 |
| `app/database.py` | engine、session 和数据库依赖 |
| `app/models.py` | 表模型、请求模型、响应模型 |
| `app/routers/tasks.py` | HTTP 接口和简单 CRUD |
| `tests/test_tasks.py` | 接口行为测试 |

先从 `app/main.py` 开始读，再看 router、model、database，最后看测试。不要从任意文件跳着读。

## 6. 数据模型：JSON 契约与数据库表要分开

`app/models.py` 中的核心设计：

```python
class TaskBase(SQLModel):
    title: str = Field(index=True, min_length=1, max_length=120)
    description: str | None = Field(default=None, max_length=1000)
    done: bool = False


class Task(TaskBase, table=True):
    id: int | None = Field(default=None, primary_key=True)


class TaskCreate(TaskBase):
    pass


class TaskRead(TaskBase):
    id: int


class TaskUpdate(SQLModel):
    title: str | None = Field(default=None, min_length=1, max_length=120)
    description: str | None = Field(default=None, max_length=1000)
    done: bool | None = None
```

### 6.1 为什么有五个模型

| 模型 | 用途 |
| --- | --- |
| `TaskBase` | 复用公共字段 |
| `Task` | 数据库表，`table=True` |
| `TaskCreate` | POST 请求体 |
| `TaskRead` | 对外响应 |
| `TaskUpdate` | PATCH 请求体，字段都可选 |

`pass` 表示类体暂时没有新增内容。`TaskCreate` 仍然继承了 `TaskBase` 的所有字段。

### 6.2 `id` 为什么在表里可以为空

创建 Python 对象时，数据库还没有生成主键，所以表模型写：

```python
id: int | None = Field(default=None, primary_key=True)
```

写入数据库并刷新后，`id` 就会有值。返回客户端的 `TaskRead` 则要求 `id: int`，因为已保存的数据必须有 ID。

### 6.3 `Field` 同时表达规则

```python
title: str = Field(index=True, min_length=1, max_length=120)
```

它表达：

- 输入必须是字符串。
- 长度为 1 到 120。
- 数据库为该字段创建索引。

### 6.4 为什么不能所有地方都返回数据库模型

表模型可能包含密码哈希、内部状态、删除标记等敏感字段。`response_model=TaskRead` 会校验并过滤输出，只返回接口契约允许的字段。

这接近 Flutter 中把数据库 entity、domain model 和 API DTO 分开，而不是让一份 model 承担所有职责。

## 7. 数据库：engine、session 与事务

`app/database.py`：

```python
DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./app.db")
connect_args = (
    {"check_same_thread": False}
    if DATABASE_URL.startswith("sqlite")
    else {}
)
engine = create_engine(DATABASE_URL, connect_args=connect_args)
```

三个概念：

- engine：管理数据库连接能力，应用通常只创建一个。
- session：一次业务操作中查询、追踪和写入对象的工作单元。
- transaction：一组要么全部成功、要么全部失败的数据库操作。

默认地址 `sqlite:///./app.db` 会在仓库根目录生成 `app.db`。

### 7.1 每个请求获得自己的 session

```python
def get_session() -> Generator[Session, None, None]:
    with Session(engine) as session:
        yield session


SessionDep = Annotated[Session, Depends(get_session)]
```

路由只声明需要 session：

```python
def create_task(task: TaskCreate, session: SessionDep):
    ...
```

FastAPI 负责创建和关闭它。不要在每个路由里重复连接数据库。

### 7.2 `add`、`commit`、`refresh`

```python
session.add(db_task)
session.commit()
session.refresh(db_task)
```

- `add`：把对象加入当前 session。
- `commit`：提交事务，让数据正式写入数据库。
- `refresh`：重新从数据库读取对象，包括自动生成的 ID。

订单项目还会使用：

- `flush`：把 SQL 发送给数据库、获得 ID，但暂时不提交整个事务。
- `rollback`：发生错误时撤销当前事务中尚未提交的修改。

## 8. CRUD 路由逐个看

### 8.1 创建

```python
@router.post("", response_model=TaskRead, status_code=status.HTTP_201_CREATED)
def create_task(task: TaskCreate, session: SessionDep) -> Task:
    db_task = Task.model_validate(task)
    session.add(db_task)
    session.commit()
    session.refresh(db_task)
    return db_task
```

关键点：

1. JSON 先校验为 `TaskCreate`。
2. `Task.model_validate(task)` 转成表模型。
3. 保存并提交。
4. 返回值按 `TaskRead` 输出。
5. 状态码是 201。

请求：

```bash
curl -X POST http://127.0.0.1:8000/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"学习 FastAPI","description":"完成 CRUD","done":false}'
```

### 8.2 列表与分页

```python
statement = select(Task).offset(offset).limit(limit)
tasks = session.exec(statement).all()
```

`select(Task)` 构造 SQL 查询，`exec` 执行，`all` 读取所有结果。

请求：

```bash
curl "http://127.0.0.1:8000/tasks?offset=0&limit=20"
```

项目把 `limit` 限制在 1 到 100，防止一次请求无限读取数据。

### 8.3 查询单个与 404

```python
task = session.get(Task, task_id)
if task is None:
    raise HTTPException(status_code=404, detail="Task not found")
return task
```

`raise` 会立刻中断函数，FastAPI 把 `HTTPException` 转成 HTTP 响应：

```json
{"detail": "Task not found"}
```

### 8.4 PATCH 部分更新

```python
task_data = task_update.model_dump(exclude_unset=True)
task.sqlmodel_update(task_data)
```

假设客户端只发：

```json
{"done": true}
```

`exclude_unset=True` 保证只更新 `done`，不会把未提交的 `title` 或 `description` 覆盖掉。

### 8.5 删除

```python
session.delete(task)
session.commit()
```

成功返回 204，没有 JSON 响应体。

### 本章验收

按顺序完成：

1. POST 创建任务。
2. GET 列表找到它。
3. GET 单条读取它。
4. PATCH 只修改 `done`。
5. DELETE 删除它。
6. 再 GET 单条，确认返回 404。
7. 故意提交空 `title`，观察 422 响应。

## 9. APIRouter、依赖注入与应用生命周期

### 9.1 APIRouter

`app/routers/tasks.py`：

```python
router = APIRouter(prefix="/tasks", tags=["tasks"])
```

因此文件里的 `@router.get("")` 最终路径是 `GET /tasks`。

`app/main.py` 注册 router：

```python
app.include_router(tasks.router)
```

企业项目一般按业务模块拆分 router，例如 `auth`、`users`、`projects`、`orders`，不要把全部接口堆在 `main.py`。

### 9.2 依赖注入不只是数据库

后面的 RBAC 项目使用同一个机制注入：

- bearer token。
- 当前登录用户。
- 角色检查。
- 数据库 session。

依赖可以继续依赖其他依赖，FastAPI 会构建请求级依赖图，并把依赖需要的输入同步到 OpenAPI 文档。

### 9.3 lifespan

`app/main.py`：

```python
@asynccontextmanager
async def lifespan(app: FastAPI):
    create_db_and_tables()
    yield
```

- `yield` 前：应用启动时执行一次。
- `yield` 后：应用关闭时执行清理。
- 当前项目在启动时创建数据库表。

这接近 Flutter 应用或服务的初始化与 dispose 生命周期。

注意：`create_all` 只会创建缺失的表，不会可靠地修改已有表结构。它适合学习和原型，不是生产数据库迁移方案。

## 10. 测试：验证行为而不是手动点到疲劳

运行全部测试：

```bash
python -m pytest -q
```

根目录 `tests/test_tasks.py` 使用内存 SQLite 和 `StaticPool`，不会污染 `app.db`。

核心做法：

```python
def get_test_session():
    with Session(engine) as session:
        yield session

app.dependency_overrides[get_session] = get_test_session
```

测试把生产数据库依赖替换成测试数据库依赖。路由代码不用修改，这正是依赖注入带来的可测试性。

一个 CRUD 流程测试应该验证：

- POST 返回 201。
- 返回 JSON 字段正确。
- GET 列表能查到数据。
- PATCH 修改成功。
- DELETE 返回 204。
- 删除后 GET 返回 404。

### 10.1 阶段一动手任务

不要新增第四个项目。直接修改当前任务项目。

任务 A：增加优先级。

1. 在 `TaskBase` 增加 `priority`，范围 0 到 5。
2. 在 `TaskUpdate` 增加可选 `priority`。
3. 在 Swagger 创建和更新优先级。
4. 在测试中断言优先级。

提示：

```python
priority: int = Field(default=0, ge=0, le=5, index=True)
```

任务 B：按完成状态过滤。

让下面的请求只返回已完成任务：

```text
GET /tasks?done=true
```

提示：

```python
statement = select(Task)
if done is not None:
    statement = statement.where(Task.done == done)
```

任务 C：补测试。

至少增加：

- `priority=6` 返回 422。
- `done=true` 只返回已完成任务。

如果你修改了表结构，`create_all` 不会修改旧表。学习阶段可以停止服务并删除可丢弃的 `app.db` 后重启；生产项目必须使用迁移。

### 阶段一通过标准

只有全部满足才进入 RBAC：

- 能不看答案新增一个字段。
- 能写一个查询过滤条件。
- 能解释请求模型和响应模型为什么不同。
- 能解释 session 为什么由 `Depends` 提供。
- `python -m pytest -q` 通过。

## 11. 阶段二项目：JWT、RBAC 与资源权限

启动：

```bash
python -m uvicorn projects.rbac_project_api.app.main:app \
  --reload --port 8001
```

打开 `http://127.0.0.1:8001/docs`。

### 11.1 先区分三个概念

| 概念 | 要回答的问题 | 项目中的实现 |
| --- | --- | --- |
| Authentication 认证 | 你是谁 | 用户名、密码、JWT |
| Authorization 授权 | 你能做什么 | admin、manager、member |
| Resource permission 资源权限 | 你能操作哪一条数据 | 项目负责人或项目成员 |

只有 RBAC 不够。一个 manager 可以创建项目，不等于他可以修改所有其他 manager 的项目。

### 11.2 登录链路

```text
用户名 + 密码
      |
      v
校验 Argon2 密码哈希
      |
      v
签发带过期时间的 JWT
      |
      v
Authorization: Bearer <token>
      |
      v
get_current_user
      |
      v
require_roles
      |
      v
项目归属 / 成员关系检查
```

阅读顺序：

1. `app/models.py`：用户、角色、项目、成员关系。
2. `app/security.py`：密码哈希与 JWT 编解码。
3. `app/dependencies.py`：当前用户和角色依赖。
4. `app/routers/auth.py`：初始化管理员和登录。
5. `app/services/project_service.py`：资源级权限。
6. `tests/test_projects.py`：完整权限链路。

### 11.3 第一个管理员为什么单独初始化

`POST /auth/bootstrap` 只允许数据库中没有用户时调用。否则会出现“创建第一个管理员需要管理员权限”的循环问题。

练习顺序：

1. `POST /auth/bootstrap` 创建 admin。
2. 在 Swagger 点击 Authorize，填写 admin 的用户名和密码；Swagger 会自动调用 `POST /auth/token`。
3. admin 创建 manager 和 member。
4. 退出 Authorize 后，用 manager 的用户名和密码重新授权。
5. manager 创建项目并把 member 加入项目。
6. 用 member 登录，验证加入前后可见范围。
7. 验证 member 创建项目返回 403。

使用 curl、Flutter 或其他客户端时，则需要自己调用 `POST /auth/token` 取得 token，并发送 `Authorization: Bearer <token>`。

### 11.4 `Depends` 如何组成权限链

```python
CurrentUserDep = Annotated[User, Depends(get_current_user)]

AdminUserDep = Annotated[
    User,
    Depends(require_roles(UserRole.ADMIN)),
]
```

FastAPI 会先解析 bearer token，再查用户，然后检查角色。路由只声明它需要哪种用户：

```python
def create_user(..., _admin: AdminUserDep):
    ...
```

下划线前缀表示路由不需要读取这个值，但需要依赖完成权限检查。

### 11.5 为什么不可见项目返回 404

如果接口对“存在但无权访问”的项目统一返回 404，可以减少攻击者枚举资源 ID 的信息。403 和 404 的选择是接口安全设计的一部分，不只是代码风格。

### 11.6 JWT 不是加密数据仓库

JWT 的内容可以被读取，它主要依靠签名防篡改。不要在 token 中放密码或敏感隐私。本项目只把用户名放在 `sub` 中，并设置过期时间。

生产项目还需要考虑：

- 强随机密钥和密钥轮换。
- refresh token 和 token 撤销。
- 登录限流。
- HTTPS。
- 审计日志。
- 外部身份提供商或 OpenID Connect。

### 阶段二动手任务

1. 增加 `PATCH /users/{id}/active`，仅 admin 可禁用用户。
2. 禁用后，旧 token 调用 `GET /auth/me` 应返回 401。
3. 增加删除项目成员接口，仅负责人或 admin 可调用。
4. 为两条接口补测试。

### 阶段二通过标准

- 能解释 401 和 403 的区别。
- 能画出 token 到当前用户的依赖链。
- 能区分角色权限和资源权限。
- 能新增一个受保护接口和对应测试。
- `pytest projects/rbac_project_api/tests -q` 通过。

## 12. 阶段三项目：订单、库存与事务

启动：

```bash
python -m uvicorn projects.order_inventory_api.app.main:app \
  --reload --port 8002
```

打开 `http://127.0.0.1:8002/docs`。

阅读顺序：

1. `app/models.py`：商品、订单、订单项、库存流水、幂等记录。
2. `app/routers/products.py`：商品和库存接口。
3. `app/routers/orders.py`：订单 HTTP 契约。
4. `app/services/inventory_service.py`：库存规则。
5. `app/services/order_service.py`：事务和幂等。
6. `tests/test_orders.py`：完整业务验证。

### 12.1 为什么金额使用整数分

不要用二进制浮点数表示货币：

```python
price_cents: int = 12900
```

`12900` 表示 129.00 元。这样计算总价不会产生 `0.1 + 0.2` 一类精度问题。复杂财务系统也会使用 Decimal 或专门的 money 类型，但普通订单接口用最小货币单位的整数很清晰。

### 12.2 为什么订单项保存价格快照

商品价格以后可能变化。订单项保存：

```python
unit_price_cents=product.price_cents
```

以后查询旧订单时，仍然能看到成交时的价格，而不是商品当前价格。

### 12.3 创建订单的事务边界

`create_order` 一次事务中完成：

1. 查询幂等键。
2. 锁定相关商品行。
3. 校验商品和库存。
4. 计算总价。
5. 创建订单并 `flush` 获得订单 ID。
6. 创建订单项。
7. 扣减商品库存。
8. 写库存流水。
9. 写幂等记录。
10. 最后只 `commit` 一次。

任何步骤异常都会 `rollback`。这保证不会出现“订单创建成功但库存没扣”或“库存扣了但订单没保存”的半完成状态。

### 12.4 `flush` 与 `commit`

- `flush`：让数据库执行当前 SQL，因此可以获得自动 ID，但事务仍可回滚。
- `commit`：正式提交整个事务。

订单项和库存流水需要订单 ID，因此先 flush 订单，再在最后统一 commit。

### 12.5 幂等为什么必要

Flutter 端可能因为超时、弱网或用户重复点击而重试下单。如果每次请求都创建新订单，会重复扣库存。

客户端发送：

```text
Idempotency-Key: order-request-0001
```

服务端保存 key、请求内容哈希和订单 ID：

- 相同 key + 相同请求：返回第一次创建的订单，不再次扣库存。
- 相同 key + 不同请求：返回 409。
- 新 key：创建新订单。

幂等键应代表一次业务操作，不要对所有订单固定使用同一个值。

### 12.6 行锁与 SQLite 的边界

代码使用 `with_for_update()` 表达“修改库存前锁定行”的意图。SQLite 适合本地学习，但不具备 PostgreSQL 那样的细粒度行锁行为。

生产并发库存场景应该使用 PostgreSQL，并增加并发测试。不要因为单线程手动测试通过，就认为库存一定不会超卖。

### 12.7 实际练习

1. 创建两个商品并设置库存。
2. 调整一次库存，确认产生库存流水。
3. 下单时添加 `Idempotency-Key`。
4. 相同请求重复两次，确认订单 ID 相同、库存只扣一次。
5. 相同 key 改数量，确认返回 409。
6. 创建超过库存的订单，确认库存未变化。
7. 取消订单两次，确认库存只恢复一次。

### 阶段三动手任务

1. 增加 `GET /products/{id}/stock-movements`。
2. 返回流水时支持分页。
3. 增加订单状态 `shipped`。
4. 已发货订单不允许取消，返回 409。
5. 为以上规则补测试。

### 阶段三通过标准

- 能解释一次事务中为什么只在最后 commit。
- 能解释 flush 与 commit 的区别。
- 能解释价格快照和库存流水的作用。
- 能说明幂等键如何阻止重复扣库存。
- `pytest projects/order_inventory_api/tests -q` 通过。

## 13. Flutter 如何调用 FastAPI

### 13.1 Task DTO

FastAPI `TaskRead` 返回：

```json
{
  "title": "学习 FastAPI",
  "description": "完成 CRUD",
  "done": false,
  "id": 1
}
```

Dart 可以映射为：

```dart
class TaskDto {
  const TaskDto({
    required this.id,
    required this.title,
    required this.description,
    required this.done,
  });

  final int id;
  final String title;
  final String? description;
  final bool done;

  factory TaskDto.fromJson(Map<String, dynamic> json) {
    return TaskDto(
      id: json['id'] as int,
      title: json['title'] as String,
      description: json['description'] as String?,
      done: json['done'] as bool,
    );
  }
}
```

真实 Flutter 项目可以继续使用 `json_serializable` 或 Freezed。

### 13.2 使用 Dio 查询任务

```dart
final dio = Dio(
  BaseOptions(baseUrl: 'http://127.0.0.1:8000'),
);

Future<List<TaskDto>> fetchTasks() async {
  final response = await dio.get<List<dynamic>>(
    '/tasks',
    queryParameters: {'offset': 0, 'limit': 20},
  );

  return response.data!
      .map((json) => TaskDto.fromJson(json as Map<String, dynamic>))
      .toList();
}
```

Android 模拟器访问宿主机时通常需要使用 `10.0.2.2`，不要机械照搬 `127.0.0.1`。真机需要使用开发机在局域网中的地址，并确认防火墙和监听地址。

### 13.3 登录并携带 JWT

登录接口使用表单，不是 JSON：

```dart
final response = await dio.post<Map<String, dynamic>>(
  '/auth/token',
  data: {
    'username': username,
    'password': password,
  },
  options: Options(
    contentType: Headers.formUrlEncodedContentType,
  ),
);

final token = response.data!['access_token'] as String;
dio.options.headers['Authorization'] = 'Bearer $token';
```

更完整的项目应使用 Dio interceptor 注入 token，并在 401 时进入刷新 token 或重新登录流程。

### 13.4 统一解析错误

FastAPI 手动抛出的错误通常是：

```json
{"detail": "Task not found"}
```

模型校验错误的 `detail` 是数组，包含字段位置和错误类型。Flutter 端不要假设 `detail` 永远是字符串。

建议按状态码处理：

- 401：清理登录状态或刷新 token。
- 403：显示无权限，不要当成网络失败。
- 404：资源已删除或不可见。
- 409：提示业务冲突，例如库存不足。
- 422：检查客户端请求模型与后端 schema。
- 500：记录 request ID，展示通用错误。

两个实战项目会返回 `X-Request-ID` 响应头，可用于日志定位。

### 13.5 CORS

Flutter iOS、Android 原生应用不受浏览器 CORS 策略限制。Flutter Web 运行在浏览器中，需要 FastAPI 配置 `CORSMiddleware` 并明确允许前端 origin。

不要用允许所有 origin 且同时允许 credentials 的配置直接上生产。

## 14. FastAPI 中什么时候用 async

FastAPI 同时支持同步与异步路由。选择依据是依赖库，而不是接口看起来是否复杂。

使用 `async def` 的典型情况：

- 异步 HTTP 客户端。
- 异步数据库驱动。
- WebSocket。
- 其他需要 `await` 的库。

使用普通 `def` 的典型情况：

- 当前仓库的同步 SQLModel session。
- 普通同步文件或 SDK 操作。
- 没有 await 的简单业务函数。

CPU 密集工作，例如大图处理、视频转码、大型报表，不应直接阻塞请求进程。它们通常需要任务队列、独立 worker 或其他计算服务。

## 15. 从学习项目升级到生产项目

当前代码是企业概念练习，不等于已经满足生产要求。

### 15.1 PostgreSQL

安装驱动：

```bash
python -m pip install "psycopg[binary]"
```

任务项目：

```bash
export DATABASE_URL="postgresql+psycopg://user:password@localhost:5432/fastapi_demo"
```

RBAC 和订单项目分别读取 `RBAC_DATABASE_URL`、`ORDER_DATABASE_URL`。

### 15.2 必须补齐的能力

| 领域 | 学习项目 | 生产项目应补充 |
| --- | --- | --- |
| 表结构 | `create_all` | Alembic migration |
| 配置 | 环境变量 + 简单 Settings | pydantic-settings、环境分层、密钥管理 |
| 数据库 | SQLite | PostgreSQL、连接池、备份 |
| 认证 | access token | refresh、撤销、限流、密钥轮换 |
| 日志 | request ID | 结构化日志、trace、指标、告警 |
| 测试 | SQLite 流程测试 | PostgreSQL 集成测试、并发测试 |
| 部署 | 本地 Uvicorn | Docker、反向代理、HTTPS、CI/CD |
| 后台任务 | 请求内执行 | 队列、worker、重试和死信处理 |

### 15.3 不要过早抽象

入门时不要一开始就加入 repository、unit of work、事件总线和十层目录。

合理顺序是：

1. router 只处理 HTTP。
2. 业务规则变复杂时提取 service。
3. 多处数据访问出现真实重复时再考虑 repository。
4. 需要替换实现或管理事务时再增加更深抽象。

订单项目使用 service 层，是因为它已经包含多表事务和复杂业务规则；简单任务 CRUD 暂时不需要。

## 16. 快速学习安排

按每天约 1.5 到 2 小时安排：

| 天数 | 内容 | 当天产出 |
| --- | --- | --- |
| Day 1 | 第 1 到 4 章 | 读懂 Python 基础并启动 `/docs` |
| Day 2 | 第 5 到 8 章 | 完整执行 Task CRUD |
| Day 3 | 第 9 到 10 章 | 增加 priority、过滤和测试 |
| Day 4 | 第 11 章 | 跑通 JWT、RBAC、资源权限 |
| Day 5 | 第 12 章 | 跑通订单事务、幂等和取消 |
| Day 6 | 第 13 到 14 章 | Flutter 调通 Task 和登录接口 |
| Day 7 | 第 15 章 | 列出自己的生产升级计划 |

时间紧时可以压缩，但不要跳过每阶段的动手任务和测试。只看代码会产生“好像懂了”的错觉。

## 17. 常见问题排查

| 现象 | 优先检查 |
| --- | --- |
| `ModuleNotFoundError` | 是否激活 `.venv`，是否执行安装 |
| Python 版本不满足 | 使用 `python3.12 -m venv .venv` |
| 端口被占用 | 换 `--port 8003` 或停止旧进程 |
| 422 | 打开 `/docs` 对照请求 schema 和字段类型 |
| 401 | token 是否缺失、过期、格式是否为 Bearer |
| 403 | 用户角色或资源归属是否满足 |
| 404 | ID 是否存在，资源是否对当前用户可见 |
| 409 | 查看 `detail`，通常是业务状态冲突 |
| 修改 model 后表没变化 | `create_all` 不负责 migration |
| SQLite locked | 并发边界已超出 SQLite 学习场景 |
| Flutter 连不上本机 | 检查模拟器宿主机地址、监听地址和防火墙 |

## 18. 最终掌握检查表

当你能完成下面内容，才算达到“能利用 FastAPI 完成后台接口项目”的目标：

- [ ] 不看教程创建一个 FastAPI router。
- [ ] 正确声明 path、query、body 和 header 参数。
- [ ] 为创建、读取、更新设计不同 schema。
- [ ] 连接数据库并用请求级 session 完成 CRUD。
- [ ] 对 404、409、422 等情况返回合理状态码。
- [ ] 使用 `Depends` 注入数据库和当前用户。
- [ ] 用 JWT 与密码哈希实现登录。
- [ ] 同时实现角色权限和资源级权限。
- [ ] 把多表业务放进一次事务，并处理 rollback。
- [ ] 为重试敏感接口设计幂等键。
- [ ] 使用 TestClient 和独立数据库写流程测试。
- [ ] 从 Flutter 发请求、解析响应并按状态码处理错误。
- [ ] 能说明 SQLite、`create_all` 和开发密钥为什么不能直接用于生产。

最终验证命令：

```bash
python -m pytest -q
```

本仓库当前测试覆盖三个核心流程：

1. Task CRUD。
2. RBAC 登录、角色和项目成员可见性。
3. 订单幂等、库存不足回滚和取消恢复。

## 19. 官方资料阅读顺序

不要从高级部署章节随机跳读。按下面顺序补充：

1. [Python 类型提示与 FastAPI](https://fastapi.tiangolo.com/python-types/)
2. [FastAPI First Steps](https://fastapi.tiangolo.com/tutorial/first-steps/)
3. [Request Body](https://fastapi.tiangolo.com/tutorial/body/)
4. [Dependencies](https://fastapi.tiangolo.com/tutorial/dependencies/)
5. [SQL Databases](https://fastapi.tiangolo.com/tutorial/sql-databases/)
6. [Bigger Applications](https://fastapi.tiangolo.com/tutorial/bigger-applications/)
7. [Testing](https://fastapi.tiangolo.com/tutorial/testing/)
8. [OAuth2、密码哈希与 JWT](https://fastapi.tiangolo.com/tutorial/security/oauth2-jwt/)
9. [Async 与并发](https://fastapi.tiangolo.com/async/)
10. [Python 官方教程](https://docs.python.org/3/tutorial/)

阅读官方文档时，把示例映射回本仓库的真实文件。能指出一个概念在项目中的位置，比单独记住 API 名称更重要。
