package com.kkudaero.server.domain.seller.entity;

import com.kkudaero.server.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sellers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seller extends BaseTimeEntity {

    public static final int EMAIL_MAX_LENGTH = 320;

    private static final int PASSWORD_HASH_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = EMAIL_MAX_LENGTH)
    private String email;

    @Column(nullable = false, length = PASSWORD_HASH_MAX_LENGTH)
    private String passwordHash;

    private Seller(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public static Seller create(String email, String passwordHash) {
        return new Seller(email, passwordHash);
    }
}
