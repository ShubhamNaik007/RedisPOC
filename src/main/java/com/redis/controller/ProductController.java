package com.redis.controller;

import com.redis.models.Product;
import com.redis.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/product")
@RestController
public class ProductController {

    @Autowired
    private ProductService dao;

    @PostMapping
    public Product save(@RequestBody Product product){
        return dao.save(product);
    }

    @GetMapping
    public List<Product> getAllProducts(){
        return dao.findAll();
    }

    @GetMapping("/{id}")
    @Cacheable(value = "Product",key = "#id",unless = "#result.price > 10000")
    public Product findProduct(@PathVariable int id){
        return dao.findProductById(id);
    }

    @DeleteMapping("/{id}")
    @CacheEvict(value = "Product",key = "#id")
    public String deleteProduct(@PathVariable int id){
        return dao.deleteProductById(id);
    }
    
}
