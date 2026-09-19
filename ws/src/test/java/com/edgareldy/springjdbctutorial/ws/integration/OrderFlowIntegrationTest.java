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
import java.util.ArrayList;
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
 * End-to-end order flows through the REAL stack (real WebMvcConfig, real services, real PostgreSQL migrated by
 * Flyway, real logins): the total computed from the product price at creation, the filters and pagination, the
 * 404 and 422 refusals, the deletes of a customer or a product refused while an order references them, and the
 * permission enforcement.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
// Same recipe as CustomerFlowIntegrationTest: the whole application context on the shared Testcontainers database.
// Customers, categories and products are created through their own REAL endpoints with an admin, named with an
// it-<uuid> prefix; tearDown() removes the orders first (they reference the others), then the rest, then the
// users and roles of RbacTestSupport.
@SpringJUnitWebConfig(WebMvcConfig.class)
class OrderFlowIntegrationTest {

    private static final String PREFIX = "it-";
    private static final String URL = "/api/v1/orders";

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
        jdbcTemplate.update("DELETE FROM orders WHERE customer_id IN (SELECT id FROM customers WHERE first_name LIKE ?) "
                + "OR product_id IN (SELECT id FROM products WHERE product_name LIKE ?)", PREFIX + "%", PREFIX + "%");
        jdbcTemplate.update("DELETE FROM customers WHERE first_name LIKE ?", PREFIX + "%");
        jdbcTemplate.update("DELETE FROM products WHERE product_name LIKE ?", PREFIX + "%");
        jdbcTemplate.update("DELETE FROM categories WHERE category_name LIKE ?", PREFIX + "%");
        support.close();
    }

    private static String unique() {
        return PREFIX + UUID.randomUUID().toString().substring(0, 8);
    }

    private static long idOf(MvcResult result) throws Exception {
        return assertSuccess(result, 201).get("data").get("id").asLong();
    }

    private long customer(String jwt) throws Exception {
        return idOf(support.post(jwt, "/api/v1/customers", "{\"firstName\":\"" + unique() + "\",\"lastName\":\"Martin\"}"));
    }

    private long category(String jwt) throws Exception {
        return idOf(support.post(jwt, "/api/v1/categories", "{\"categoryName\":\"" + unique() + "\"}"));
    }

    private long product(String jwt, long categoryId, String unitPrice) throws Exception {
        return idOf(support.post(jwt, "/api/v1/products", "{\"categoryId\":" + categoryId + ",\"productName\":\""
                + unique() + "\",\"unitPrice\":" + unitPrice + "}"));
    }

    private static String orderBody(Object customerId, Object productId, Object quantity) {
        return "{\"customerId\":" + customerId + ",\"productId\":" + productId + ",\"quantity\":" + quantity + "}";
    }

    private long order(String jwt, long customerId, long productId, int quantity) throws Exception {
        return idOf(support.post(jwt, URL, orderBody(customerId, productId, quantity)));
    }

    private long count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }

    private static List<Long> ids(JsonNode page) {
        List<Long> ids = new ArrayList<>();
        for (JsonNode order : page.get("data").get("content")) {
            ids.add(order.get("id").asLong());
        }
        return ids;
    }

    @Test
    void _01_ShouldComputeTheTotalFromTheProductPrice_WhenAdminCreatesAnOrder() throws Exception {
        String jwt = support.createAdmin().getJwt();
        long customerId = customer(jwt);
        long productId = product(jwt, category(jwt), "19.99");

        JsonNode created = assertSuccess(support.post(jwt, URL, orderBody(customerId, productId, 3)), 201);

        assertThat(created.get("message").asText()).isEqualTo("Order created");
        assertThat(created.get("data").get("customerId").asLong()).isEqualTo(customerId);
        assertThat(created.get("data").get("productId").asLong()).isEqualTo(productId);
        assertThat(created.get("data").get("quantity").asInt()).isEqualTo(3);
        assertThat(created.get("data").get("total").decimalValue()).isEqualByComparingTo("59.97");
        long id = created.get("data").get("id").asLong();
        JsonNode read = assertSuccess(support.get(jwt, URL + "/" + id), 200).get("data");
        assertThat(read.get("total").decimalValue()).isEqualByComparingTo("59.97");
        assertThat(read.get("quantity").asInt()).isEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM orders WHERE id = ? AND total = 59.97", id)).isEqualTo(1L);
    }

    @Test
    void _02_ShouldKeepTheTotal_WhenTheProductPriceIsUpdatedLater() throws Exception {
        String jwt = support.createAdmin().getJwt();
        long customerId = customer(jwt);
        long categoryId = category(jwt);
        long productId = product(jwt, categoryId, "10.00");
        long id = order(jwt, customerId, productId, 2);

        assertSuccess(support.put(jwt, "/api/v1/products/" + productId,
                "{\"categoryId\":" + categoryId + ",\"productName\":\"" + unique() + "\",\"unitPrice\":99.00}"), 200);

        JsonNode read = assertSuccess(support.get(jwt, URL + "/" + id), 200).get("data");
        assertThat(read.get("total").decimalValue()).isEqualByComparingTo("20.00");
        long next = order(jwt, customerId, productId, 2);
        assertThat(assertSuccess(support.get(jwt, URL + "/" + next), 200).get("data").get("total").decimalValue())
                .isEqualByComparingTo("198.00");
    }

    @Test
    void _03_ShouldFilterAndPaginate_WhenListingWithCustomerIdAndProductId() throws Exception {
        String jwt = support.createAdmin().getJwt();
        long c1 = customer(jwt);
        long c2 = customer(jwt);
        long categoryId = category(jwt);
        long p1 = product(jwt, categoryId, "1.00");
        long p2 = product(jwt, categoryId, "2.00");
        long o1 = order(jwt, c1, p1, 1);
        long o2 = order(jwt, c1, p2, 1);
        long o3 = order(jwt, c2, p1, 1);
        long o4 = order(jwt, c1, p1, 2);

        assertThat(ids(assertSuccess(support.get(jwt, URL + "?customerId=" + c1), 200))).containsExactly(o1, o2, o4);
        assertThat(ids(assertSuccess(support.get(jwt, URL + "?productId=" + p1), 200))).containsExactly(o1, o3, o4);
        assertThat(ids(assertSuccess(support.get(jwt, URL + "?customerId=" + c1 + "&productId=" + p1), 200)))
                .containsExactly(o1, o4);
        assertThat(ids(assertSuccess(support.get(jwt, URL + "?customerId=" + c2 + "&productId=" + p2), 200))).isEmpty();

        JsonNode second = assertSuccess(support.get(jwt, URL + "?customerId=" + c1 + "&size=2&page=1"), 200);
        assertThat(ids(second)).containsExactly(o4);
        assertThat(second.get("data").get("totalElements").asLong()).isEqualTo(3L);
        assertThat(second.get("data").get("totalPages").asInt()).isEqualTo(2);
    }

    @Test
    void _04_ShouldReturn404AndStoreNothing_WhenCustomerOrProductDoesNotExist() throws Exception {
        String jwt = support.createAdmin().getJwt();
        long customerId = customer(jwt);
        long productId = product(jwt, category(jwt), "5.00");

        JsonNode noCustomer = assertError(support.post(jwt, URL, orderBody(999999999L, productId, 1)), 404);
        JsonNode noProduct = assertError(support.post(jwt, URL, orderBody(customerId, 999999999L, 1)), 404);

        assertThat(noCustomer.get("message").asText()).isEqualTo("Customer not found: 999999999");
        assertThat(noProduct.get("message").asText()).isEqualTo("Product not found: 999999999");
        assertThat(count("SELECT COUNT(*) FROM orders WHERE customer_id = ? OR product_id = ?", customerId, productId))
                .isZero();
    }

    @Test
    void _05_ShouldReturn400AndStoreNothing_WhenBodyIsInvalid() throws Exception {
        String jwt = support.createAdmin().getJwt();
        long customerId = customer(jwt);
        long productId = product(jwt, category(jwt), "5.00");

        for (Object quantity : List.of(0, -1, 1000001)) {
            JsonNode json = assertError(support.post(jwt, URL, orderBody(customerId, productId, quantity)), 400);
            assertThat(json.get("message").asText()).contains("quantity: ");
        }
        JsonNode missing = assertError(support.post(jwt, URL, "{\"customerId\":" + customerId + ",\"productId\":"
                + productId + "}"), 400);
        JsonNode noIds = assertError(support.post(jwt, URL, "{\"quantity\":1}"), 400);

        assertThat(missing.get("message").asText()).contains("quantity: ");
        assertThat(noIds.get("message").asText()).contains("customerId: ").contains("productId: ");
        assertThat(count("SELECT COUNT(*) FROM orders WHERE customer_id = ?", customerId)).isZero();
    }

    @Test
    void _06_ShouldReturn422AndStoreNothing_WhenTheTotalExceedsTheColumnLimit() throws Exception {
        String jwt = support.createAdmin().getJwt();
        long customerId = customer(jwt);
        long productId = product(jwt, category(jwt), "9999999999.99");

        JsonNode json = assertError(support.post(jwt, URL, orderBody(customerId, productId, 1000000)), 422);

        assertThat(json.get("message").asText()).isEqualTo("Order total must not exceed 999999999999.99");
        assertThat(count("SELECT COUNT(*) FROM orders WHERE customer_id = ?", customerId)).isZero();
        // 99 x 9999999999.99 is still below the limit and goes through
        assertSuccess(support.post(jwt, URL, orderBody(customerId, productId, 99)), 201);
    }

    @Test
    void _07_ShouldRefuseTheCustomerDeleteWith422UntilTheOrderIsRemoved_WhenAnOrderReferencesTheCustomer()
            throws Exception {
        String jwt = support.createAdmin().getJwt();
        long customerId = customer(jwt);
        long productId = product(jwt, category(jwt), "5.00");
        long orderId = order(jwt, customerId, productId, 1);

        JsonNode refused = assertError(support.delete(jwt, "/api/v1/customers/" + customerId), 422);

        assertThat(refused.get("message").asText()).isEqualTo("Customer is referenced by orders");
        assertSuccess(support.get(jwt, "/api/v1/customers/" + customerId), 200);
        jdbcTemplate.update("DELETE FROM orders WHERE id = ?", orderId);
        assertSuccess(support.delete(jwt, "/api/v1/customers/" + customerId), 200);
        assertError(support.get(jwt, "/api/v1/customers/" + customerId), 404);
    }

    @Test
    void _08_ShouldRefuseTheProductDeleteWith422UntilTheOrderIsRemoved_WhenAnOrderReferencesTheProduct()
            throws Exception {
        String jwt = support.createAdmin().getJwt();
        long customerId = customer(jwt);
        long productId = product(jwt, category(jwt), "5.00");
        long orderId = order(jwt, customerId, productId, 1);

        JsonNode refused = assertError(support.delete(jwt, "/api/v1/products/" + productId), 422);

        assertThat(refused.get("message").asText()).isEqualTo("Product is referenced by orders");
        assertSuccess(support.get(jwt, "/api/v1/products/" + productId), 200);
        jdbcTemplate.update("DELETE FROM orders WHERE id = ?", orderId);
        assertSuccess(support.delete(jwt, "/api/v1/products/" + productId), 200);
        assertError(support.get(jwt, "/api/v1/products/" + productId), 404);
    }

    @Test
    void _09_ShouldReturn404_WhenOrderDoesNotExist() throws Exception {
        String jwt = support.createAdmin().getJwt();

        JsonNode json = assertError(support.get(jwt, URL + "/999999999"), 404);

        assertThat(json.get("message").asText()).isEqualTo("Order not found: 999999999");
    }

    @Test
    void _10_ShouldReturn400NamingTheParameter_WhenPageOrSizeIsOutOfBounds() throws Exception {
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
    void _11_ShouldKeepEmptyContentInTheJson_WhenPageIsBeyondTheLastOne() throws Exception {
        String jwt = support.createAdmin().getJwt();
        long customerId = customer(jwt);
        order(jwt, customerId, product(jwt, category(jwt), "1.00"), 1);

        JsonNode data = assertSuccess(support.get(jwt, URL + "?customerId=" + customerId + "&page=100000&size=10"), 200)
                .get("data");

        assertThat(data.has("content")).isTrue();
        assertThat(data.get("content").isArray()).isTrue();
        assertThat(data.get("content")).isEmpty();
        assertThat(data.get("page").asInt()).isEqualTo(100000);
        assertThat(data.get("totalElements").asLong()).isEqualTo(1L);
    }

    @Test
    void _12_ShouldReturn401_WhenNoTokenIsSent() throws Exception {
        assertError(support.get(null, URL), 401);
        assertError(support.get(null, URL + "/1"), 401);
        assertError(support.post(null, URL, orderBody(1, 1, 1)), 401);
    }

    @Test
    void _13_ShouldReturn403AndStoreNothing_WhenUserLacksTheWritePermission() throws Exception {
        TestUser simple = support.createSimpleUser();
        TestUser readOnly = support.createUser(true, false, support.createRole("ORDER:READ"));
        String admin = support.createAdmin().getJwt();
        long customerId = customer(admin);
        long productId = product(admin, category(admin), "5.00");
        long id = order(admin, customerId, productId, 1);

        for (TestUser user : List.of(simple, readOnly)) {
            assertError(support.post(user.getJwt(), URL, orderBody(customerId, productId, 1)), 403);
        }

        assertThat(count("SELECT COUNT(*) FROM orders WHERE customer_id = ?", customerId)).isEqualTo(1L);
        // The simple user has no read permission either, the read-only user does
        assertError(support.get(simple.getJwt(), URL), 403);
        assertError(support.get(simple.getJwt(), URL + "/" + id), 403);
        assertSuccess(support.get(readOnly.getJwt(), URL + "/" + id), 200);
        assertSuccess(support.get(readOnly.getJwt(), URL + "?customerId=" + customerId), 200);
    }

    @Test
    void _14_ShouldCreateButNotRead_WhenUserHoldsOnlyOrderWrite() throws Exception {
        String admin = support.createAdmin().getJwt();
        long customerId = customer(admin);
        long productId = product(admin, category(admin), "5.00");
        TestUser writer = support.createUser(true, false, support.createRole("ORDER:WRITE"));

        long id = order(writer.getJwt(), customerId, productId, 2);

        assertError(support.get(writer.getJwt(), URL + "/" + id), 403);
        assertError(support.get(writer.getJwt(), URL), 403);
    }

    @Test
    void _15_ShouldReturn405_WhenAnOrderIsUpdatedOrDeleted() throws Exception {
        String jwt = support.createAdmin().getJwt();
        long customerId = customer(jwt);
        long id = order(jwt, customerId, product(jwt, category(jwt), "5.00"), 1);

        assertError(support.put(jwt, URL + "/" + id, orderBody(customerId, 1, 1)), 405);
        assertError(support.delete(jwt, URL + "/" + id), 405);
        assertSuccess(support.get(jwt, URL + "/" + id), 200);
    }

    // Non-regression: Jackson used to truncate a decimal quantity such as 1.5 to 1 and create the order
    @Test
    void _16_ShouldReturn400AndStoreNothing_WhenQuantityIsADecimal() throws Exception {
        String jwt = support.createAdmin().getJwt();
        long customerId = customer(jwt);
        long productId = product(jwt, category(jwt), "5.00");

        assertError(support.post(jwt, URL, orderBody(customerId, productId, "1.5")), 400);

        assertThat(count("SELECT COUNT(*) FROM orders WHERE customer_id = ?", customerId)).isZero();
    }
}
