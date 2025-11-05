package com.example.reminder.model;

import java.time.Instant;
import java.util.Objects;

import org.hibernate.Hibernate;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "reminders")
public class Reminder {

    @Builder
    public Reminder(String title, String description, Instant remind, User user) {
        this.title = title;
        this.description = description;
        this.remind = remind;
        this.user = user;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reminder_seq")
    @SequenceGenerator(
            name = "reminder_seq",
            sequenceName = "public.reminder_seq",
            allocationSize = 1
    )
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 4096)
    private String description;

    @Column(name = "remind", nullable = false)
    private Instant remind;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_reminder_user"))
    private User user;

    // equals/hashCode — только по id, корректно с Hibernate-прокси
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        Reminder other = (Reminder) o;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Reminder{"
                + "id=" + id
                + ", title='" + title + '\''
                + ", remind=" + remind
                + '}';
    }
}
