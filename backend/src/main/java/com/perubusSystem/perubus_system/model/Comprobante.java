package com.perubusSystem.perubus_system.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "comprobante")
@Getter
@Setter
public class Comprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comprobante")
    private Long idComprobante;

    @Column(name = "tipo_comprobante", length = 50)
    private String tipoComprobante;

    @Column(name = "monto_total", precision = 10, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "monto_subtotal", precision = 10, scale = 2)
    private BigDecimal montoSubtotal;

    @Column(name = "monto_igv", precision = 10, scale = 2)
    private BigDecimal montoIgv;

    @Column(name = "fecha_emision")
    private LocalDateTime fechaEmision;

    @Column(name = "estado_pago", length = 30)
    private String estadoPago;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @OneToOne
    @JoinColumn(name = "id_encomienda")
    private Encomienda encomienda;

    public Comprobante() {
    }

}