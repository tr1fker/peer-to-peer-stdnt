package com.peerlending.persistence.repository;

import com.peerlending.persistence.entity.OAuthAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccountEntity, Long> {

    Optional<OAuthAccountEntity> findByProviderAndProviderUserId(String provider, String providerUserId);

    boolean existsByUser_IdAndProvider(Long userId, String provider);
}
