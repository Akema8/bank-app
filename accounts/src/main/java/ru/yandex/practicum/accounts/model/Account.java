package ru.yandex.practicum.accounts.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("account")
public class Account {

    @Id
    private Long id;
    private String login;
    private String name;
    private LocalDate birthdate;
    private BigDecimal balance;
}