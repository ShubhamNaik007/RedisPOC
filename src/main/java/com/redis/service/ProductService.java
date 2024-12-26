package com.redis.service;

import com.redis.models.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
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

    private static final String HASH_KEY = "productsDetails";
    private static final long CACHE_TTL = 300L;

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final RedisTemplate<String, Object> template;

    @Autowired
    public ProductService(RedisTemplate<String, Object> template) {
        this.template = template;
    }

    @CachePut
    public Product save(Product product) {
        template.opsForHash().put(HASH_KEY, product.getId(), product);
        return product;
    }

    public List<Product> findAll() {
        List<Object> cachedData = template.opsForHash().values(HASH_KEY);

        if (cachedData.isEmpty()) {
            log.info("Fetching from database");

            List<Product> allProducts = fetchFromDatabase();

            template.opsForHash().putAll(HASH_KEY, allProducts.stream()
                    .collect(Collectors.toMap(product -> String.valueOf(product.getId()), product -> product)));

            template.expire(HASH_KEY, CACHE_TTL, TimeUnit.SECONDS);

            return allProducts;
        }

        log.info("Redis cache already present");

        return cachedData.stream()
                .map(product -> (Product) product)
                .collect(Collectors.toList());
    }

    public Product findProductById(int id) {
        String cacheKey = String.valueOf(id);

        Product product = (Product) template.opsForHash().get(HASH_KEY, cacheKey);

        if (product == null) {
            log.info("Fetching from database");

            product = fetchProductFromDatabaseById(id);

            template.opsForHash().put(HASH_KEY, cacheKey, product);
            template.expire(HASH_KEY, CACHE_TTL, TimeUnit.SECONDS);

            log.info("for id - {} fetched Product from DB is {}", id,product);
        } else {
            log.warn("Cache hit for product id {}", id);
        }

        return product;
    }

    @CacheEvict
    public void deleteProductById(int id) {
        template.opsForHash().delete(HASH_KEY, id);
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
        return new Product(id, "Product" + id, 10, 200);
    }
}
