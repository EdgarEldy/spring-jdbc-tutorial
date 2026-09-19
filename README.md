# spring-jdbc-tutorial

A complete tutorial for building a REST API with classic **Spring Framework** (no Spring Boot) and **Spring JDBC** (`JdbcTemplate`), organized as a Maven multi-module project that strictly separates business logic from the web layer: everything business-related lives in `core`, everything HTTP-related lives in `ws`. Covers a full identity/RBAC domain (users, roles, permissions, tokens) and an e-commerce domain (categories, products, customers, orders) as two independent data models.

This document is the **complete specification** of the project: it is meant to be followed step by step to implement each branch.

## Table of contents

- [Why no Spring Boot, and why no stereotype annotations](#why-no-spring-boot-and-why-no-stereotype-annotations)
- [core vs. ws: what belongs where](#core-vs-ws-what-belongs-where)
- [Entity, DTO, Mapper, DAO, Service: five layers, five responsibilities](#entity-dto-mapper-dao-service-five-layers-five-responsibilities)
- [Tech stack](#tech-stack)
- [Data model](#data-model)
- [Module structure](#module-structure)
- [Branching strategy](#branching-strategy)
- [Project structure](#project-structure)
- [Standard response format](#standard-response-format)
- [Testing strategy](#testing-strategy)
  - [Test naming convention](#test-naming-convention)
- [feature/core-architecture](#featurecore-architecture)
- [feature/auth](#featureauth)
- [feature/rbac](#featurerbac)
- [feature/catalog](#featurecatalog)
- [feature/customer](#featurecustomer)
- [feature/order](#featureorder)
- [Order of work](#order-of-work)
- [Code conventions](#code-conventions)
- [Concepts covered](#concepts-covered)
- [How to follow this tutorial](#how-to-follow-this-tutorial)

## Why no Spring Boot, and why no stereotype annotations

This project uses **Spring Framework directly**, wired through explicit Java `@Configuration` classes rather than Spring Boot's auto-configuration - every bean this project needs is declared by hand, in a `@Bean` method, in a config class that belongs to the same module as the thing it wires. There is no `@SpringBootApplication`, no starter, no classpath-driven auto-configuration guessing what should exist.

The same philosophy extends to the business layer: **no `@Repository`, no `@Service`, no `@Component`** on a DAO or service implementation class. Every DAO and service bean is registered explicitly in that module's own `DaoConfig`/`ServiceConfig`, by its interface type. This is a deliberate constraint, not an oversight - it means a bean's existence and its wiring are always visible by reading one config class, never inferred from an annotation plus a component-scan base package that might or might not cover it.

## core vs. ws: what belongs where

- **`core`** owns every business concern: entities, data access (DAOs), business rules (services), and the configuration wiring both together. `core` has **no dependency on anything web-related** - no Spring MVC, no Servlet API, no JSON serialization concern. A `core` module could be reused behind a completely different front door (a CLI, a batch job, a different web framework) without modification.
- **`ws`** ("web service") owns every HTTP concern: controllers, request/response payloads, security, and the application's single entry point. `ws` depends on every `core/*` module, but no `core/*` module ever depends on `ws`.
- This is not multi-module for the sake of enforcing it via the compiler alone (though it does that too) - it's a statement about where business logic is allowed to live, matching the same discipline other projects enforce through a test instead of a module boundary.

## Entity, DTO, Mapper, DAO, Service: five layers, five responsibilities

Without an ORM, five concerns that JPA would normally blur together into two (an `@Entity` and a repository) are kept explicitly separate here:

- **Entity** (`entity/`): the raw, table-shaped representation of a row - one class per table, annotated `@jakarta.persistence.Table(name = "...")` (and `@Id`/`@Column` on its fields). **These annotations are documentation only** - this project has no JPA provider, no `EntityManager`, nothing reads them at runtime. They exist purely so the mapping between a class and its table/columns is declared in one visible place, next to the class itself, instead of being implicit in whatever SQL a `RowMapper` happens to write. A `RowMapper` still does all the actual work of turning a `ResultSet` into this object - the annotations don't do it.
- **DTO** (`dto/`): the business-facing representation a `Service` returns and accepts. It usually mirrors its entity closely, but it's a genuinely separate class (suffixed `Dto` to keep the two importable side by side without a naming clash) - a `Service` method never returns an `entity/` object directly, even when nothing has changed between the two shapes yet.
- **Mapper** (`mapper/`): now covers two distinct, equally single-purpose jobs, both living in the same package but never the same class:
  - a `RowMapper<Entity>` implementation, converting one `ResultSet` row into one `entity/` object - used only by `dao/impl/`
  - an `EntityMapper`, converting `entity/` ↔ `dto/` in both directions - used only by `service/impl/`
- **DAO** (`dao/` + `dao/impl/`): owns SQL and `JdbcTemplate` calls, and works exclusively in `entity/` types - a DAO method never sees or returns a `dto/` object.
- **Service** (`service/` + `service/impl/`): owns business rules and transaction boundaries, and works exclusively in `dto/` types at its own boundary - it calls a DAO (getting `entity/` back), converts via the `EntityMapper`, and only ever hands a `dto/` object to whatever called it.

A DAO never calls a service. A service never touches `JdbcTemplate` directly. A controller (in `ws`) never touches a DAO or an `entity/` class - only a service, and only `dto/` types.

## Tech stack

| Component | Choice |
|---|---|
| Framework | Spring Framework 6.2.x (classic, no Spring Boot) |
| Language | Java 17 (LTS) |
| Build | Maven, multi-module (`core` and `ws` as top-level modules, `core` itself aggregating `common`/`auth`/`catalog`/`customer`/`order`) |
| Data access | Spring JDBC (`JdbcTemplate`), hand-written `RowMapper<T>` implementations - no JPA provider, no Hibernate, no Spring Data, no `EntityManager` |
| Entity annotations | `jakarta.persistence-api` only (the annotations `@Table`/`@Id`/`@Column`) - used purely as documentation on `entity/` classes, never processed by anything at runtime |
| Database | PostgreSQL 16 (via Docker Compose) |
| Migrations | Flyway |
| Web layer | Spring MVC, packaging `war` |
| Application server | Tomcat 10.1.x (Jakarta Servlet 6.0, matching Spring Framework 6.x's `jakarta.*` namespace) |
| Security | Spring Security 6.x (explicit `SecurityFilterChain` bean, no auto-configuration), JWT via `io.jsonwebtoken:jjwt` |
| Permission checks | `@PreAuthorize("hasPermission(...)")` backed by a custom `PermissionEvaluator` |
| Validation | Jakarta Bean Validation (Hibernate Validator as the implementation, used standalone - not JPA's) |
| Tests | JUnit 5, Mockito, Spring Test (`MockMvc`), Testcontainers (PostgreSQL, for DAO tests) |
| CI/CD | GitHub Actions |
| Containerization | Docker, docker-compose |

## Data model

Two independent domains, no cross-domain foreign key.

```
users (id, first_name, last_name, email, password, enabled, account_locked)
    │ N──N (via role_user)
roles (id, role_name)
    │ N──N (via role_permission)
permissions (id, resource, action)

activation_tokens (id, user_id, token, created_at, expires_at, validated_at)
blacklisted_tokens (id, user_id, token, jti, blacklisted_at, created_at, expires_at, validated_at)
password_reset_tokens (id, user_id, token, type, expiry_date)
audit_logs (id, actor_user_id, action, entity_type, entity_id, details, created_at)

categories (id, category_name)
    │ 1
    │
    │ N
products (id, category_id, product_name, unit_price)
    │ 1
    │
    │ N
orders (id, customer_id, product_id, quantity, total)
    │ N
    │
    │ 1
customers (id, first_name, last_name, telephone, email, address)
```

## Module structure

```
                         ┌────────┐
                         │   ws   │   (Spring MVC, security, HTTP payloads)
                         └───┬────┘
             ┌───────────────┼───────────────┬──────────────┐
             │                │               │              │
        ┌────▼────┐    ┌──────▼─────┐  ┌──────▼─────┐  ┌─────▼────┐
        │   auth   │    │  catalog   │  │  customer  │  │  order   │
        └────┬─────┘    └──────┬─────┘  └──────┬─────┘  └────┬─────┘
             │                │               │              │
             │           order also depends on catalog and customer
             │                │               │              │
             └────────────────┴───────────────┴──────────────┘
                              │
                         ┌────▼────┐
                         │ common  │   (DataSourceConfig, AbstractDao, shared exceptions)
                         └─────────┘
```

`order` is the only `core` module that depends on another business module (`catalog`, `customer`) - and only on their **service** interfaces, never their DAOs, to validate a product/customer exist and read the product's price.

## Branching strategy

| Branch | Role |
|---|---|
| `master` | Stable, production-ready code. No direct commits, only merges from `develop`. |
| `develop` | Integration branch. |
| `feature/core-architecture` | Parent/`core`/`ws` POM structure, `common` module, `ws` skeleton (servlet wiring, exception handling, Docker, CI). |
| `feature/auth` | `core/auth` module: `User`, tokens, registration/activation/login/logout/password reset. |
| `feature/rbac` | `Role`/`Permission` inside `core/auth`, `PermissionEvaluator`, full CRUD, audit logging. |
| `feature/catalog` | `core/catalog` module: `Category`/`Product`. |
| `feature/customer` | `core/customer` module: `Customer`. |
| `feature/order` | `core/order` module: `Order`, depends on `catalog`/`customer` services. |

## Project structure

```
spring-jdbc-tutorial/
├── pom.xml                                        (parent aggregator, packaging=pom, modules: core, ws)
├── core/
│   ├── pom.xml                                    (aggregator, packaging=pom, modules: common, auth, catalog, customer, order)
│   ├── common/
│   │   ├── pom.xml
│   │   └── src/main/java/com/edgareldy/springjdbctutorial/core/common/
│   │       ├── exception/
│   │       │   ├── ResourceNotFoundException.java
│   │       │   └── BusinessRuleException.java
│   │       ├── dao/support/
│   │       │   └── AbstractDao.java               (shared JdbcTemplate access, common query helpers)
│   │       └── config/
│   │           └── DataSourceConfig.java          (DataSource, JdbcTemplate, PlatformTransactionManager - the only place these are declared)
│   ├── auth/
│   │   ├── pom.xml                                (depends on common)
│   │   └── src/main/java/.../core/auth/
│   │       ├── entity/
│   │       │   ├── User.java, Role.java, Permission.java                 (@Table("users"), @Table("roles"), @Table("permissions"))
│   │       │   ├── ActivationToken.java, BlacklistedToken.java, PasswordResetToken.java
│   │       │   └── AuditLog.java
│   │       ├── dto/
│   │       │   ├── UserDto.java, RoleDto.java, PermissionDto.java
│   │       │   ├── ActivationTokenDto.java, BlacklistedTokenDto.java, PasswordResetTokenDto.java
│   │       │   └── AuditLogDto.java
│   │       ├── mapper/
│   │       │   ├── UserRowMapper.java, RoleRowMapper.java, PermissionRowMapper.java        (ResultSet → entity)
│   │       │   ├── ActivationTokenRowMapper.java, BlacklistedTokenRowMapper.java, PasswordResetTokenRowMapper.java, AuditLogRowMapper.java
│   │       │   └── UserMapper.java, RoleMapper.java, PermissionMapper.java, AuditLogMapper.java   (entity ↔ dto)
│   │       ├── dao/
│   │       │   ├── UserDao.java, RoleDao.java, PermissionDao.java, AuditLogDao.java  (interfaces, work in entity/ types)
│   │       │   └── impl/
│   │       │       └── UserDaoImpl.java, RoleDaoImpl.java, PermissionDaoImpl.java, AuditLogDaoImpl.java   (JdbcTemplate, unannotated)
│   │       ├── service/
│   │       │   ├── AuthService.java, RbacService.java, AuditLogger.java   (interfaces, work in dto/ types)
│   │       │   └── impl/
│   │       │       └── AuthServiceImpl.java, RbacServiceImpl.java, AuditLoggerImpl.java   (unannotated)
│   │       └── config/
│   │           ├── DaoConfig.java                 (@Bean UserDao, RoleDao, PermissionDao, AuditLogDao)
│   │           └── ServiceConfig.java             (@Bean AuthService, RbacService, AuditLogger)
│   ├── catalog/
│   │   ├── pom.xml                                (depends on common)
│   │   └── src/main/java/.../core/catalog/
│   │       ├── entity/ (Category.java @Table("categories"), Product.java @Table("products"))
│   │       ├── dto/ (CategoryDto.java, ProductDto.java)
│   │       ├── mapper/ (CategoryRowMapper.java, ProductRowMapper.java, CategoryMapper.java, ProductMapper.java)
│   │       ├── dao/ (CategoryDao.java, ProductDao.java + impl/)
│   │       ├── service/ (CategoryService.java, ProductService.java + impl/)
│   │       └── config/ (DaoConfig.java, ServiceConfig.java)
│   ├── customer/
│   │   ├── pom.xml                                (depends on common)
│   │   └── src/main/java/.../core/customer/
│   │       ├── entity/ (Customer.java @Table("customers"))
│   │       ├── dto/ (CustomerDto.java)
│   │       ├── mapper/ (CustomerRowMapper.java, CustomerMapper.java)
│   │       ├── dao/ (CustomerDao.java + impl/)
│   │       ├── service/ (CustomerService.java + impl/)
│   │       └── config/ (DaoConfig.java, ServiceConfig.java)
│   └── order/
│       ├── pom.xml                                (depends on catalog, customer)
│       └── src/main/java/.../core/order/
│           ├── entity/ (Order.java @Table("orders"))
│           ├── dto/ (OrderDto.java)
│           ├── mapper/ (OrderRowMapper.java, OrderMapper.java)
│           ├── dao/ (OrderDao.java + impl/)
│           ├── service/ (OrderService.java + impl/, depends on CategoryService/ProductService/CustomerService)
│           └── config/ (DaoConfig.java, ServiceConfig.java)
└── ws/
    ├── pom.xml                                    (depends on core/auth, core/catalog, core/customer, core/order)
    └── src/main/
        ├── java/com/edgareldy/springjdbctutorial/ws/
        │   ├── controller/
        │   │   ├── AuthController.java, UserController.java, RoleController.java, PermissionController.java
        │   │   └── CategoryController.java, ProductController.java, CustomerController.java, OrderController.java
        │   ├── payload/
        │   │   ├── common/ (ApiResponse.java, PageResponse.java)
        │   │   └── auth/, rbac/, catalog/, customer/, order/     (HTTP request/response classes - "payload", not "dto",
        │   │                                                       to avoid colliding with core's own dto/ naming)
        │   ├── converter/                          (core dto/ ↔ ws payload/ - deliberately not called "mapper",
        │   │                                         since that name is already used for RowMapper in core)
        │   │   ├── UserConverter.java, RoleConverter.java, PermissionConverter.java
        │   │   └── CategoryConverter.java, ProductConverter.java, CustomerConverter.java, OrderConverter.java
        │   ├── security/
        │   │   ├── JwtService.java
        │   │   ├── JwtAuthFilter.java
        │   │   └── CustomPermissionEvaluator.java   (PermissionEvaluator, backs @PreAuthorize("hasPermission(...)"))
        │   ├── exception/
        │   │   └── GlobalExceptionHandler.java       (@RestControllerAdvice - the one stereotype-adjacent annotation
        │   │                                           in this project, since Spring MVC requires it for this mechanism)
        │   └── config/
        │       ├── WebAppInitializer.java            (WebApplicationInitializer, replaces web.xml)
        │       ├── WebMvcConfig.java                 (@EnableWebMvc, imports every core module's DaoConfig/ServiceConfig, scans only ws.controller and ws.exception)
        │       ├── SecurityConfig.java                (SecurityFilterChain, method security enabling @PreAuthorize)
        │       └── OpenApiConfig.java                (not delivered: springdoc-openapi relies on Spring Boot classes)
        └── webapp/
            └── (empty - no JSPs, API-only)
├── docker-compose.yml
├── Dockerfile
├── .github/workflows/ci.yml
└── README.md
```

## Standard response format

Every response is wrapped in a generic `ApiResponse<T>`, defined once in `ws/payload/common/`.

```java
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Instant timestamp;

    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.message = message;
        response.data = data;
        response.timestamp = Instant.now();
        return response;
    }

    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.message = message;
        response.timestamp = Instant.now();
        return response;
    }

    // explicit getters/setters below - no Lombok, no record (see Code conventions)
}
```

`GlobalExceptionHandler` maps `ResourceNotFoundException` (from any `core` module) to 404, Bean Validation failures to 400, `BusinessRuleException` to 422, and anything unmapped to 500, always through `ApiResponse<Void>`.

## Testing strategy

Every branch from `feature/auth` onward ships tests at all five layers before its Pull Request is opened - a branch is not complete with only service tests, or only controller tests.

| Layer | Tool | What it verifies | Data |
|---|---|---|---|
| DAO | JUnit 5 + Spring Test + Testcontainers (real PostgreSQL) | Real SQL against a real schema: inserts, updates, deletes, derived queries, all in `entity/` types | Each DAO test class loads **its own dedicated SQL fixture file** (e.g. `user-dao-dataset.sql`, `category-dao-dataset.sql`), executed before each test - never a shared dataset reused across DAOs, so one DAO's test data can never accidentally satisfy or break another's assumptions |
| RowMapper | JUnit 5 + Mockito | A `RowMapper.mapRow(...)` implementation, given a mocked `java.sql.ResultSet` (`Mockito.mock(ResultSet.class)`, stubbed column by column), producing the expected `entity/` object | No database at all - pure, fast unit tests |
| EntityMapper | JUnit 5 | An entity ↔ dto conversion, given a hand-built object on either side | No database, no mocks needed - the simplest tests in the project |
| Service | JUnit 5 + Mockito | Business rules and orchestration, with every DAO (and any other module's service) dependency mocked, asserting on the `dto/` objects returned | No database - constructed test data only |
| Controller | JUnit 5 + Spring Test (`MockMvc`) | HTTP-level behavior: status codes, payload shape, security/permission enforcement, with the service layer mocked | No database - `MockMvc` against a standalone or `WebApplicationContext`-backed setup |

### Test naming convention

Every test method, at every layer, is named `_NN_Should<Outcome>_When<Condition>` - a two-digit, zero-padded sequence number (execution order within the class, not a requirement enforced by JUnit but kept consistent by convention), followed by what's expected, followed by the condition that produces it.

```java
@Test
void _01_ShouldReturnCategory_WhenCategoryExists() { ... }

@Test
void _02_ShouldReturnEmpty_WhenCategoryDoesNotExist() { ... }
```

No other naming style (`shouldX()`, `testX()`, `givenX_whenY_thenZ()`) is used anywhere in this project's test suite.

## feature/core-architecture

### Tasks

- [x] Parent `pom.xml` (packaging `pom`, modules `core`/`ws`, `dependencyManagement` for Spring Framework 6.2.x, Testcontainers, etc.)
- [x] `core/pom.xml` (packaging `pom`, modules `common`/`auth`/`catalog`/`customer`/`order`)
- [x] `core/common`: `ResourceNotFoundException`, `BusinessRuleException`, `AbstractDao` (holds a `JdbcTemplate` reference, offers a couple of shared helpers), `DataSourceConfig` (`DataSource` via HikariCP, `JdbcTemplate`, `DataSourceTransactionManager`)
- [x] Flyway script `V1__init_schema.sql` (all tables from both domains, including `audit_logs`)
- [x] `ws` skeleton: `WebAppInitializer` (`WebApplicationInitializer`), `WebMvcConfig` (`@EnableWebMvc`, no module config imported yet), `ApiResponse<T>`, `PageResponse<T>`, `GlobalExceptionHandler`
- [x] `docker-compose.yml` (`ws` deployed on Tomcat + PostgreSQL), `Dockerfile` (multi-stage: Maven build, Tomcat 10.1 runtime)
- [x] `.github/workflows/ci.yml`: `mvn verify` across the whole reactor

## feature/auth

### Endpoints

| Method | URL | Description |
|---|---|---|
| POST | `/api/v1/auth/register` | Register (creates a disabled user + activation token) |
| GET | `/api/v1/auth/activate-account` | Activates a user account |
| POST | `/api/v1/auth/login` | Returns a JWT |
| POST | `/api/v1/auth/logout` | Blacklists the current JWT |
| GET | `/api/v1/auth/me` | Current user profile |
| POST | `/api/v1/auth/forgot-password` | Generates a password-reset token |
| POST | `/api/v1/auth/reset-password` | Consumes the token, updates the password |

### Tasks

- [ ] `core/auth`: `User`, `ActivationToken`, `BlacklistedToken`, `PasswordResetToken` **entity** classes (`entity/`, `@Table("users")`/`@Table("activation_tokens")`/etc., `@Id`/`@Column` on fields, plain Java otherwise) and their matching `UserDto`, `ActivationTokenDto`, `BlacklistedTokenDto`, `PasswordResetTokenDto` classes (`dto/`)
- [ ] `UserRowMapper`, `ActivationTokenRowMapper`, `BlacklistedTokenRowMapper`, `PasswordResetTokenRowMapper` (`ResultSet` → entity), and `UserMapper` (entity ↔ dto, the only one of these four actually needed yet - the token entities/dtos are used directly by `AuthServiceImpl` without a full bidirectional mapper, since nothing outside the service ever needs a token dto's shape beyond what `AuthController` reads directly off `AuthService`'s own return types)
- [ ] `UserDao` + `UserDaoImpl` (and the token DAOs), all `JdbcTemplate`-based, all unannotated, all working in `entity/` types
- [ ] `AuthService` (interface) + `AuthServiceImpl`: registration, activation, login (password hashing/verification via `org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder`, used standalone), logout, forgot/reset password - every public method takes/returns `dto/` types, converting to/from `entity/` via `UserMapper` around each `UserDao` call
- [ ] `forgotPassword` returns the exact same outcome - same behavior, roughly the same timing - whether or not the submitted email matches an existing account, so the endpoint can't be used to enumerate registered emails
- [ ] `core/auth`'s `DaoConfig`/`ServiceConfig`
- [ ] `ws`: `JwtService` (issuance/validation via `jjwt`, unique `jti` per token), `JwtAuthFilter`, `AuthController`, `UserConverter` (`core.auth.dto.UserDto` ↔ `ws.payload.auth.*`)
- [ ] `WebMvcConfig` updated to import `core/auth`'s `DaoConfig`/`ServiceConfig`
- [ ] Tests at all five layers (see [Testing strategy](#testing-strategy)): `UserDao`/token DAOs against Testcontainers with their own fixture files, each `RowMapper` against a mocked `ResultSet`, `UserMapper` against a hand-built entity/dto pair (pure unit test, no mocks needed), `AuthServiceImpl` with `UserDao` mocked, `AuthController` via `MockMvc` with `AuthService` mocked - plus one full register → activate → login → `/me` flow

## feature/rbac

Full CRUD for users, roles, and permissions, still inside `core/auth`. Assignments always flow in one direction: **permissions are assigned onto a role**, never the reverse; **roles are assigned onto a user**, never the reverse.

### Endpoints

| Method | URL | Description | Access |
|---|---|---|---|
| GET | `/api/v1/users` | Paginated list | `@PreAuthorize("hasPermission('USER','READ')")` |
| GET | `/api/v1/users/{id}` | Detail, including roles | `hasPermission('USER','READ')` |
| PATCH | `/api/v1/users/{id}/roles/{roleId}` | Assign a role to a user | `hasPermission('USER','WRITE')` |
| DELETE | `/api/v1/users/{id}/roles/{roleId}` | Remove a role from a user | `hasPermission('USER','WRITE')` |
| GET | `/api/v1/roles` | List, including permissions | `hasPermission('ROLE','READ')` |
| POST | `/api/v1/roles` | Create | `hasPermission('ROLE','WRITE')` |
| PUT | `/api/v1/roles/{id}` | Update (name only) | `hasPermission('ROLE','WRITE')` |
| DELETE | `/api/v1/roles/{id}` | Delete | `hasPermission('ROLE','WRITE')` |
| POST | `/api/v1/roles/{id}/permissions/{permissionId}` | Assign a permission to a role | `hasPermission('ROLE','WRITE')` |
| DELETE | `/api/v1/roles/{id}/permissions/{permissionId}` | Remove a permission from a role | `hasPermission('ROLE','WRITE')` |
| GET | `/api/v1/permissions` | List | `hasPermission('PERMISSION','READ')` |
| POST | `/api/v1/permissions` | Create | `hasPermission('PERMISSION','WRITE')` |
| PUT | `/api/v1/permissions/{id}` | Update | `hasPermission('PERMISSION','WRITE')` |
| DELETE | `/api/v1/permissions/{id}` | Delete | `hasPermission('PERMISSION','WRITE')` |

### Tasks

- [ ] `Role`, `Permission`, `AuditLog` entity classes (`entity/`, `@Table("roles")`/`@Table("permissions")`/`@Table("audit_logs")`) and `RoleDto`/`PermissionDto`/`AuditLogDto` (`dto/`), `RoleRowMapper`/`PermissionRowMapper`/`AuditLogRowMapper` (entity, from `ResultSet`) plus `RoleMapper`/`PermissionMapper`/`AuditLogMapper` (entity ↔ dto), `RoleDao`/`PermissionDao`/`AuditLogDao` + impl (working in entity/ types)
- [ ] `RbacService` (interface) + `RbacServiceImpl`:
  - `createRole`/`updateRole`/`deleteRole` - `deleteRole` rejects if any user is still assigned this role
  - `createPermission`/`updatePermission`/`deletePermission` - rejects if any role still has this permission
  - `assignPermissionToRole`/`removePermissionFromRole` - rejects removing `ROLE:WRITE` from a role if it would leave zero users anywhere holding a role that grants it
  - `assignRoleToUser`/`removeRoleFromUser` - the same last-admin check applied at the point of removal from a specific user
- [ ] `AuditLogger`/`AuditLoggerImpl`: a single `log(String action, String entityType, Long entityId, String details)` method, called from every `RbacServiceImpl` mutation
- [ ] `CustomPermissionEvaluator` (`ws`, implements Spring Security's `PermissionEvaluator`): resolves the authenticated user's permissions (loaded once at login and embedded in the JWT, read from the `Authentication` object - no database call per request) and answers `hasPermission(target, permission)` calls from `@PreAuthorize`
- [ ] `SecurityConfig` updated: `@EnableMethodSecurity`, `CustomPermissionEvaluator` registered on the `MethodSecurityExpressionHandler`
- [ ] A seeding step (a Flyway data-migration): baseline permissions covering every resource/action this project defines, assigned to a seeded `ADMIN` role - without this, nobody could ever be granted `ROLE:WRITE`/`PERMISSION:WRITE` to create the first assignment
- [ ] `RoleController`, `PermissionController`, `UserController` role-assignment endpoints, `RoleConverter`/`PermissionConverter`
- [ ] Tests at all five layers, including the "still referenced" rejection on `deleteRole`/`deletePermission`, the last-admin rejection triggered both ways, `CustomPermissionEvaluator` allowing/denying correctly, and an assertion that every `RbacServiceImpl` mutation produces a matching `AuditLog` row

## feature/catalog

### Endpoints

| Method | URL | Description | Access |
|---|---|---|---|
| GET | `/api/v1/categories` | Paginated list | `hasPermission('CATEGORY','READ')` |
| GET | `/api/v1/categories/{id}` | Detail | `hasPermission('CATEGORY','READ')` |
| POST | `/api/v1/categories` | Create | `hasPermission('CATEGORY','WRITE')` |
| PUT | `/api/v1/categories/{id}` | Update | `hasPermission('CATEGORY','WRITE')` |
| DELETE | `/api/v1/categories/{id}` | Delete | `hasPermission('CATEGORY','WRITE')` |
| GET | `/api/v1/products` | Paginated list, filterable by `categoryId` | `hasPermission('PRODUCT','READ')` |
| GET | `/api/v1/products/{id}` | Detail | `hasPermission('PRODUCT','READ')` |
| POST | `/api/v1/products` | Create | `hasPermission('PRODUCT','WRITE')` |
| PUT | `/api/v1/products/{id}` | Update | `hasPermission('PRODUCT','WRITE')` |
| DELETE | `/api/v1/products/{id}` | Delete | `hasPermission('PRODUCT','WRITE')` |

### Tasks

- [ ] `core/catalog`: `Category`, `Product` entity classes (`@Table("categories")`/`@Table("products")`) and `CategoryDto`/`ProductDto`, `CategoryRowMapper`/`ProductRowMapper` (entity) plus `CategoryMapper`/`ProductMapper` (entity ↔ dto), `CategoryDao`/`ProductDao` + impl, `CategoryService`/`ProductService` + impl, `DaoConfig`/`ServiceConfig`
- [ ] Business rule: deleting a category that still has products is rejected (`BusinessRuleException` → 422)
- [ ] `ws`: `CategoryController`, `ProductController`, `CategoryConverter`, `ProductConverter`
- [ ] `WebMvcConfig` updated to import `core/catalog`'s config
- [ ] Tests at all five layers - `CategoryDao`/`ProductDao` each with their own fixture dataset, the category-deletion rejection case, a permission-denied case

## feature/customer

### Endpoints

| Method | URL | Description | Access |
|---|---|---|---|
| GET | `/api/v1/customers` | Paginated list | `hasPermission('CUSTOMER','READ')` |
| GET | `/api/v1/customers/{id}` | Detail | `hasPermission('CUSTOMER','READ')` |
| POST | `/api/v1/customers` | Create | `hasPermission('CUSTOMER','WRITE')` |
| PUT | `/api/v1/customers/{id}` | Update | `hasPermission('CUSTOMER','WRITE')` |
| DELETE | `/api/v1/customers/{id}` | Delete | `hasPermission('CUSTOMER','WRITE')` |

### Tasks

- [ ] `core/customer`: `Customer` entity (`@Table("customers")`) and `CustomerDto`, `CustomerRowMapper` (entity) plus `CustomerMapper` (entity ↔ dto), `CustomerDao` + impl, `CustomerService` + impl, `DaoConfig`/`ServiceConfig`
- [ ] `ws`: `CustomerController`, `CustomerConverter`
- [ ] `WebMvcConfig` updated to import `core/customer`'s config
- [ ] Tests at all five layers, `CustomerDao` with its own fixture dataset

## feature/order

### Endpoints

| Method | URL | Description | Access |
|---|---|---|---|
| GET | `/api/v1/orders` | Paginated list, filterable by `customerId`/`productId` | `hasPermission('ORDER','READ')` |
| GET | `/api/v1/orders/{id}` | Detail | `hasPermission('ORDER','READ')` |
| POST | `/api/v1/orders` | Create (computes `total`) | `hasPermission('ORDER','WRITE')` |

### Tasks

- [ ] `core/order`: `Order` entity (`@Table("orders")`) and `OrderDto`, `OrderRowMapper` (entity) plus `OrderMapper` (entity ↔ dto), `OrderDao` + impl, `OrderService` + impl (depends on `catalog`'s `ProductService` and `customer`'s `CustomerService` - their service interfaces, never their DAOs directly): computes `total = quantity * product.unitPrice`, checks the referenced customer/product exist
- [ ] `core/order`'s `DaoConfig`/`ServiceConfig` (the latter injecting `ProductService`/`CustomerService` beans from the other modules' contexts)
- [ ] `ws`: `OrderController`, `OrderConverter`
- [ ] `WebMvcConfig` updated to import `core/order`'s config
- [ ] Tests at all five layers, `OrderDao` with its own fixture dataset, the total computation, and the not-found cases for a bad `customerId`/`productId`

## Order of work

1. `feature/core-architecture` → Pull Request to `develop`
2. `feature/auth` (depends on `core-architecture`) → Pull Request to `develop`
3. `feature/rbac` (depends on `auth`) → Pull Request to `develop`
4. `feature/catalog` (depends on `core-architecture`) → Pull Request to `develop`
5. `feature/customer` (depends on `core-architecture`) → Pull Request to `develop`
6. `feature/order` (depends on `catalog`, `customer`) → Pull Request to `develop`
7. `develop` → `master`

## Code conventions

- Root package: `com.edgareldy.springjdbctutorial`, sub-rooted `core.<module>` and `ws`
- **No Lombok, no Java `record`, anywhere in this project** - every entity, dto, and payload class is a plain Java class with explicit fields, an explicit constructor, explicit getters/setters, and explicit `equals`/`hashCode` where needed. This is a deliberate departure from other tutorials that default to records for DTOs.
- **No `@Repository`, `@Service`, or `@Component`** on a DAO or service implementation - every bean is declared in that module's `DaoConfig`/`ServiceConfig` via an explicit `@Bean` method
- `entity/` classes carry `@Table`/`@Id`/`@Column` as documentation only - no JPA provider ever reads them, and their presence never implies any actual persistence behavior beyond what `dao/impl/` explicitly writes in SQL
- A DAO method never returns a `ResultSet`, never takes SQL as a parameter from outside `dao/impl/`, and never sees a `dto/` type - only `entity/`
- A `RowMapper` never queries the database and never contains business logic - it only converts one `ResultSet` row into one `entity/` object
- An `EntityMapper` never queries the database and never contains business logic - it only converts between `entity/` and `dto/`, in both directions
- A service never calls `JdbcTemplate` directly, never calls another module's DAO (only another module's service, injected by interface type), and never accepts or returns an `entity/` type at its own public boundary - only `dto/`
- `core`'s `dto/` and `ws`'s `payload/` are named differently on purpose, to keep "the business object" and "the HTTP request/response shape" visually distinct even where their fields overlap; `entity/` and `dto/` classes are distinguished by the `Dto` suffix so both can be imported in the same `EntityMapper` without ambiguity
- Every RBAC mutation calls `AuditLogger`
- Every DAO test class loads its own dedicated SQL fixture file, never a dataset shared with another DAO's tests

## Concepts covered

- Multi-module Maven with a strict business/web separation enforced by module boundaries, not just convention
- Explicit Spring configuration (`@Configuration`/`@Bean`) as an alternative to component scanning and stereotype annotations
- Spring JDBC (`JdbcTemplate`) as an ORM-free data access layer
- The Entity / DTO / Mapper / DAO / Service five-way split, and why an ORM normally hides this distinction
- Hand-written `RowMapper<T>` implementations, tested independently of any database
- Spring Security configured explicitly (`SecurityFilterChain`, method security), JWT issued and validated by hand
- Fine-grained permission checks via a custom `PermissionEvaluator` backing `@PreAuthorize("hasPermission(...)")`
- Directional relationship management (permissions onto roles, roles onto users) as a deliberate convention
- Audit logging as a first-class concern for every sensitive RBAC mutation
- Five-layer testing (DAO, RowMapper, EntityMapper, service, controller), each with the tool and data isolation appropriate to it
- Using `jakarta.persistence` annotations purely as documentation, with no JPA provider ever processing them
- Classic WAR packaging and deployment on an external servlet container, without Spring Boot
- Containerization (Docker, docker-compose)
- Continuous integration (GitHub Actions)

## How to follow this tutorial

1. Clone the repository and check out `develop`
2. Follow the branches in order: `feature/core-architecture` → `feature/auth` → `feature/rbac` → `feature/catalog` → `feature/customer` → `feature/order`
3. Run `docker-compose up`, then deploy the WAR (`mvn package` at the root, then deploy `ws/target/ws.war` to Tomcat 10.1, or configure `mvn tomcat10:deploy`)
4. Access the API at `http://localhost:8080/spring-jdbc-tutorial/api/v1/...`
