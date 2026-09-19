package com.edgareldy.springjdbctutorial.core.order.config;

import com.edgareldy.springjdbctutorial.core.catalog.service.ProductService;
import com.edgareldy.springjdbctutorial.core.customer.service.CustomerService;
import com.edgareldy.springjdbctutorial.core.order.dao.OrderDao;
import com.edgareldy.springjdbctutorial.core.order.service.OrderService;
import com.edgareldy.springjdbctutorial.core.order.service.impl.OrderServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Declares the service beans of the order module. Imports its DaoConfig and the catalog and customer
 * ServiceConfig (fully qualified, every module has a class of that name) so importing this class alone is enough.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
@Configuration
@Import({DaoConfig.class,
        com.edgareldy.springjdbctutorial.core.catalog.config.ServiceConfig.class,
        com.edgareldy.springjdbctutorial.core.customer.config.ServiceConfig.class})
public class ServiceConfig {

    @Bean
    public OrderService orderService(OrderDao orderDao, ProductService productService, CustomerService customerService) {
        return new OrderServiceImpl(orderDao, productService, customerService);
    }
}
