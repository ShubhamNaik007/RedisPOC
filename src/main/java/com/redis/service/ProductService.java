package com.redis.service;

import com.redis.models.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@EnableCaching
@Component
public class ProductService {

    private static final String HASH_KEY = "ProductData";
    private static final long CACHE_TTL = 300L;

    private final RedisTemplate<String, Object> template;

    @Autowired
    public ProductService(RedisTemplate<String, Object> template) {
        this.template = template;
    }

    public Product save(Product product) {
        template.opsForHash().put(HASH_KEY, product.getId(), product);
        return product;
    }

    public List<Product> findAll() {
        List<Object> cachedData = template.opsForHash().values(HASH_KEY);

        if (cachedData.isEmpty()) {
            System.out.println("Cache miss: Fetching from database");

            List<Product> allProducts = fetchFromDatabase();

            System.out.println("allProducts fetch from Db "+allProducts);

            template.opsForHash().putAll(HASH_KEY, allProducts.stream()
                    .collect(Collectors.toMap(product -> String.valueOf(product.getId()), product -> product)));

            template.expire(HASH_KEY, CACHE_TTL, TimeUnit.SECONDS);

            return allProducts;
        }
        System.out.println("Redis cache already present");

        return cachedData.stream()
                .map(product -> (Product) product)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "Product")
    public Product findProductById(int id) {
        System.out.println("findProductById is called");

        Product product = (Product) template.opsForHash().get(HASH_KEY, id);

        if (product == null) {
            System.out.println("No Cache at redis,fetching it from db");
            product = fetchProductFromDatabaseById(id);

            template.opsForHash().put(HASH_KEY, id, product);

            template.expire(HASH_KEY, CACHE_TTL, TimeUnit.SECONDS);
        }

        System.out.println("Data is already present at cache");
        return product;
    }

    @CacheEvict
    public String deleteProductById(int id) {
        System.out.println("delete method called");
        template.opsForHash().delete(HASH_KEY, id);
        return "Product removed!";
    }

    private List<Product> fetchFromDatabase() {
        return List.of(new Product(1, "Product1", 10, 100),
                new Product(2, "Product2", 40, 200),
                new Product(3, "Product2", 50, 200),
                new Product(4, "Product2", 20, 200),
                new Product(5, "Product2", 20, 200),
                new Product(6, "Product2", 10, 200),
                new Product(7, "Product2", 40, 200),
                new Product(8, "Product2", 20, 200),
                new Product(9, "Product2", 20, 200),
                new Product(10, "Product2", 20, 200));
    }

    private Product fetchProductFromDatabaseById(int id) {
        return new Product(id, "Product" + id, 10, 100);
    }
}
