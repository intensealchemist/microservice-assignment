package com.assignment.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "persona_description")
    private String personaDescription;
}
