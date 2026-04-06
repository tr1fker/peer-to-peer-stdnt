package com.peerlending.persistence.repository;

import com.peerlending.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByEmailVerified(boolean emailVerified);

    long countByBlocked(boolean blocked);

    @Query(
            value = """
                    select r.name, count(distinct ur.user_id)
                    from user_roles ur
                    join roles r on r.id = ur.role_id
                    group by r.name
                    order by r.name
                    """,
            nativeQuery = true
    )
    List<Object[]> countDistinctUsersPerRoleName();

    @Query(
            value = """
                    select cast(u.created_at at time zone 'UTC' as date), count(*)
                    from users u
                    where u.created_at >= cast(:from as timestamptz)
                    group by 1
                    order by 1
                    """,
            nativeQuery = true
    )
    List<Object[]> countNewUsersByDayUtc(@Param("from") Instant from);
}
