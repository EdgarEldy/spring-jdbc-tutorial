package com.edgareldy.springjdbctutorial.ws.integration;

import static com.edgareldy.springjdbctutorial.ws.support.ApiAssertions.assertError;
import static com.edgareldy.springjdbctutorial.ws.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.edgareldy.springjdbctutorial.core.common.support.PostgresTestContainer;
import com.edgareldy.springjdbctutorial.ws.config.WebMvcConfig;
import com.edgareldy.springjdbctutorial.ws.support.RbacTestSupport;
import com.edgareldy.springjdbctutorial.ws.support.RbacTestSupport.TestUser;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * End-to-end customer flows through the REAL stack (real WebMvcConfig, real service, real PostgreSQL migrated by
 * Flyway, real logins): CRUD, optional fields, the email format rules at both levels, the delete refused while
 * orders reference the customer, pagination bounds and permission enforcement.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
// Same recipe as CatalogFlowIntegrationTest: the whole application context on the shared Testcontainers database.
// Every customer a test creates has a it-<uuid> first name, and tearDown() removes them (with the orders, product
// and category some tests insert by SQL, and the users and roles of RbacTestSupport).
@SpringJUnitWebConfig(WebMvcConfig.class)
class CustomerFlowIntegrationTest {

    private static final String PREFIX = "it-";
    private static final String URL = "/api/v1/customers";

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestContainer.register(registry);
    }

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;
    private RbacTestSupport support;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        support = new RbacTestSupport(mockMvc, jdbcTemplate);
    }

    @AfterEach
    void tearDown() {
        // Orders first: they reference the customers and the products
        jdbcTemplate.update("DELETE FROM orders WHERE customer_id IN (SELECT id FROM customers WHERE first_name LIKE ?)",
                PREFIX + "%");
        jdbcTemplate.update("DELETE FROM customers WHERE first_name LIKE ?", PREFIX + "%");
        jdbcTemplate.update("DELETE FROM products WHERE product_name LIKE ?", PREFIX + "%");
        jdbcTemplate.update("DELETE FROM categories WHERE category_name LIKE ?", PREFIX + "%");
        support.close();
    }

    private static String unique() {
        return PREFIX + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String body(String firstName, String lastName, String telephone, String email, String address) {
        return "{\"firstName\":\"" + firstName + "\",\"lastName\":\"" + lastName + "\",\"telephone\":" + quote(telephone)
                + ",\"email\":" + quote(email) + ",\"address\":" + quote(address) + "}";
    }

    private static String quote(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private static String minimalBody(String firstName) {
        return "{\"firstName\":\"" + firstName + "\",\"lastName\":\"Martin\"}";
    }

    private static long idOf(MvcResult result) throws Exception {
        return assertSuccess(result, 201).get("data").get("id").asLong();
    }

    private long count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }

    @Test
    void _01_ShouldCreateReadUpdateListAndDeleteACustomer_WhenAdminRunsTheFullCrud() throws Exception {
        String jwt = support.createAdmin().getJwt();
        String name = unique();

        MvcResult created = support.post(jwt, URL, body(" " + name + " ", " Martin ", " 555 ", "a@example.com", " 1 Main "));
        long id = idOf(created);

        // Every text is stored trimmed (the email is sent without spaces: @Email rejects surrounding spaces with a 400)
        JsonNode read = assertSuccess(support.get(jwt, URL + "/" + id), 200).get("data");
        assertThat(read.get("firstName").asText()).isEqualTo(name);
        assertThat(read.get("lastName").asText()).isEqualTo("Martin");
        assertThat(read.get("telephone").asText()).isEqualTo("555");
        assertThat(read.get("email").asText()).isEqualTo("a@example.com");
        assertThat(read.get("address").asText()).isEqualTo("1 Main");

        String renamed = unique();
        JsonNode updated = assertSuccess(support.put(jwt, URL + "/" + id,
                body(renamed, "Stone", "777", "b@example.com", "2 Road")), 200);
        assertThat(updated.get("data").get("firstName").asText()).isEqualTo(renamed);
        assertThat(count("SELECT COUNT(*) FROM customers WHERE first_name = ? AND last_name = 'Stone' AND telephone = '777'",
                renamed)).isEqualTo(1L);

        JsonNode list = assertSuccess(support.get(jwt, URL + "?size=100"), 200);
        boolean listed = false;
        for (JsonNode customer : list.get("data").get("content")) {
            listed |= customer.get("id").asLong() == id;
        }
        assertThat(listed).isTrue();

        JsonNode deleted = assertSuccess(support.delete(jwt, URL + "/" + id), 200);
        assertThat(deleted.get("data") == null || deleted.get("data").isNull()).isTrue();
        assertError(support.get(jwt, URL + "/" + id), 404);
    }

    @Test
    void _02_ShouldStoreNullOptionalFields_WhenTheyAreAbsentEmptyOrBlank() throws Exception {
        String jwt = support.createAdmin().getJwt();

        long absent = idOf(support.post(jwt, URL, minimalBody(unique())));
        long blank = idOf(support.post(jwt, URL, body(unique(), "Martin", "   ", "", " ")));

        for (long id : List.of(absent, blank)) {
            JsonNode data = assertSuccess(support.get(jwt, URL + "/" + id), 200).get("data");
            assertThat(data.get("telephone").isNull()).isTrue();
            assertThat(data.get("email").isNull()).isTrue();
            assertThat(data.get("address").isNull()).isTrue();
            assertThat(count("SELECT COUNT(*) FROM customers WHERE id = ? AND telephone IS NULL AND email IS NULL "
                    + "AND address IS NULL", id)).isEqualTo(1L);
        }
    }

    @Test
    void _03_ShouldClearOptionalFields_WhenUpdatedWithoutThem() throws Exception {
        String jwt = support.createAdmin().getJwt();
        String name = unique();
        long id = idOf(support.post(jwt, URL, body(name, "Martin", "555", "a@example.com", "1 Main")));

        assertSuccess(support.put(jwt, URL + "/" + id, minimalBody(name)), 200);

        assertThat(count("SELECT COUNT(*) FROM customers WHERE id = ? AND telephone IS NULL AND email IS NULL "
                + "AND address IS NULL", id)).isEqualTo(1L);
    }

    @Test
    void _04_ShouldReturn400OrHandOverTo422_WhenEmailIsMalformed() throws Exception {
        String jwt = support.createAdmin().getJwt();
        String name = unique();

        // Refused by Bean Validation (@Email) before the service runs: 400 naming the field
        for (String bad : List.of("no-at-sign", "a@@b.com", "a b@c.com", "@c.com")) {
            JsonNode json = assertError(support.post(jwt, URL, body(name, "Martin", null, bad, null)), 400);
            assertThat(json.get("message").asText()).contains("email: ");
        }
        // The Bean Validation rule accepts a domain without a dot, the service rule then refuses it: 422
        JsonNode noDot = assertError(support.post(jwt, URL, body(name, "Martin", null, "alice@example", null)), 422);
        assertThat(noDot.get("message").asText()).isEqualTo("Email is not valid");
        JsonNode onUpdate = assertError(support.put(jwt, URL + "/" + idOf(support.post(jwt, URL, minimalBody(name))),
                body(name, "Martin", null, "alice@example", null)), 422);
        assertThat(onUpdate.get("message").asText()).isEqualTo("Email is not valid");
        assertThat(count("SELECT COUNT(*) FROM customers WHERE first_name = ? AND email IS NOT NULL", name)).isZero();
    }

    @Test
    void _05_ShouldReturn400NamingTheFieldAndStoreNothing_WhenBodyIsInvalid() throws Exception {
        String jwt = support.createAdmin().getJwt();
        String name = unique();

        JsonNode blank = assertError(support.post(jwt, URL, body(" ", "Martin", null, null, null)), 400);
        JsonNode oversize = assertError(support.post(jwt, URL, body(name, "y".repeat(101), null, null, null)), 400);
        JsonNode phone = assertError(support.post(jwt, URL, body(name, "Martin", "1".repeat(31), null, null)), 400);

        assertThat(blank.get("message").asText()).contains("firstName: ");
        assertThat(oversize.get("message").asText()).contains("lastName: ");
        assertThat(phone.get("message").asText()).contains("telephone: ");
        assertThat(count("SELECT COUNT(*) FROM customers WHERE first_name = ?", name)).isZero();
    }

    @Test
    void _06_ShouldReturn404_WhenCustomerDoesNotExist() throws Exception {
        String jwt = support.createAdmin().getJwt();

        assertError(support.get(jwt, URL + "/999999999"), 404);
        assertError(support.put(jwt, URL + "/999999999", minimalBody(unique())), 404);
        assertError(support.delete(jwt, URL + "/999999999"), 404);
    }

    @Test
    void _07_ShouldRefuseTheDeleteWith422AndKeepTheCustomer_WhenAnOrderReferencesIt() throws Exception {
        String jwt = support.createAdmin().getJwt();
        long id = idOf(support.post(jwt, URL, minimalBody(unique())));
        // The order endpoints do not exist yet: a category, a product and the order are inserted through SQL
        long categoryId = jdbcTemplate.queryForObject(
                "INSERT INTO categories (category_name) VALUES (?) RETURNING id", Long.class, unique());
        long productId = jdbcTemplate.queryForObject(
                "INSERT INTO products (category_id, product_name, unit_price) VALUES (?, ?, 5.00) RETURNING id",
                Long.class, categoryId, unique());
        jdbcTemplate.update("INSERT INTO orders (customer_id, product_id, quantity, total) VALUES (?, ?, 2, 10.00)",
                id, productId);

        JsonNode refused = assertError(support.delete(jwt, URL + "/" + id), 422);

        assertThat(refused.get("message").asText()).isEqualTo("Customer is referenced by orders");
        assertSuccess(support.get(jwt, URL + "/" + id), 200);
        assertThat(count("SELECT COUNT(*) FROM customers WHERE id = ?", id)).isEqualTo(1L);

        // Once the order is gone the same delete goes through
        jdbcTemplate.update("DELETE FROM orders WHERE customer_id = ?", id);
        assertSuccess(support.delete(jwt, URL + "/" + id), 200);
        assertError(support.get(jwt, URL + "/" + id), 404);
    }

    @Test
    void _08_ShouldReturn400NamingTheParameter_WhenPageOrSizeIsOutOfBounds() throws Exception {
        String jwt = support.createAdmin().getJwt();

        JsonNode page = assertError(support.get(jwt, URL + "?page=-1"), 400);
        JsonNode zero = assertError(support.get(jwt, URL + "?size=0"), 400);
        JsonNode big = assertError(support.get(jwt, URL + "?size=101"), 400);

        assertThat(page.get("message").asText()).contains("page: ");
        assertThat(zero.get("message").asText()).contains("size: ");
        assertThat(big.get("message").asText()).contains("size: ");
        assertSuccess(support.get(jwt, URL + "?size=1"), 200);
        assertSuccess(support.get(jwt, URL + "?size=100"), 200);
    }

    @Test
    void _09_ShouldKeepEmptyContentInTheJson_WhenPageIsBeyondTheLastOne() throws Exception {
        String jwt = support.createAdmin().getJwt();
        idOf(support.post(jwt, URL, minimalBody(unique())));

        JsonNode data = assertSuccess(support.get(jwt, URL + "?page=100000&size=10"), 200).get("data");

        assertThat(data.has("content")).isTrue();
        assertThat(data.get("content").isArray()).isTrue();
        assertThat(data.get("content")).isEmpty();
        assertThat(data.get("page").asInt()).isEqualTo(100000);
        assertThat(data.get("totalElements").asLong()).isGreaterThanOrEqualTo(1L);
    }

    @Test
    void _10_ShouldReturn401_WhenNoTokenIsSent() throws Exception {
        for (String url : List.of(URL, URL + "/1")) {
            assertError(support.get(null, url), 401);
        }
        assertError(support.post(null, URL, minimalBody(unique())), 401);
        assertError(support.put(null, URL + "/1", minimalBody(unique())), 401);
        assertError(support.delete(null, URL + "/1"), 401);
    }

    @Test
    void _11_ShouldReturn403AndChangeNothing_WhenUserLacksTheWritePermission() throws Exception {
        TestUser simple = support.createSimpleUser();
        TestUser readOnly = support.createUser(true, false, support.createRole("CUSTOMER:READ"));
        String admin = support.createAdmin().getJwt();
        String existing = unique();
        long id = idOf(support.post(admin, URL, minimalBody(existing)));
        String name = unique();

        for (TestUser user : List.of(simple, readOnly)) {
            assertError(support.post(user.getJwt(), URL, minimalBody(name)), 403);
            assertError(support.put(user.getJwt(), URL + "/" + id, minimalBody(name)), 403);
            assertError(support.delete(user.getJwt(), URL + "/" + id), 403);
        }
        assertThat(count("SELECT COUNT(*) FROM customers WHERE first_name = ?", name)).isZero();
        assertThat(count("SELECT COUNT(*) FROM customers WHERE id = ? AND first_name = ?", id, existing)).isEqualTo(1L);
        // The simple user has no read permission either, the read-only user does
        assertError(support.get(simple.getJwt(), URL), 403);
        assertSuccess(support.get(readOnly.getJwt(), URL + "/" + id), 200);
    }

    @Test
    void _12_ShouldAllowOnlyWhatEachPermissionGrants_WhenUsersHoldASingleCustomerPermission() throws Exception {
        TestUser writer = support.createUser(true, false, support.createRole("CUSTOMER:WRITE"));

        // CUSTOMER:WRITE alone creates customers but neither lists nor reads them
        long id = idOf(support.post(writer.getJwt(), URL, minimalBody(unique())));
        assertError(support.get(writer.getJwt(), URL + "/" + id), 403);
        assertError(support.get(writer.getJwt(), URL), 403);
    }
}
