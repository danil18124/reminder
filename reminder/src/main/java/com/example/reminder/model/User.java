package com.example.reminder.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import lombok.*;
import jakarta.validation.constraints.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "users")
public class User {

    @Builder
    public User(String email, OAuthProvider provider, String providerId, Role role) {
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
        this.role = role == null ? Role.USER : role;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(name = "user_seq", sequenceName = "user_seq", allocationSize = 1)
    @Setter(AccessLevel.NONE)
    private Long id;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String email; // лучше не использовать как единственный идентификатор — у Google email может меняться

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32, updatable = false)
    @Setter(AccessLevel.NONE)
    private OAuthProvider provider;

    @NotBlank
    @Size(max = 255)
    @Column(name = "provider_id", nullable = false, length = 255, updatable = false)
    @Setter(AccessLevel.NONE)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role = Role.USER;

    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    private Set<Reminder> reminders = new HashSet<>();

    @CreatedDate
    @Setter(AccessLevel.NONE)
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Setter(AccessLevel.NONE)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    private void normalize() {
        if (email != null) {
            email = email.trim().toLowerCase(java.util.Locale.ROOT);
        }
        if (providerId != null) {
            providerId = providerId.trim();
        }
    }

    /* Утилиты для двунаправленной связи */
    public void addReminder(Reminder r) {
        reminders.add(r);
        r.setUser(this);
    }

    public void removeReminder(Reminder r) {
        reminders.remove(r);
        r.setUser(null);
    }

    /* equals/hashCode — только по id, устойчиво к Hibernate-прокси */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        User other = (User) o;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "User{"
                + "id=" + id
                + ", email='" + email + '\''
                + ", provider=" + provider
                + ", role=" + role
                + '}';
    }

}
