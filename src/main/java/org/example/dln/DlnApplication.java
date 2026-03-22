package org.example.dln;

import org.example.dln.entity.User;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.example.dln.mapper")
public class DlnApplication {

    public static void main(String[] args) {
        SpringApplication.run(DlnApplication.class, args);
    }

}
