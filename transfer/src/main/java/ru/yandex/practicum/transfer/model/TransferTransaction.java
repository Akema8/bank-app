package ru.yandex.practicum.transfer.model;

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
@Table("transfer_transaction")
public class TransferTransaction {

    @Id
    private Long id;
    private String fromLogin;
    private String toLogin;
    private BigDecimal amount;
    private String status;
    private String error;
    private LocalDateTime createdAt;
}
