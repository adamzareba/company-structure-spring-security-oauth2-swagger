package com.adamzareba.spring.security.oauth2.swagger.repository;

import com.adamzareba.spring.security.oauth2.swagger.model.Company;

import java.util.List;

public interface CompanyRepository {

    Company find(Long id);

    Company find(String name);

    List<Company> findAll();

    void create(Company company);

    Company update(Company company);

    void delete(Long id);

    void delete(Company company);
}
