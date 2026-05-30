package ru.yandex.practicum.cash.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("cash_transaction")
public class CashTransaction {

    @Id
    private Long id;
    private String login;
    private String action;
    private BigDecimal amount;
    private LocalDateTime createdAt;
}
