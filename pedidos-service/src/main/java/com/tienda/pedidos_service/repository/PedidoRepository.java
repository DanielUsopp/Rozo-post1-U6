package com.tienda.pedidos_service.repository;

import com.tienda.pedidos_service.validacion.ContextoPedido;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Repository
public class PedidoRepository {

    private final JdbcTemplate jdbcTemplate;

    public PedidoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long guardar(ContextoPedido contexto, double descuento, double impuesto, double total) {
        jdbcTemplate.update(
                "INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                contexto.getRequest().getClienteId(), contexto.getSubtotal(), descuento, impuesto, total,
                Timestamp.valueOf(LocalDateTime.now()), "CONFIRMADO");

        Long pedidoId = jdbcTemplate.queryForObject("CALL IDENTITY()", Long.class);

        for (var item : contexto.getRequest().getItems()) {
            jdbcTemplate.update(
                    "INSERT INTO detalle_pedido (pedido_id, producto_id, cantidad) VALUES (?, ?, ?)",
                    pedidoId, item.getProductoId(), item.getCantidad());

            jdbcTemplate.update(
                    "UPDATE inventario SET stock = stock - ? WHERE producto_id = ?",
                    item.getCantidad(), item.getProductoId());
        }

        return pedidoId;
    }
}