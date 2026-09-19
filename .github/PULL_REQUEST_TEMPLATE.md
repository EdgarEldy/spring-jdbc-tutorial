## Branch

<!-- e.g. feature/core-architecture -->

## Task checklist

<!-- Copy the relevant section's checklist from README.md and check off each item. -->

- [ ]

## Commit summary

<!-- One line per group of commits, summarizing what changed and why. -->

## Test plan

- [ ] Tests exist at every layer the branch's checklist calls for (DAO, RowMapper, EntityMapper, service, controller)
- [ ] Each DAO test class loads its own dedicated SQL fixture file
- [ ] `./mvnw clean verify` is green on the whole reactor

## Code review checklist

- [ ] No business logic in controllers; controllers only see services and `payload/` types
- [ ] DAOs only use `entity/` types, services only `dto/` types at their boundary
- [ ] Every response, success or error, is an `ApiResponse<T>` (errors through `GlobalExceptionHandler`, 401/403 through the security handlers)
- [ ] Permission checks are declarative (`@PreAuthorize("hasPermission(...)")`), no manual permission `if`
- [ ] No `@Repository`, `@Service` or `@Component` on a DAO or service; every bean is an explicit `@Bean` in `DaoConfig`/`ServiceConfig`
- [ ] No Lombok, no `record`, no ORM (`JdbcTemplate` only)
- [ ] Every RBAC mutation calls `AuditLogger`
- [ ] The database schema only changes through a Flyway migration
- [ ] Test methods are named `_NN_Should<Outcome>_When<Condition>`
- [ ] Commits are atomic, follow Conventional Commits with a scope, no em dash, no tool attribution
