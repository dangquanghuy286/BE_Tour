package com.project.booktour.repositories;

import com.project.booktour.models.Token;
import com.project.booktour.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {
    List<Token> findByUser(User user);

//    Token findByToken(String token);
//
//    Token findByRefreshToken(String token);
    Optional<Token> findByToken(String token);
    void deleteAllByUser(User user);

    boolean existsByToken(String resetToken);
}