package com.joycai.orderservice.config

import com.joycai.orderservice.model.Category
import com.joycai.orderservice.repository.CategoryRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DataInitializer {
    @Bean
    fun seedCategories(repo: CategoryRepository) = CommandLineRunner {
        if (repo.count() == 0L) {
            val electronics = repo.save(Category(name = "electronics"))
            repo.save(Category(name = "phone", parentId = electronics.id))
            repo.save(Category(name = "computer", parentId = electronics.id))

            val clothing = repo.save(Category(name = "clothing"))
            repo.save(Category(name = "menswear", parentId = clothing.id))
            repo.save(Category(name = "womenswear", parentId = clothing.id))

            val home = repo.save(Category(name = "home"))
            repo.save(Category(name = "furniture", parentId = home.id))
            repo.save(Category(name = "kitchen", parentId = home.id))
        }
    }
}
