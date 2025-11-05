package io.byteforge.backend.repository;

import io.byteforge.backend.model.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByToken(String token);

    Optional<Token> findByUser_Id(Long userId);
}
