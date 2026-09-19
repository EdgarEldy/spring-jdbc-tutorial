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
import java.math.BigDecimal;
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
 * End-to-end catalog flows through the REAL stack (real WebMvcConfig, real services, real PostgreSQL migrated by
 * Flyway, real logins): category and product CRUD, the categoryId filter, referenced deletes, unknown categories,
 * duplicate names, price edge cases, pagination bounds and permission enforcement.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
// Same recipe as RbacFlowIntegrationTest: the whole application context on the shared Testcontainers database.
// Every category and product a test creates is named it-<uuid>, and tearDown() removes them (plus the users and
// roles of RbacTestSupport), so the classes sharing the one database never see each other's rows. The catalog
// domain has no last-admin style global rule, so no isolation of other holders is needed here.
@SpringJUnitWebConfig(WebMvcConfig.class)
class CatalogFlowIntegrationTest {

    private static final String PREFIX = "it-";

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
        // Products first: they reference the categories
        jdbcTemplate.update("DELETE FROM products WHERE product_name LIKE ?", PREFIX + "%");
        jdbcTemplate.update("DELETE FROM categories WHERE category_name LIKE ?", PREFIX + "%");
        support.close();
    }

    private static String unique() {
        return PREFIX + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String categoryBody(String name) {
        return "{\"categoryName\":\"" + name + "\"}";
    }

    private static String productBody(long categoryId, String name, String price) {
        return "{\"categoryId\":" + categoryId + ",\"productName\":\"" + name + "\",\"unitPrice\":" + price + "}";
    }

    private static long idOf(MvcResult result) throws Exception {
        return assertSuccess(result, 201).get("data").get("id").asLong();
    }

    private long createCategory(String jwt, String name) throws Exception {
        return idOf(support.post(jwt, "/api/v1/categories", categoryBody(name)));
    }

    private long createProduct(String jwt, long categoryId, String name, String price) throws Exception {
        return idOf(support.post(jwt, "/api/v1/products", productBody(categoryId, name, price)));
    }

    private long count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }

    private List<String> productNames(JsonNode page) {
        List<String> names = new ArrayList<>();
        for (JsonNode product : page.get("data").get("content")) {
            names.add(product.get("productName").asText());
        }
        return names;
    }

    // ---------------------------------------------------------------- categories

    @Test
    void _01_ShouldCreateReadUpdateListAndDeleteACategory_WhenAdminRunsTheFullCrud() throws Exception {
        String jwt = support.createAdmin().getJwt();
        String name = unique();

        long id = createCategory(jwt, " " + name + " ");

        // The stored name is trimmed
        JsonNode read = assertSuccess(support.get(jwt, "/api/v1/categories/" + id), 200);
        assertThat(read.get("data").get("categoryName").asText()).isEqualTo(name);

        String renamed = unique();
        JsonNode updated = assertSuccess(support.put(jwt, "/api/v1/categories/" + id, categoryBody(renamed)), 200);
        assertThat(updated.get("data").get("categoryName").asText()).isEqualTo(renamed);
        assertThat(count("SELECT COUNT(*) FROM categories WHERE category_name = ?", renamed)).isEqualTo(1L);

        JsonNode list = assertSuccess(support.get(jwt, "/api/v1/categories?size=100"), 200);
        boolean listed = false;
        for (JsonNode category : list.get("data").get("content")) {
            listed |= category.get("id").asLong() == id;
        }
        assertThat(listed).isTrue();

        JsonNode deleted = assertSuccess(support.delete(jwt, "/api/v1/categories/" + id), 200);
        assertThat(deleted.get("data") == null || deleted.get("data").isNull()).isTrue();
        assertError(support.get(jwt, "/api/v1/categories/" + id), 404);
    }

    @Test
    void _02_ShouldReturn422_WhenCategoryNameIsDuplicatedOnCreateOrRename() throws Exception {
        String jwt = support.createAdmin().getJwt();
        String first = unique();
        long firstId = createCategory(jwt, first);
        long secondId = createCategory(jwt, unique());

        JsonNode onCreate = assertError(support.post(jwt, "/api/v1/categories", categoryBody(first)), 422);
        JsonNode onRename = assertError(support.put(jwt, "/api/v1/categories/" + secondId, categoryBody(first)), 422);

        assertThat(onCreate.get("message").asText()).isEqualTo("Category name already exists");
        assertThat(onRename.get("message").asText()).isEqualTo("Category name already exists");
        // Saving a category under its own name is not a duplicate
        assertSuccess(support.put(jwt, "/api/v1/categories/" + firstId, categoryBody(first)), 200);
        assertThat(count("SELECT COUNT(*) FROM categories WHERE category_name = ?", first)).isEqualTo(1L);
    }

    @Test
    void _03_ShouldRefuseTheDeleteWith422ThenSucceed_WhenTheCategoryHasProductsUntilTheyAreDeleted() throws Exception {
        String jwt = support.createAdmin().getJwt();
        long categoryId = createCategory(jwt, unique());
        long productId = createProduct(jwt, categoryId, unique(), "5.00");

        JsonNode refused = assertError(support.delete(jwt, "/api/v1/categories/" + categoryId), 422);

        assertThat(refused.get("message").asText()).isEqualTo("Category still has products");
        assertSuccess(support.get(jwt, "/api/v1/categories/" + categoryId), 200);

        assertSuccess(support.delete(jwt, "/api/v1/products/" + productId), 200);
        assertSuccess(support.delete(jwt, "/api/v1/categories/" + categoryId), 200);
        assertError(support.get(jwt, "/api/v1/categories/" + categoryId), 404);
    }

    @Test
    void _04_ShouldReturn404_WhenCategoryDoesNotExist() throws Exception {
        String jwt = support.createAdmin().getJwt();

        assertError(support.get(jwt, "/api/v1/categories/999999999"), 404);
        assertError(support.put(jwt, "/api/v1/categories/999999999", categoryBody(unique())), 404);
        assertError(support.delete(jwt, "/api/v1/categories/999999999"), 404);
    }

    // ---------------------------------------------------------------- products

    @Test
    void _05_ShouldCreateReadUpdateAndDeleteAProductWithExactPrice_WhenAdminRunsTheFullCrud() throws Exception {
        String jwt = support.createAdmin().getJwt();
        long categoryId = createCategory(jwt, unique());
        long otherCategoryId = createCategory(jwt, unique());
        String name = unique();

        MvcResult created = support.post(jwt, "/api/v1/products", productBody(categoryId, " " + name + " ", "12.5"));
        long id = idOf(created);

        // The price comes back with two decimals, and the database holds exactly 12.50
        assertThat(created.getResponse().getContentAsString()).contains("\"unitPrice\":12.50");
        assertThat(jdbcTemplate.queryForObject("SELECT unit_price FROM products WHERE id = ?", BigDecimal.class, id))
                .isEqualTo(new BigDecimal("12.50"));
        JsonNode read = assertSuccess(support.get(jwt, "/api/v1/products/" + id), 200);
        assertThat(read.get("data").get("productName").asText()).isEqualTo(name);
        assertThat(read.get("data").get("categoryId").asLong()).isEqualTo(categoryId);

        String renamed = unique();
        JsonNode updated = assertSuccess(support.put(jwt, "/api/v1/products/" + id,
                productBody(otherCategoryId, renamed, "1.5")), 200);
        assertThat(updated.get("data").get("categoryId").asLong()).isEqualTo(otherCategoryId);
        assertThat(updated.get("data").get("productName").asText()).isEqualTo(renamed);
        assertThat(updated.get("data").get("unitPrice").decimalValue()).isEqualByComparingTo("1.50");

        assertSuccess(support.delete(jwt, "/api/v1/products/" + id), 200);
        assertError(support.get(jwt, "/api/v1/products/" + id), 404);
    }

    @Test
    void _06_ShouldReturn404AndStoreNothing_WhenProductTargetsAnUnknownCategory() throws Exception {
        String jwt = support.createAdmin().getJwt();
        long categoryId = createCategory(jwt, unique());
        String name = unique();
        long id = createProduct(jwt, categoryId, unique(), "3.00");

        JsonNode onCreate = assertError(support.post(jwt, "/api/v1/products", productBody(999999999L, name, "3.00")), 404);
        assertError(support.put(jwt, "/api/v1/products/" + id, productBody(999999999L, name, "3.00")), 404);

        assertThat(onCreate.get("message").asText()).contains("Category not found");
        assertThat(count("SELECT COUNT(*) FROM products WHERE product_name = ?", name)).isZero();
        // The failed update left the product on its category
        assertThat(count("SELECT category_id FROM products WHERE id = ?", id)).isEqualTo(categoryId);
    }

    @Test
    void _07_ShouldReturn404_WhenProductDoesNotExist() throws Exception {
        String jwt = support.createAdmin().getJwt();
        long categoryId = createCategory(jwt, unique());

        assertError(support.get(jwt, "/api/v1/products/999999999"), 404);
        assertError(support.put(jwt, "/api/v1/products/999999999", productBody(categoryId, unique(), "1.00")), 404);
        assertError(support.delete(jwt, "/api/v1/products/999999999"), 404);
    }

    @Test
    void _08_ShouldKeepOnlyTheProductsOfTheCategory_WhenFilteringByCategoryId() throws Exception {
        String jwt = support.createAdmin().getJwt();
        long categoryA = createCategory(jwt, unique());
        long categoryB = createCategory(jwt, unique());
        long emptyCategory = createCategory(jwt, unique());
        String a1 = unique();
        String a2 = unique();
        String b1 = unique();
        createProduct(jwt, categoryA, a1, "1.00");
        createProduct(jwt, categoryA, a2, "2.00");
        createProduct(jwt, categoryB, b1, "3.00");

        JsonNode filteredA = assertSuccess(support.get(jwt, "/api/v1/products?size=100&categoryId=" + categoryA), 200);
        JsonNode pagedA = assertSuccess(support.get(jwt, "/api/v1/products?size=1&page=1&categoryId=" + categoryA), 200);
        JsonNode filteredB = assertSuccess(support.get(jwt, "/api/v1/products?categoryId=" + categoryB), 200);
        JsonNode filteredEmpty = assertSuccess(support.get(jwt, "/api/v1/products?categoryId=" + emptyCategory), 200);

        assertThat(productNames(filteredA)).containsExactly(a1, a2);
        assertThat(filteredA.get("data").get("totalElements").asLong()).isEqualTo(2L);
        assertThat(productNames(pagedA)).containsExactly(a2);
        assertThat(pagedA.get("data").get("totalElements").asLong()).isEqualTo(2L);
        assertThat(pagedA.get("data").get("totalPages").asInt()).isEqualTo(2);
        assertThat(productNames(filteredB)).containsExactly(b1);
        assertThat(filteredEmpty.get("data").get("content")).isEmpty();
        assertThat(filteredEmpty.get("data").get("totalElements").asLong()).isZero();
    }

    @Test
    void _09_ShouldRejectBadPricesWith400AndAcceptTheBoundaries_WhenPricesAreSentThroughHttp() throws Exception {
        String jwt = support.createAdmin().getJwt();
        long categoryId = createCategory(jwt, unique());
        String name = unique();

        JsonNode negative = assertError(support.post(jwt, "/api/v1/products", productBody(categoryId, name, "-0.01")), 400);
        JsonNode threeDecimals = assertError(support.post(jwt, "/api/v1/products", productBody(categoryId, name, "1.005")), 400);
        JsonNode huge = assertError(support.post(jwt, "/api/v1/products", productBody(categoryId, name, "10000000000.00")), 400);
        JsonNode missing = assertError(support.post(jwt, "/api/v1/products",
                "{\"categoryId\":" + categoryId + ",\"productName\":\"" + name + "\"}"), 400);

        assertThat(negative.get("message").asText()).contains("unitPrice: ");
        assertThat(threeDecimals.get("message").asText()).contains("unitPrice: ");
        assertThat(huge.get("message").asText()).contains("unitPrice: ");
        assertThat(missing.get("message").asText()).contains("unitPrice: ");
        assertThat(count("SELECT COUNT(*) FROM products WHERE product_name = ?", name)).isZero();

        // The boundaries themselves are valid: zero, and the largest NUMERIC(12, 2) value
        long free = createProduct(jwt, categoryId, unique(), "0");
        long gold = createProduct(jwt, categoryId, unique(), "9999999999.99");
        assertThat(jdbcTemplate.queryForObject("SELECT unit_price FROM products WHERE id = ?", BigDecimal.class, free))
                .isEqualByComparingTo("0.00");
        assertThat(jdbcTemplate.queryForObject("SELECT unit_price FROM products WHERE id = ?", BigDecimal.class, gold))
                .isEqualByComparingTo("9999999999.99");
    }

    // ---------------------------------------------------------------- pagination

    @Test
    void _10_ShouldReturn400NamingTheParameter_WhenPageOrSizeIsOutOfBounds() throws Exception {
        String jwt = support.createAdmin().getJwt();

        for (String base : List.of("/api/v1/categories", "/api/v1/products")) {
            JsonNode page = assertError(support.get(jwt, base + "?page=-1"), 400);
            JsonNode zero = assertError(support.get(jwt, base + "?size=0"), 400);
            JsonNode big = assertError(support.get(jwt, base + "?size=101"), 400);

            assertThat(page.get("message").asText()).contains("page: ");
            assertThat(zero.get("message").asText()).contains("size: ");
            assertThat(big.get("message").asText()).contains("size: ");
            assertSuccess(support.get(jwt, base + "?size=1"), 200);
            assertSuccess(support.get(jwt, base + "?size=100"), 200);
        }
    }

    @Test
    void _11_ShouldKeepEmptyContentInTheJson_WhenPageIsBeyondTheLastOne() throws Exception {
        String jwt = support.createAdmin().getJwt();
        long categoryId = createCategory(jwt, unique());
        createProduct(jwt, categoryId, unique(), "1.00");

        for (String base : List.of("/api/v1/categories", "/api/v1/products")) {
            JsonNode data = assertSuccess(support.get(jwt, base + "?page=100000&size=10"), 200).get("data");

            assertThat(data.has("content")).isTrue();
            assertThat(data.get("content").isArray()).isTrue();
            assertThat(data.get("content")).isEmpty();
            assertThat(data.get("page").asInt()).isEqualTo(100000);
            assertThat(data.get("totalElements").asLong()).isGreaterThanOrEqualTo(1L);
        }
    }

    // ---------------------------------------------------------------- permissions

    @Test
    void _12_ShouldReturn401_WhenNoTokenIsSent() throws Exception {
        for (String url : List.of("/api/v1/categories", "/api/v1/categories/1", "/api/v1/products", "/api/v1/products/1")) {
            assertError(support.get(null, url), 401);
        }
        assertError(support.post(null, "/api/v1/categories", categoryBody(unique())), 401);
        assertError(support.post(null, "/api/v1/products", productBody(1L, unique(), "1.00")), 401);
    }

    @Test
    void _13_ShouldReturn403AndChangeNothing_WhenUserLacksTheWritePermission() throws Exception {
        TestUser simple = support.createSimpleUser();
        TestUser readOnly = support.createUser(true, false, support.createRole("CATEGORY:READ", "PRODUCT:READ"));
        String admin = support.createAdmin().getJwt();
        long categoryId = createCategory(admin, unique());
        long productId = createProduct(admin, categoryId, unique(), "1.00");
        String name = unique();

        for (TestUser user : List.of(simple, readOnly)) {
            assertError(support.post(user.getJwt(), "/api/v1/categories", categoryBody(name)), 403);
            assertError(support.put(user.getJwt(), "/api/v1/categories/" + categoryId, categoryBody(name)), 403);
            assertError(support.delete(user.getJwt(), "/api/v1/categories/" + categoryId), 403);
            assertError(support.post(user.getJwt(), "/api/v1/products", productBody(categoryId, name, "1.00")), 403);
            assertError(support.put(user.getJwt(), "/api/v1/products/" + productId, productBody(categoryId, name, "1.00")), 403);
            assertError(support.delete(user.getJwt(), "/api/v1/products/" + productId), 403);
        }
        assertThat(count("SELECT COUNT(*) FROM categories WHERE category_name = ?", name)).isZero();
        assertThat(count("SELECT COUNT(*) FROM products WHERE id = ?", productId)).isEqualTo(1L);
        // The simple user has no read permission either, the read-only user does
        assertError(support.get(simple.getJwt(), "/api/v1/categories"), 403);
        assertError(support.get(simple.getJwt(), "/api/v1/products"), 403);
        assertSuccess(support.get(readOnly.getJwt(), "/api/v1/categories/" + categoryId), 200);
        assertSuccess(support.get(readOnly.getJwt(), "/api/v1/products/" + productId), 200);
    }

    @Test
    void _14_ShouldAllowOnlyWhatEachPermissionGrants_WhenUsersHoldASingleCatalogPermission() throws Exception {
        String admin = support.createAdmin().getJwt();
        long categoryId = createCategory(admin, unique());
        TestUser categoryWriter = support.createUser(true, false, support.createRole("CATEGORY:WRITE"));
        TestUser productWriter = support.createUser(true, false, support.createRole("PRODUCT:WRITE"));

        // CATEGORY:WRITE alone creates categories but neither reads them nor touches products
        long created = idOf(support.post(categoryWriter.getJwt(), "/api/v1/categories", categoryBody(unique())));
        assertThat(created).isPositive();
        assertError(support.get(categoryWriter.getJwt(), "/api/v1/categories/" + created), 403);
        assertError(support.post(categoryWriter.getJwt(), "/api/v1/products", productBody(categoryId, unique(), "1.00")), 403);
        // PRODUCT:WRITE alone creates products but cannot create a category
        createProduct(productWriter.getJwt(), categoryId, unique(), "1.00");
        assertError(support.post(productWriter.getJwt(), "/api/v1/categories", categoryBody(unique())), 403);
    }
}
