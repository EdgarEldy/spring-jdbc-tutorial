package com.edgareldy.springjdbctutorial.core.customer.config;

import com.edgareldy.springjdbctutorial.core.customer.dao.CustomerDao;
import com.edgareldy.springjdbctutorial.core.customer.service.CustomerService;
import com.edgareldy.springjdbctutorial.core.customer.service.impl.CustomerServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Declares the service beans of the customer module. Imports its DaoConfig so importing this class alone is enough.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
@Configuration
@Import(DaoConfig.class)
public class ServiceConfig {

    @Bean
    public CustomerService customerService(CustomerDao customerDao) {
        return new CustomerServiceImpl(customerDao);
    }
}
